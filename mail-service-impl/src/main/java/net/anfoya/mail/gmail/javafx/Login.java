package net.anfoya.mail.gmail.javafx;

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

public class Login extends Stage {
	private static final String LOGIN_SUCESS_PREFIX = "Success code=";
	private static final int LOGIN_SUCESS_LENGTH = LOGIN_SUCESS_PREFIX.length();

	private final WebEngine webEngine;

	private volatile String authCode;

	public Login() {
		super(StageStyle.UNIFIED);

		final WebView webView = new WebView();
		setScene(new Scene(webView, 450, 650));

		authCode = "";
		webEngine = webView.getEngine();
		webEngine.getLoadWorker().stateProperty().addListener((ovState, oldState, newState) -> {
			if (newState == State.SUCCEEDED) {
				final String title = getTitle(webEngine);
				setTitle(title);
				if (title.length() > LOGIN_SUCESS_LENGTH && title.startsWith(LOGIN_SUCESS_PREFIX)) {
					authCode = title.substring(LOGIN_SUCESS_LENGTH);
					close();
				}
			}
		});
	}

	public String getAuthorizationCode(final String url) throws InterruptedException {
		webEngine.load(url);
		return authCode;
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
