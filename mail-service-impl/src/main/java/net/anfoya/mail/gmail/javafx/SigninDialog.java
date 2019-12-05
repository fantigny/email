package net.anfoya.mail.gmail.javafx;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SigninDialog {
	private static final String LOADING = " loading...";
	private static final String HEAD = "head";
	private static final String TITLE = "title";
	private static final String LOGIN_SUCESS = "Success code=";
	private static final List<String> SCOPE = Arrays.asList(new String[] {
			"https://www.googleapis.com/auth/gmail.modify"
			, "https://www.googleapis.com/auth/gmail.labels"
			, "https://www.googleapis.com/auth/contacts.readonly" });

	private final GoogleClientSecrets clientSecrets;
	private final HttpTransport httpTransport;
	private final JsonFactory jsonFactory;

	public SigninDialog(final GoogleClientSecrets clientSecrets) {
		this.clientSecrets = clientSecrets;

		httpTransport = new NetHttpTransport();
		jsonFactory = new JacksonFactory();

		// workaround Google login issue -- https://stackoverflow.com/questions/44905264/cannot-sign-in-to-google-in-javafx-webview
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
	}

	public TokenResponse getToken() throws IOException {
		// Create flow object for the application
		final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets, SCOPE)
				.setAccessType("offline")
				.setApprovalPrompt("auto")
				.build();
		// Allow user to authorise via URL and retrieve authorisation code
		final String url = flow
				.newAuthorizationUrl()
				.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI)
				.build();

		final String authCode;
		if (GraphicsEnvironment.isHeadless()) {
			System.out.println("Please open the following URL in your browser then copy the authorization code here:\n" + url);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
				authCode = in.readLine();
			}
		} else {
			authCode = getAuthCodeFx(url);
		}

		if (authCode.length() == 0) {
			return null;
		}

		// Generate token
		return flow
				.newTokenRequest(authCode.toString())
				.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI)
				.execute();
	}

	private String getAuthCodeFx(final String url) {
		StringBuffer authCode = new StringBuffer();

		final Stage stage = new Stage(StageStyle.DECORATED);
		final WebView webView = new WebView();
		final WebEngine webEngine = webView.getEngine();
		webEngine.load(url);
		webEngine.getLoadWorker().stateProperty().addListener((ov, o, n) -> {
			if (n == State.SUCCEEDED) {
				authCode.append(getAuthCode(webEngine));
				if (authCode.length() != 0) {
					stage.close();
				}
			}
			stage.setTitle(LOADING);
		});

		stage.getIcons().add(new Image(getClass().getResourceAsStream("googlemail-64.png")));
		stage.setScene(new Scene(webView, 550, 800));
		stage.setTitle(LOADING);
		stage.showAndWait();

		return authCode.toString();
	}

	private String getAuthCode(WebEngine webEngine) {
		String title = getTitle(webEngine.getDocument());
		if (title.startsWith(LOGIN_SUCESS)) {
			return title.substring(LOGIN_SUCESS.length());
		}

		return "";
	}

	private String getTitle(final Document document) {
		try {
			return ((Element) document
					.getElementsByTagName(HEAD)
					.item(0))
					.getElementsByTagName(TITLE)
					.item(0)
					.getTextContent();
		} catch (Exception e) {
			return "";
		}
	}
}
