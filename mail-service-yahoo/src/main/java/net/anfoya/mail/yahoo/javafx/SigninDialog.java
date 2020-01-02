package net.anfoya.mail.yahoo.javafx;

import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.webkit.network.CookieManager;

import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.mail.yahoo.YahooServiceInfo;

public class SigninDialog {
	private static final String LOADING = " Please wait, page is loading...";
	private static final String HEAD = "head";
	private static final String TITLE = "title";

	private static final String EMAIL_PAGE_REF = "<strong class=\"challenge-heading\">Enter&nbsp;password</strong>";
	private static final String EMAIL_PREFIX = "<div class=\"yid\">";
	private static final String EMAIL_SUFFIX = "</div>";

	private static final String CODE_PAGE_REF = "<p class=\"oauth2-desc\">Use this code to connect and share your Yahoo info with&nbsp;";
	private static final String CODE_PREFIX = "<code class=\"oauth2-code\">";
	private static final String CODE_SUFFIX = "</code>";

	private final String url;
	private String email;
	private String code;

	public SigninDialog(String url) {
		this.url = url;

		CookieManager.setDefault(new CookieManager());
	}

	public String requestCode() throws IOException {
		final Stage stage = new Stage(StageStyle.DECORATED);
		final WebView webView = new WebView();
		final WebEngine webEngine = webView.getEngine();
		webEngine.load(url);
		webEngine.getLoadWorker().stateProperty().addListener((ov, o, n) -> {
			String title = LOADING;
			if (n == State.SUCCEEDED) {
				title = getTitle(webEngine.getDocument());
				String html = (String) webEngine.executeScript("document.documentElement.outerHTML");
				if (html.contains(EMAIL_PAGE_REF)) {
					email = getEmail(html);
				} else if (html.contains(CODE_PAGE_REF)) {
					code = getCode(html);
					if (!code.isEmpty()) {
						stage.hide();
					}
				}
			}
			stage.setTitle(title);
		});

		stage.getIcons().add(new YahooServiceInfo().getIcon());
		stage.setScene(new Scene(webView, 550, 800));
		stage.setTitle(LOADING);
		stage.showAndWait();

		return code.toString();
	}

	public String getEmail() {
		return email;
	}

	private String getCode(String html) {
		int index = html.indexOf(CODE_PREFIX);
		if (index >= 0) {
			html = html.substring(index + CODE_PREFIX.length());
			index = html.indexOf(CODE_SUFFIX);
			if (index >= 0) {
				return html.substring(0, index);
			}
		}

		return "";
	}

	private String getEmail(String html) {
		int index = html.indexOf(EMAIL_PREFIX);
		if (index >= 0) {
			html = html.substring(index + EMAIL_PREFIX.length());
			index = html.indexOf(EMAIL_SUFFIX);
			if (index >= 0) {
				String email = html.substring(0, index);
				if (email.indexOf('@') == -1) {
					email += "@yahoo.com";
				}
				return email;
			}
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
		} catch (final Exception e) {
			return "";
		}
	}
}
