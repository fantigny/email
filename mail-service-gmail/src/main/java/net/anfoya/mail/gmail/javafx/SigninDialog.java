package net.anfoya.mail.gmail.javafx;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.webkit.network.CookieManager;

import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SigninDialog {
	private static final String LOADING = " Please wait, page is loading...";
	private static final String HEAD = "head";
	private static final String TITLE = "title";
	private static final String TOKEN_PREFIX = "Success code=";

	private final String url;

	public SigninDialog(String url) {
		this.url = url;

		CookieManager.setDefault(new CookieManager());
	}

	public String requestToken() throws IOException {
		return GraphicsEnvironment.isHeadless()?
				getTokenFromConsole():
					getTokenFromGui();
	}

	private String getTokenFromConsole() {
		System.out.println("Please open the following URL in your browser: " + url);
		System.out.println("then copy the authorization code here: ");
		try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
			return in.readLine();
		} catch (final IOException e) {
			return "";
		}
	}

	private String getTokenFromGui() {
		final StringBuffer token = new StringBuffer();

		final Stage stage = new Stage(StageStyle.DECORATED);
		final WebView webView = new WebView();
		final WebEngine webEngine = webView.getEngine();
		webEngine.load(url);
		webEngine.getLoadWorker().stateProperty().addListener((ov, o, n) -> {
			String title = LOADING;
			if (n == State.SUCCEEDED) {
				title = getTitle(webEngine.getDocument());
				if (title.startsWith(TOKEN_PREFIX)) {
					token.append(title.substring(TOKEN_PREFIX.length()));
					stage.hide();
				}
			}
			stage.setTitle(title);
		});

		stage.getIcons().add(new Image(getClass().getResourceAsStream("googlemail-64.png")));
		stage.setScene(new Scene(webView, 550, 800));
		stage.setTitle(LOADING);
		stage.showAndWait();

		return token.toString();
	}

	private String getTitle(final Document document) {
		try {
			return ((Element) document
					.getElementsByTagName(HEAD)
					.item(0))
					.getElementsByTagName(TITLE)
					.item(0)
					.getTextContent();
		} catch (final Exception e) {
			return "";
		}
	}
}
