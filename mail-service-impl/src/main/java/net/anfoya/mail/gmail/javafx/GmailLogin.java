package net.anfoya.mail.gmail.javafx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.javafx.application.PlatformHelper;
import net.anfoya.mail.gmail.GMailException;

public class GmailLogin {

	public static final String TEST_ID = "test";

	private static final Logger LOGGER = LoggerFactory.getLogger(GmailLogin.class);
	private static final List<String> SCOPE = Arrays.asList(new String[] {
			"https://www.googleapis.com/auth/gmail.modify"
			, "https://www.googleapis.com/auth/gmail.labels"
			, "https://www.googleapis.com/auth/contacts.readonly" });
	private static final String LOGIN_SUCESS_PREFIX = "Success code=";

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
		if (PlatformHelper.isHeadless()) {
			System.out.println("Please open the following URL in your browser then type the authorization code:\n" + url);
			// Read code entered by user.
			final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			authCode = br.readLine();
		} else {
			authCode = getCredentialsFx(url);
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
		final CountDownLatch fxLock = new CountDownLatch(1);
		final StringBuilder sb = new StringBuilder();

		final Runnable loginRequest = () -> {
			final WebView webView = new WebView();
			final Stage stage = new Stage(StageStyle.UNIFIED);
			stage.setTitle("loading...");
			stage.getIcons().add(new Image(getClass().getResourceAsStream("googlemail-64.png")));
			stage.setScene(new Scene(webView, 450, 650));

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
			fxLock.countDown();
		};

		if (Platform.isFxApplicationThread()) {
			loginRequest.run();
		} else {
			Platform.runLater(loginRequest);
		}

		try {
			fxLock.await();
		} catch (final InterruptedException e) {
			LOGGER.error("waiting for credentials", e);
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
