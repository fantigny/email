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
	private static final String SUCCESS_CODE = "Success code=";

	private final String url;

	public SigninDialog(String url) {
		this.url = url;

		// workaround Google login issue -- https://stackoverflow.com/questions/44905264/cannot-sign-in-to-google-in-javafx-webview
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
	}

	public String requestAuthCode() throws IOException {
		return GraphicsEnvironment.isHeadless()?
				getAuthCodeConsole():
					getAuthCodeFx();
	}

	private String getAuthCodeConsole() {
		System.out.println("Please open the following URL in your browser: " + url);
		System.out.println("then copy the authorization code here: ");
		try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
			return in.readLine();
		} catch (IOException e) {
			return "";
		}
	}

	private String getAuthCodeFx() {
		StringBuffer authCode = new StringBuffer();

		final Stage stage = new Stage(StageStyle.DECORATED);
		final WebView webView = new WebView();
		final WebEngine webEngine = webView.getEngine();
		webEngine.load(url);
		webEngine.getLoadWorker().stateProperty().addListener((ov, o, n) -> {
			String title = LOADING;
			if (n == State.SUCCEEDED) {
				title = getTitle(webEngine.getDocument());
				if (title.startsWith(SUCCESS_CODE)) {
					authCode.append(title.substring(SUCCESS_CODE.length()));
					stage.hide();
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
