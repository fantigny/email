package net.anfoya.mail.gmail.javafx;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.mail.gmail.GMailException;

public class SigninDialog {
	private static final List<String> SCOPE = Arrays.asList(new String[] {
			"https://www.googleapis.com/auth/gmail.modify"
			, "https://www.googleapis.com/auth/gmail.labels"
			, "https://www.googleapis.com/auth/contacts.readonly" });
	private static final String LOGIN_SUCESS_PREFIX = "Success code=";

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

	public TokenResponse getTokenResponseCredentials() throws GMailException, IOException, InterruptedException {
		// Create flow object for the application
		final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets, SCOPE)
				.setAccessType("offline")
				.setApprovalPrompt("auto")
				.build();
		// Allow user to authorise via URL and retrieve authorisation code
		final String uri = flow
				.newAuthorizationUrl()
				.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI)
				.build();

		final String authCode;
		if (GraphicsEnvironment.isHeadless()) {
			System.out.println("Please open the following URL in your browser then type the authorization code:\n" + uri);
			authCode = new BufferedReader(new InputStreamReader(System.in)).readLine();
		} else {
			authCode = getCredentialsFx(uri);
		}

		if (authCode.length() == 0) {
			return null;
		}

		// Generate credential
		return flow
				.newTokenRequest(authCode)
				.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI)
				.execute();
	}

	private String getCredentialsFx(final String url) {
		final StringBuilder sb = new StringBuilder();

		final Runnable loginRequest = () -> {
			final WebView webView = new WebView();
			final Stage stage = new Stage(StageStyle.UNIFIED);
			stage.setTitle(" loading...");
			stage.getIcons().add(new Image(getClass().getResourceAsStream("googlemail-64.png")));
			stage.setScene(new Scene(webView, 550, 800));

			final WebEngine webEngine = webView.getEngine();
			webEngine.getLoadWorker().stateProperty().addListener((ov, o, n) -> {
				switch(n) {
				case SUCCEEDED: {
					final String title = getTitle(webEngine);
					stage.setTitle(title);
					if (title.startsWith(LOGIN_SUCESS_PREFIX)) {
						sb.append(title.substring(LOGIN_SUCESS_PREFIX.length()));
						stage.close();
					}
				} break;
				case FAILED: {
					final Throwable exception = webEngine.getLoadWorker().getException();
					final String msg = exception == null? n.toString(): exception.toString();
					Platform.runLater(() -> new Alert(AlertType.ERROR, msg, ButtonType.OK).showAndWait());
				} break;
				default:
				}
			});
			webEngine.load(url);
			stage.showAndWait();
		};

		if (Platform.isFxApplicationThread()) {
			loginRequest.run();
		} else {
			Platform.runLater(loginRequest);
		}

		return sb.toString();
	}

	private String getTitle(final WebEngine webEngine) {
		final NodeList heads = webEngine
				.getDocument()
				.getElementsByTagName("head");
		return getFirstElement(heads)
				.map(h -> h.getElementsByTagName("title"))
				.flatMap(this::getFirstElement)
				.map(Node::getTextContent)
				.orElse("");
	}

	private Optional<Element> getFirstElement(final NodeList nodeList) {
		if (nodeList.getLength() > 0 && nodeList.item(0) instanceof Element) {
			return Optional.of((Element) nodeList.item(0));
		}
		return Optional.empty();
	}
}
