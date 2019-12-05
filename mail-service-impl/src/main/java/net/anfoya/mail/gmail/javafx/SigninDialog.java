package net.anfoya.mail.gmail.javafx;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

	public SigninDialog() {
		// workaround Google login issue -- https://stackoverflow.com/questions/44905264/cannot-sign-in-to-google-in-javafx-webview
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
	}

	public String requestAuthCode(String url) throws IOException {
		final String authCode;
		if (GraphicsEnvironment.isHeadless()) {
			System.out.println("Please open the following URL in your browser then copy the authorization code here:\n" + url);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
				authCode = in.readLine();
			}
		} else {
			authCode = getAuthCodeFx(url);
		}

		return authCode;
	}

	private String getAuthCodeFx(final String url) {
		StringBuffer authCode = new StringBuffer();

		final Stage stage = new Stage(StageStyle.DECORATED);
		final WebView webView = new WebView();
		final WebEngine webEngine = webView.getEngine();
		webEngine.load(url);
		webEngine.getLoadWorker().stateProperty().addListener((ov, o, n) -> {
			String title = LOADING;
			if (n == State.SUCCEEDED) {
				title = getTitle(webEngine.getDocument());
				authCode.append(getAuthCode(title));
				if (authCode.length() != 0) {
					stage.close();
				}
			}
			stage.setTitle(title);
		});

		stage.getIcons().add(new Image(getClass().getResourceAsStream("googlemail-64.png")));
		stage.setScene(new Scene(webView, 550, 800));
		stage.setTitle(LOADING);
		stage.showAndWait();

		return authCode.toString();
	}

	private String getAuthCode(String title) {
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
