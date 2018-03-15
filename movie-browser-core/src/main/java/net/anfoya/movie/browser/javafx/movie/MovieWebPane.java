package net.anfoya.movie.browser.javafx.movie;

import java.util.LinkedHashSet;
import java.util.Set;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebHistory.Entry;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import net.anfoya.javafx.scene.layout.LocationPane;
import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.model.Website;

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

		final WebView view = new WebView();
		view.getEngine().setCreatePopupHandler(new Callback<PopupFeatures, WebEngine>() {
			@Override
			public WebEngine call(final PopupFeatures popupFeatures) {
				return null;
			}
		});
		view.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
			@Override
			public void changed(final ObservableValue<? extends State> ov, final State oldState, final State newState) {
				if (newState == State.RUNNING) {
					final String url = engine.getLocation();
					checkMoviePageUrl(url);
				}
			}
		});
		setCenter(view);

		engine = view.getEngine();

		final WebHistory history = view.getEngine().getHistory();
		final BooleanBinding backwardDisableProperty = Bindings.equal(0, history.currentIndexProperty());
		final BooleanBinding forwardDisableProperty = Bindings.equal(new ListBinding<Entry>() {
		  @Override
		protected ObservableList<Entry> computeValue() {
		     return history.getEntries();
		  }
		}.sizeProperty(), Bindings.add(1, history.currentIndexProperty()));

		final LocationPane locationPane = new LocationPane();
		locationPane.runningProperty().bind(engine.getLoadWorker().runningProperty());
		locationPane.backwardDisableProperty().bind(backwardDisableProperty);
		locationPane.forwardDisableProperty().bind(forwardDisableProperty);
		locationPane.setOnHomeAction(event -> goHome());
		locationPane.setOnReloadAction(event -> engine.reload());
		locationPane.setOnBackAction(event -> goHistory(-1));
		locationPane.setOnForwardAction(event -> goHistory(1));
		locationPane.setOnStopAction(event -> engine.getLoadWorker().cancel());
		if (website.isFreeInput()) {
			locationPane.setOnAction(event -> load(locationPane.locationProperty().get()));
			engine.locationProperty().addListener((ov, oldVal, newVal) -> locationPane.locationProperty().set(newVal));
		} else {
			locationPane.locationProperty().bind(view.getEngine().locationProperty());
		}
		setTop(locationPane);
	}

	public void goHome() {
		load(website.getHomeUrl());
	}

	public void goHistory(final int offset) {
		final WebHistory history = engine.getHistory();
		final int index = history.getCurrentIndex() + offset;
		if (index >= 0 && index < history.getEntries().size()) {
			LOGGER.info("{} - move in history with offset ({}{})", website.getName(), offset>0?"+":"", offset);
			history.go(offset);
		}
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
