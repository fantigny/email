package net.anfoya.mail.gmail.service;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.gdata.client.contacts.ContactsService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.gmail.GMailException;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.javafx.ConnectionProgress;
import net.anfoya.mail.gmail.javafx.SigninDialog;

public class AuthenticationService {
	private static final boolean GUI = !GraphicsEnvironment.isHeadless();
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

	private static final String CLIENT_SECRET_PATH = "client_secret.json";
	private static final String REFRESH_TOKEN_SUFFIX = "%s-refresh-token";

	private final String appName;
	private final String refreshTokenName;

	private final HttpTransport httpTransport;
	private final JsonFactory jsonFactory;

	private Stage stage;

	private GoogleCredential credential;

	private ConnectionProgress progress;

	private ContactsService gcontact;

	private Gmail gmail;

	private Runnable authCallback;
	private Runnable authFailedCallback;

	private GMailException exception;

	public AuthenticationService(final String appName) {
		this.appName = appName;
		refreshTokenName = String.format(REFRESH_TOKEN_SUFFIX, appName);

		httpTransport = new NetHttpTransport();
		jsonFactory = new JacksonFactory();

		credential = null;

		updateProgress(() -> {
			progress = new ConnectionProgress();
			stage = new Stage(StageStyle.UNDECORATED);
			stage.setScene(progress);
			stage.sizeToScene();
			stage.show();
		});
	}

	public void setOnAuth(Runnable callback) {
		authCallback = () -> {
			Task<Void> task = new Task<Void>() {
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
		AuthenticationService.this.updateProgress(() -> progress.setValue(2/3d, "connect to contact..."));
		gcontact = new ContactsService(appName);
		gcontact.setOAuth2Credentials(credential);

		// connect to gmail
		AuthenticationService.this.updateProgress(() -> progress.setValue(1, "connect to mail..."));
		gmail = new Gmail.Builder(httpTransport, jsonFactory, credential)
				.setApplicationName(appName)
				.build();

		// save refresh token
		try {
			final Preferences prefs = Preferences.userNodeForPackage(GmailService.class);
			prefs.put(refreshTokenName, credential.getRefreshToken());
			prefs.flush();
		} catch (Exception e) {
			LOGGER.error("saving refresh token", e.getMessage());
		}

		updateProgress(() -> stage.hide());
	}

	public void setOnAuthFailed(Runnable callback) {
		authFailedCallback = () -> {
			updateProgress(() -> stage.hide());
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
		} catch (Exception e) {
			authFailedCallback.run();
			return;
		}

		credential = new GoogleCredential.Builder()
				.setClientSecrets(clientSecrets)
				.setJsonFactory(jsonFactory)
				.setTransport(httpTransport)
				.build();

		Task<Boolean> task = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				return refresh(clientSecrets);
			}
		};
		task.setOnFailed((e) -> authFailedCallback.run());
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

	private boolean refresh(GoogleClientSecrets clientSecrets) throws IOException {
		updateProgress(() -> progress.setValue(1/3d, "Google sign in..."));

		final Preferences prefs = Preferences.userNodeForPackage(GmailService.class);
		String refreshToken = prefs.get(refreshTokenName, null);
		if (refreshToken == null) {
			return false;
		}

		credential.setRefreshToken(refreshToken);

		return credential.refreshToken();
	}

	private boolean signin(GoogleClientSecrets clientSecrets) {
		// Generate Credential using login token.
		final TokenResponse token;
		try {
			token = new SigninDialog(clientSecrets).getToken();
		} catch (Exception e) {
			LOGGER.error("getting token from signin dialog", e.getMessage());
			return false;
		}
		credential.setFromTokenResponse(token);

		return true;
	}

	public void signout() {
		// remove token from local preferences
		final Preferences prefs = Preferences.userNodeForPackage(GmailService.class);
		prefs.remove(refreshTokenName);
		try {
			prefs.flush();
		} catch (final BackingStoreException e) {
			LOGGER.error("remove authentication token", e);
		}

		// revoke token
		try {
			final GenericUrl url = new GenericUrl(String.format(
					"https://accounts.google.com/o/oauth2/revoke?token=%s"
					, credential.getAccessToken()));
			httpTransport
			.createRequestFactory()
			.buildGetRequest(url)
			.execute();
		} catch (final IOException e) {
			LOGGER.error("revoke authentication token", e);
		}

		// reset cookies
		CookieManager.setDefault(new CookieManager());
	}

	public void reconnect() {
	}

	public Gmail getGmailService() {
		return gmail;
	}

	public ContactsService getGcontactService() {
		return gcontact;
	}

	private void updateProgress(Runnable runnable) {
		if (GUI) {
			if (Platform.isFxApplicationThread()) {
				runnable.run();
			} else {
				Platform.runLater(runnable);
			}
		}
	}
}
