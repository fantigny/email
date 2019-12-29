package net.anfoya.mail.yahoo.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.mail.Session;
import javax.mail.Store;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import javafx.concurrent.Task;
import net.anfoya.java.io.SerializedFile;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.model.SimpleContact;
import net.anfoya.mail.yahoo.YahooException;
import net.anfoya.mail.yahoo.YahooMailService;
import net.anfoya.mail.yahoo.javafx.SigninDialog;

public class AuthenticationService {
	private static final String CONTACT_FILE_PREFIX = System.getProperty("java.io.tmpdir") + File.separatorChar + "fsm-cache-contact-";

	private static final String CLIENT_ID_PATH = "client_id.txt";
	private static final String CLIENT_SECRET_PATH = "client_secret.txt";

	private static final String REQUEST_AUTH_URL = "https://api.login.yahoo.com/oauth2/request_auth";
	private static final String REQUEST_AUTH_PARAM = "?client_id=%s&redirect_uri=oob&response_type=code";

	private static final String GET_TOKEN_URL = "https://api.login.yahoo.com/oauth2/get_token";

	private static final String USER_INFO_URL = "https://api.login.yahoo.com/openid/v1/userinfo";

	private static final String REFRESH_TOKEN_SUFFIX = "%s-refresh-token";

	private final String user;
	private final String prefsTokenName;

	private Runnable authCallback;
	private Runnable authFailedCallback;
	private YahooException exception;

	private String accessToken;
	private String refreshToken;

	private Store yahooMail;
	private SimpleContact contact;

	private String clientId;

	public AuthenticationService(final String appName, final String user) {
		this.user = user;

		prefsTokenName = String.format(REFRESH_TOKEN_SUFFIX, appName);

		try {
			contact = new SerializedFile<SimpleContact>(CONTACT_FILE_PREFIX).load();
		} catch (Exception e) {
			contact = new SimpleContact("", "");
		}
	}

	public void setOnAuth(Runnable callback) {
		authCallback = () -> {
			final Task<Void> task = new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					finaliseAuth();
					return null;
				}
			};
			task.setOnSucceeded(e -> callback.run());
			task.setOnFailed(e -> authFailedCallback.run());
			ThreadPool.getDefault().mustRun("refreshToken", task);
		};
	}

	protected void finaliseAuth() {
		// finalise contact and save
		try {
			contact = new SimpleContact(contact == null? "": contact.getEmail(), getFullName());
			new SerializedFile<SimpleContact>(CONTACT_FILE_PREFIX + user).save(contact);
		} catch (IOException e) {
			exception = new YahooException("building contact", e);
			authFailedCallback.run();
			return;
		}

		// connect to yahoo store
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");
		props.setProperty("mail.imaps.auth.mechanisms", "XOAUTH2");
		props.setProperty("mail.imaps.sasl.enable", "true");
		props.setProperty("mail.imaps.ssl.enable", "true");
		props.setProperty("mail.imaps.port", "993");
		try {
			Session session = Session.getDefaultInstance(props, null);
			yahooMail = session.getStore("imaps");
			yahooMail.connect("imap.mail.yahoo.com", clientId, accessToken);
		} catch (Exception e) {
			exception = new YahooException("build session", e);
			authFailedCallback.run();
			return;
		}

		// save refresh token
		try {
			final Preferences prefs = Preferences.userNodeForPackage(YahooMailService.class);
			prefs.put(prefsTokenName, refreshToken);
			prefs.flush();
		} catch (final Exception e) {
			exception = new YahooException("save refresh token", e);
			authFailedCallback.run();
			return;
		}
	}

	public void authenticate() {
		final String clientSecret;
		try {
			try (final BufferedReader reader = new BufferedReader(
					new InputStreamReader(getClass().getResourceAsStream(CLIENT_ID_PATH)))) {
				clientId = reader.readLine();
			}
			try (final BufferedReader reader = new BufferedReader(
					new InputStreamReader(getClass().getResourceAsStream(CLIENT_SECRET_PATH)))) {
				clientSecret = reader.readLine();
			}
		} catch (final Exception e) {
			exception = new YahooException("loading client id & secret", e);
			authFailedCallback.run();
			return;
		}

		final Task<Boolean> task = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				return refreshToken(clientId, clientSecret);
			}
		};
		task.setOnFailed((e) -> {
			exception = new YahooException(task.getException().getMessage(), task.getException());
			authFailedCallback.run();
		});
		task.setOnSucceeded((e) -> {
			if (task.getValue()) {
				authCallback.run();
			} else {
				if (signin(clientId, clientSecret)) {
					authCallback.run();
				} else {
					authFailedCallback.run();
				}
			}
		});

		ThreadPool.getDefault().mustRun("refreshToken", task);
	}

	public YahooException getException() {
		return exception;
	}

	public void setOnAuthFailed(Runnable callback) {
		authFailedCallback = () -> {
			callback.run();
		};
	}

	public boolean signout() {
		// remove token from local preferences
		final Preferences prefs = Preferences.userNodeForPackage(YahooMailService.class);
		prefs.remove(prefsTokenName);
		try {
			prefs.flush();
		} catch (final BackingStoreException e) {
			exception = new YahooException("remove authentication token", e);
			authFailedCallback.run();
			return false;
		}

		return true;
	}

	public Store getYahooMail() {
		return yahooMail;
	}

	public SimpleContact getContact() {
		return contact;
	}

	private boolean refreshToken(String clientId, String clientSecret) {
		try {
			final Preferences prefs = Preferences.userNodeForPackage(YahooMailService.class);
			final String savedToken = prefs.get(prefsTokenName, null);
			if (savedToken == null) {
				return false;
			}

			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("client_id", clientId));
			params.add(new BasicNameValuePair("client_secret", clientSecret));
			params.add(new BasicNameValuePair("redirect_uri", "oob"));
			params.add(new BasicNameValuePair("refresh_token", savedToken));
			params.add(new BasicNameValuePair("grant_type", "refresh_token"));

			HttpPost httpPost = new HttpPost(GET_TOKEN_URL);
			httpPost.setEntity(new UrlEncodedFormEntity(params));

			HttpResponse response = HttpClients.createDefault().execute(httpPost);
			String jsonString = EntityUtils.toString(response.getEntity());
			JSONObject json = new JSONObject(jsonString);

			accessToken = json.getString("access_token");
			refreshToken = json.getString("refresh_token");

			return true;
		} catch (Exception e) {
			exception = new YahooException("refreshing token", e);
			authFailedCallback.run();
			return false;
		}
	}

	private boolean signin(String clientId, String clientSecret) {
		try {
			// request authentication code
			String url = REQUEST_AUTH_URL + String.format(REQUEST_AUTH_PARAM, clientId);
			SigninDialog dialog = new SigninDialog(url);
			String code = dialog.requestCode();
			contact = new SimpleContact(dialog.getEmail(), "n/d");

			// exchange it for a token
			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("client_id", clientId));
			params.add(new BasicNameValuePair("client_secret", clientSecret));
			params.add(new BasicNameValuePair("redirect_uri", "oob"));
			params.add(new BasicNameValuePair("code", code));
			params.add(new BasicNameValuePair("grant_type", "authorization_code"));

			HttpPost httpPost = new HttpPost(GET_TOKEN_URL);
			httpPost.setEntity(new UrlEncodedFormEntity(params));

			HttpResponse response = HttpClients.createDefault().execute(httpPost);
			String jsonString = EntityUtils.toString(response.getEntity());
			JSONObject json = new JSONObject(jsonString);

			accessToken = json.getString("access_token");
			refreshToken = json.getString("refresh_token");

			return true;
		} catch (Exception e) {
			exception = new YahooException("signing in", e);
			authFailedCallback.run();
			return false;
		}
	}

	private String getFullName() throws IOException {
		HttpGet httpGet = new HttpGet(USER_INFO_URL);
		httpGet.addHeader("Authorization", "Bearer " + accessToken);
		HttpResponse response = HttpClients.createDefault().execute(httpGet);
		String jsonString = EntityUtils.toString(response.getEntity());
		JSONObject json = new JSONObject(jsonString);

		return json.getString("name");
	}
}
