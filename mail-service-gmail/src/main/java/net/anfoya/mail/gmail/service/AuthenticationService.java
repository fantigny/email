package net.anfoya.mail.gmail.service;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.gdata.client.contacts.ContactsService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.gmail.GMailException;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.javafx.ConnectionProgress;
import net.anfoya.mail.gmail.javafx.SigninDialog;

public class AuthenticationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

	private static final String URL_REVOKE_TOKEN = "https://accounts.google.com/o/oauth2/revoke?token=%s";
	private static final boolean GUI = !GraphicsEnvironment.isHeadless();

	private static final List<String> SCOPE = Arrays.asList(new String[] {
			"https://www.googleapis.com/auth/gmail.modify",
			"https://www.googleapis.com/auth/gmail.labels",
			"https://www.googleapis.com/auth/contacts.readonly"
	});

	private static final String CLIENT_SECRET_PATH = "client_secret.json";
	private static final String REFRESH_TOKEN_SUFFIX = "%s-refresh-token";

	private final String appName;
	private final String prefsTokenName;

	private ConnectionProgress progress;

	private Gmail gmail;
	private ContactsService contactsService;
	private GoogleCredential credential;

	private final HttpTransport httpTransport;
	private final JsonFactory jsonFactory;

	private Runnable authCallback;
	private Runnable authFailedCallback;

	private GMailException exception;

	public AuthenticationService(final String appName) {
		this.appName = appName;
		prefsTokenName = String.format(REFRESH_TOKEN_SUFFIX, appName);

		httpTransport = new NetHttpTransport();
		jsonFactory = new JacksonFactory();

		credential = null;

		updateProgress(() -> progress = new ConnectionProgress());
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
			task.setOnSucceeded((e) -> callback.run());
			task.setOnFailed((e) -> authFailedCallback.run());
			ThreadPool.getDefault().mustRun("refreshToken", task);
		};
	}

	protected void finaliseAuth() {
		// connect to contact
		updateProgress(() -> progress.setValue(2 / 3d, "Initialise Contacts..."));
		contactsService = new ContactsService(appName);
		contactsService.setOAuth2Credentials(credential);

		// connect to gmail
		updateProgress(() -> progress.setValue(1, "Initialise Gmail..."));
		gmail = new Gmail.Builder(httpTransport, jsonFactory, credential).setApplicationName(appName).build();

		// save refresh token
		try {
			final Preferences prefs = Preferences.userNodeForPackage(GmailService.class);
			prefs.put(prefsTokenName, credential.getRefreshToken());
			prefs.flush();
		} catch (final Exception e) {
			LOGGER.error("saving refresh token", e.getMessage());
		}

		updateProgress(() -> progress.hide());
	}

	public void setOnAuthFailed(Runnable callback) {
		authFailedCallback = () -> {
			updateProgress(() -> progress.hide());
			callback.run();
		};
	}

	public GMailException getException() {
		return exception;
	}

	public void authenticate() {
		final GoogleClientSecrets clientSecrets;
		try {
			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(CLIENT_SECRET_PATH)))) {
				clientSecrets = GoogleClientSecrets.load(jsonFactory, reader);
			}
		} catch (final Exception e) {
			exception = new GMailException("loading client secret", e);
			authFailedCallback.run();
			return;
		}

		credential = new GoogleCredential
				.Builder()
				.setClientSecrets(clientSecrets)
				.setJsonFactory(jsonFactory)
				.setTransport(httpTransport).build();

		final Task<Boolean> task = new Task<Boolean>() {
			@Override protected Boolean call() throws Exception {
				return refreshToken(clientSecrets);
			}
		};
		task.setOnFailed((e) -> {
			exception = new GMailException(task.getException().getMessage(), task.getException());
			authFailedCallback.run();
		});
		task.setOnSucceeded((e) -> {
			if (task.getValue()) {
				authCallback.run();
			} else {
				if (signin(clientSecrets)) {
					authCallback.run();
				} else {
					authFailedCallback.run();
				}
			}
		});

		ThreadPool.getDefault().mustRun("refreshToken", task);
	}

	private boolean refreshToken(GoogleClientSecrets clientSecrets) throws IOException {
		updateProgress(() -> progress.setValue(1 / 3d, "Sign in to Google..."));

		final Preferences prefs = Preferences.userNodeForPackage(GmailService.class);
		final String refreshToken = prefs.get(prefsTokenName, null);
		if (refreshToken == null) {
			return false;
		}

		credential.setRefreshToken(refreshToken);

		try {
			return credential.refreshToken();
		} catch (final Exception e) {
			return false;
		}
	}

	private boolean signin(GoogleClientSecrets clientSecrets) {
		// Allow user to authorise via URL and retrieve authorisation code
		final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow
				.Builder(httpTransport, jsonFactory, clientSecrets, SCOPE)
				.setAccessType("offline")
				.setApprovalPrompt("auto")
				.build();
		final String url = flow
				.newAuthorizationUrl()
				.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI)
				.build();

		// Generate Credential using login token
		final String tokenRequest;
		try {
			tokenRequest = new SigninDialog(url).requestToken();
		} catch (final Exception e) {
			exception = new GMailException("getting token from signin dialog", e);
			return false;
		}

		if (tokenRequest.isEmpty()) {
			exception = new GMailException("authentication aborted");
			return false;
		}

		// Generate token
		final GoogleTokenResponse token;
		try {
			token = flow
					.newTokenRequest(tokenRequest.toString())
					.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI)
					.execute();
		} catch (final IOException e) {
			exception = new GMailException("requesting token", e);
			return false;
		}

		credential.setFromTokenResponse(token);

		return true;
	}

	public boolean signout() {
		// remove token from local preferences
		final Preferences prefs = Preferences.userNodeForPackage(GmailService.class);
		prefs.remove(prefsTokenName);
		try {
			prefs.flush();
		} catch (final BackingStoreException e) {
			exception = new GMailException("remove authentication token", e);
			return false;
		}

		// revoke token (no API call available yet)
		final String url = String.format(URL_REVOKE_TOKEN, credential.getAccessToken());
		try {
			httpTransport
			.createRequestFactory()
			.buildGetRequest(new GenericUrl(url))
			.execute();
		} catch (final IOException e) {
			exception = new GMailException("revoke authentication token", e);
			return false;
		}

		return true;
	}

	public void reconnect() {
	}

	public Gmail getGmail() {
		return gmail;
	}

	public ContactsService getContactsService() {
		return contactsService;
	}

	private void updateProgress(Runnable update) {
		if (!GUI) {
			return;
		}

		Platform.runLater(update);
	}
}