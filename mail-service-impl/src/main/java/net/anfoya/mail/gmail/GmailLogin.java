package net.anfoya.mail.gmail;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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

public class GmailLogin {
	private static final List<String> SCOPE = Arrays.asList(new String[] {
			"https://www.googleapis.com/auth/gmail.modify"
			, "https://www.googleapis.com/auth/gmail.labels"
			, "https://www.googleapis.com/auth/contacts.readonly" });
	private static final String LOGIN_SUCESS_PREFIX = "Success code=";
	public static final String TEST_ID = "test";

	private final GoogleClientSecrets clientSecrets;
	private final HttpTransport httpTransport;
	private final JsonFactory jsonFactory;

	public GmailLogin(final GoogleClientSecrets clientSecrets) {
		this.clientSecrets = clientSecrets;

		httpTransport = new NetHttpTransport();
		jsonFactory = new JacksonFactory();
	}

	public TokenResponse getTokenResponseCredentials() throws GMailException, IOException, InterruptedException {
		// Create flow object for the application
		final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets, SCOPE)
				.setAccessType("offline")
				.setApprovalPrompt("auto")
				.build();
		// Allow user to authorize via URL and retrieve authorization code
		final String url = flow
				.newAuthorizationUrl()
				.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI)
				.build();

		final String authCode;
		if (GraphicsEnvironment.isHeadless()) {
			System.out.println("Please open the following URL in your browser then type the authorization code:\n" + url);
			// Read code entered by user.
			final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			authCode = br.readLine();
		} else {
			final WebView webView = new WebView();
			final Stage stage = new Stage(StageStyle.UNIFIED);
			stage.setScene(new Scene(webView, 450, 650));

			final StringBuilder sb = new StringBuilder();
			final WebEngine webEngine = webView.getEngine();
			webEngine.getLoadWorker().stateProperty().addListener((ov, o, n) -> {
				if (n == State.SUCCEEDED) {
					final String title = getTitle(webEngine);
					stage.setTitle(title);
					if (title.length() > LOGIN_SUCESS_PREFIX.length() && title.startsWith(LOGIN_SUCESS_PREFIX)) {
						sb.append(title.substring(LOGIN_SUCESS_PREFIX.length()));
						stage.close();
					}
				}
			});
			webEngine.load(url);
			stage.showAndWait();
			authCode = sb.toString();
		}

		if (authCode.length() == 0) {
			throw new GMailException("no authentication code received from GMail", null);
		}

		// Generate credential
		return flow
				.newTokenRequest(authCode)
				.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI)
				.execute();
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
