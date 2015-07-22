package net.anfoya.mail.gmail.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import net.anfoya.javafx.application.PlatformHelper;
import net.anfoya.mail.gmail.GMailException;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.javafx.ConnectionProgress;
import net.anfoya.mail.gmail.javafx.GmailLogin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.gdata.client.contacts.ContactsService;

public class ConnectionService {
	private static final Logger LOGGER = LoggerFactory.getLogger(GmailService.class);
	private static final boolean HL = PlatformHelper.isHeadless();

	private static final String CLIENT_SECRET_PATH = "client_secret.json";
    private static final String REFRESH_TOKEN_SUFFIX = "%s-refresh-token";

    private final String appName;
    private final String refreshTokenName;

	private final HttpTransport httpTransport;
	private final JsonFactory jsonFactory;

	private GoogleCredential credential;

	private ConnectionProgress progress;

	private ContactsService gcontact;

	private Gmail gmail;

    public ConnectionService(final String appName) {
    	this.appName = appName;
		refreshTokenName = String.format(REFRESH_TOKEN_SUFFIX, appName);

		httpTransport = new NetHttpTransport();
		jsonFactory = new JacksonFactory();

		credential = null;

		if (!HL) Platform.runLater(() -> progress = new ConnectionProgress());
	}

	public ConnectionService connect() throws GMailException {
		if (!HL) Platform.runLater(() -> progress.setValue(1/3d, "connecting to Google service"));
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(CLIENT_SECRET_PATH)));
			final GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, reader);
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
					refreshToken = null;
				}
			}
			if (refreshToken == null) {
				if (!HL) Platform.runLater(() -> progress.hide());

				// Generate Credential using login token.
				final TokenResponse tokenResponse = new GmailLogin(clientSecrets).getTokenResponseCredentials();
				credential.setFromTokenResponse(tokenResponse);
			}

			// save refresh token
			prefs.put(refreshTokenName, credential.getRefreshToken());
			prefs.flush();

			if (!HL) Platform.runLater(() -> progress.setValue(2/3d, "connecting to GMail"));
			gmail = new Gmail.Builder(httpTransport, jsonFactory, credential)
				.setApplicationName(appName)
				.build();

			if (!HL) Platform.runLater(() -> progress.setValue(1, "connecting to Google Contact"));
			gcontact = new ContactsService(appName);
			gcontact.setOAuth2Credentials(credential);

		} catch (final IOException | BackingStoreException | InterruptedException e) {
			throw new GMailException("connection", e);
		} finally {
			if (!HL) Platform.runLater(() -> progress.hide());
		}

		return this;
	}

	public void disconnect() {
	    final Preferences prefs = Preferences.userNodeForPackage(GmailService.class);
		prefs.remove(refreshTokenName);
		try {
			prefs.flush();
		} catch (final BackingStoreException e) {
			LOGGER.error("removing refresh token", e);
		}
	}

	public void reconnect() {
		//TODO reconnect
	}

	public Gmail getGmailService() throws GMailException {
		return gmail;
	}

	public ContactsService getGcontactService() throws GMailException {
		return gcontact;
	}
}
