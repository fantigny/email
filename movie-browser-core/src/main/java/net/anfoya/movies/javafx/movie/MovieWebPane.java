package net.anfoya.movies.javafx.movie;

import java.util.LinkedHashSet;
import java.util.Set;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import net.anfoya.movies.model.Movie;
import net.anfoya.tools.model.Website;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieWebPane extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(MovieWebPane.class);

	private static final String LOADING = ""
			+ "<br>"
			+ "&nbsp;&nbsp;&nbsp;&nbsp;"
			+ "<font face='verdana'><i>"
				+ "Loading..."
			+ "</i></font>";

	private enum Mode {
		WAIT, LOAD, PASS
	}

	private final WebEngine engine;

	private final Website website;

	private Mode mode;
	private Set<Movie> movies;
	private String currentUrl;

	private Callback<MovieWebPane, Void> savePageCallback;

	public MovieWebPane(final Website website) {
		this.website = website;

		this.mode = Mode.LOAD;
		this.movies = new LinkedHashSet<Movie>();
		this.currentUrl = "";

		final VBox urlBox = new VBox();
		setTop(urlBox);

		final TextField urlField = new TextField();
		if (!website.isSearchable()) {
			urlBox.getChildren().add(urlField);
			urlField.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					// user typed in a new address
					load(urlField.getText());
				}
			});
		}

		final WebView webView = new WebView();
		webView.getEngine().setCreatePopupHandler(new Callback<PopupFeatures, WebEngine>() {
			@Override
			public WebEngine call(final PopupFeatures popupFeatures) {
				return null;
			}
		});
		webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
			@Override
			public void changed(final ObservableValue<? extends State> ov, final State oldState, final State newState) {
				if (newState == State.RUNNING) {
					final String url = engine.getLocation();
					urlField.textProperty().set(url);
					checkMoviePageUrl(url);
				}
			}
		});
		setCenter(webView);

		engine = webView.getEngine();
	}

	private void checkMoviePageUrl(final String url) {
		if (!website.hasDefaultPattern()) {
			// no movie page for this website
			return;
		}
		if (movies.isEmpty()) {
			// no movie selected
			return;
		}

		final Movie movie = movies.iterator().next();
		if (movie.getUrlMap().containsKey(website.getName())
				&& !movie.getUrlMap().get(website.getName()).isEmpty()) {
			// movie page is already defined
			return;
		}

		if (url != null && url.contains(website.getDefaultPattern())) {
			final Alert confirmDialog = new Alert(AlertType.CONFIRMATION
					, "Save this page as default for " + movie.getName() + "?"
					, new ButtonType[] { ButtonType.OK, ButtonType.CANCEL });
			confirmDialog.setHeaderText("");
			confirmDialog.resultProperty().addListener(new ChangeListener<ButtonType>() {
				@Override
				public void changed(final ObservableValue<? extends ButtonType> ov, final ButtonType oldVal, final ButtonType newVal) {
					if (ov.getValue() == ButtonType.OK) {
						savePageCallback.call(MovieWebPane.this);
					}
				}
			});
			confirmDialog.show();
		}
	}

	private void updateMode(final String url, final boolean visible) {
		final boolean sameUrl = url.equals(currentUrl) || url.equals(engine.getLocation());
		final boolean waiting = currentUrl.equals(LOADING);

		switch (mode) {
		case WAIT:
			if (visible) {
				mode = Mode.LOAD;
			} else {
				mode = Mode.PASS;
			}
			break;
		case LOAD:
			if (sameUrl) {
				mode = Mode.PASS;
			} else if (visible) {
				mode = Mode.LOAD;
			} else {
				mode = Mode.WAIT;
			}
			break;
		case PASS:
			if (sameUrl) {
				mode = Mode.PASS;
			} else if (visible) {
				mode = Mode.LOAD;
			} else if (waiting) {
				mode = Mode.PASS;
			} else {
				mode = Mode.WAIT;
			}
			break;
		}
	}

	public void load(final Set<Movie> movies, final boolean visible) {
		this.movies = movies;

		String url = "";
		if (movies.size() == 1) {
			final Movie movie = movies.iterator().next();

			if (movie.getUrlMap().containsKey(website.getName())) {
				// saved movie page
				url = movie.getUrlMap().get(website.getName());
			}

			if (url.isEmpty() && website.isSearchable()) {
				// search page
				url = website.getSearchUrl(movie.getName());
			}
		}

		if (url.isEmpty()) {
			// use website home page
			url = website.getHomeUrl();
		}

		updateMode(url, visible);
		load(url);
	}

	private void load(final String url) {
		if (mode == Mode.LOAD || mode == Mode.WAIT) {
			if (engine.getLoadWorker().isRunning()) {
				// cancel current work
				engine.getLoadWorker().cancel();
			}
		}

		switch(mode) {
		case LOAD:
			// load now
			LOGGER.info("loading [{}, {}]", website.getName(), url);
			engine.load(url);
			currentUrl = url;
			break;
		case WAIT:
			// don't load,
			LOGGER.debug("cleared {}", website.getName());
			setMessage(LOADING);
			currentUrl = LOADING;
			break;
		case PASS:
			// already loaded, nothing to do
			LOGGER.debug("loaded {}", website.getName());
		}
	}

	private void setMessage(final String message) {
		engine.loadContent("<body><html>" + message + "</html></body>");
	}

	public Website getWebsite() {
		return website;
	}

	public void setOnSavePage(final Callback<MovieWebPane, Void> callback) {
		this.savePageCallback = callback;
	}

	public String getUrl() {
		return engine.getLocation();
	}

	public ReadOnlyDoubleProperty progressProperty() {
		return engine.getLoadWorker().progressProperty();
	}

	public ReadOnlyObjectProperty<State> stateProperty() {
		return engine.getLoadWorker().stateProperty();
	}
}
