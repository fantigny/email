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
import com.google.api.client.auth.oauth2.TokenResponseException;
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
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
		authCallback = callback;
	}

	public void setOnAuthFailed(Runnable callback) {
		authFailedCallback = callback;
	}

	public GMailException getException() {
		return exception;
	}

	public void authenticate() {
		updateProgress(() -> progress.setValue(1/3d, "Google sign in..."));
		try {
			final GoogleClientSecrets clientSecrets;
			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(CLIENT_SECRET_PATH)))) {
				clientSecrets = GoogleClientSecrets.load(jsonFactory, reader);
			}
			credential = new GoogleCredential.Builder()
					.setClientSecrets(clientSecrets)
					.setJsonFactory(jsonFactory)
					.setTransport(httpTransport)
					.build();

			final Preferences prefs = Preferences.userNodeForPackage(GmailService.class);
			String refreshToken = prefs.get(refreshTokenName, null);
			if (refreshToken != null) {
				// Generate Credential using saved token.
				credential.setRefreshToken(refreshToken);
				try {
					credential.refreshToken();
				} catch (final TokenResponseException e) {
					LOGGER.warn("invalid token ({})", e.getMessage());
					refreshToken = null;
				}
			}
			if (refreshToken == null) {
				updateProgress(() -> stage.hide());

				// Generate Credential using login token.
				final TokenResponse token = new SigninDialog(clientSecrets).getToken();
				if (token == null) {
					authFailedCallback.run();
					return;
				}
				credential.setFromTokenResponse(token);
			}

			// save refresh token
			prefs.put(refreshTokenName, credential.getRefreshToken());
			prefs.flush();

			updateProgress(() -> progress.setValue(2/3d, "connect to contact..."));
			gcontact = new ContactsService(appName);
			gcontact.setOAuth2Credentials(credential);

			updateProgress(() -> progress.setValue(1, "connect to mail..."));
			gmail = new Gmail.Builder(httpTransport, jsonFactory, credential)
					.setApplicationName(appName)
					.build();

			authCallback.run();
		} catch (final IOException | BackingStoreException | InterruptedException | GMailException e) {
			exception = new GMailException("connection", e);
		} finally {
			updateProgress(() -> stage.hide());
		}
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
			Platform.runLater(runnable);
		}
	}
}
