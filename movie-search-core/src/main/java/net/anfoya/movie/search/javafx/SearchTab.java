package net.anfoya.movie.search.javafx;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ListBinding;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebHistory.Entry;
import javafx.scene.web.WebView;
import net.anfoya.javafx.scene.control.TitledProgressBar;
import net.anfoya.movie.connector.MovieConnector;
import net.anfoya.movie.connector.MovieVo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchTab extends Tab {
	private static final Logger LOGGER = LoggerFactory.getLogger(SearchTab.class);

	private final LocationPane locationPane;
	private final WebView view;

	private final MovieConnector connector;

	public SearchTab(final MovieConnector connector) {
		this.connector = connector;

		final BorderPane content = new BorderPane();
		setContent(content);

		view = new WebView();
		view.setContextMenuEnabled(false);
		view.getEngine().setCreatePopupHandler(popupFeatures -> null);
		content.setCenter(view);

		final WebHistory history = view.getEngine().getHistory();
		final BooleanBinding backwardDisableProperty = Bindings.equal(0, history.currentIndexProperty());
		final BooleanBinding forwardDisableProperty = Bindings.equal(new ListBinding<Entry>() {
		  @Override
		protected ObservableList<Entry> computeValue() {
		     return history.getEntries();
		  }
		}.sizeProperty(), Bindings.add(1, history.currentIndexProperty()));

		locationPane = new LocationPane();
		locationPane.locationProperty().bind(view.getEngine().locationProperty());
		locationPane.runningProperty().bind(view.getEngine().getLoadWorker().runningProperty());
		locationPane.backwardDisableProperty().bind(backwardDisableProperty);
		locationPane.forwardDisableProperty().bind(forwardDisableProperty);
		locationPane.setOnHomeAction(event -> goHome());
		locationPane.setOnReloadAction(event -> view.getEngine().reload());
		locationPane.setOnBackAction(event -> goHistory(-1));
		locationPane.setOnForwardAction(event -> goHistory(1));
		locationPane.setOnStopAction(event -> view.getEngine().getLoadWorker().cancel());
		content.setTop(locationPane);

		final TitledProgressBar progressTitle = new TitledProgressBar(connector.getName());
		progressTitle.setPrefWidth(120);
		progressTitle.progressProperty().bind(view.getEngine().getLoadWorker().progressProperty());
		progressTitle.stateProperty().bind(view.getEngine().getLoadWorker().stateProperty());
		setGraphic(progressTitle);
	}

	public void goHome() {
		LOGGER.info("{} - going home", connector.getName());
		view.getEngine().load(connector.getHomeUrl());
	}

	public void goHistory(final int offset) {
		final WebHistory history = view.getEngine().getHistory();
		final int index = history.getCurrentIndex() + offset;
		if (index >= 0 && index < history.getEntries().size()) {
			LOGGER.info("{} - move in history with offset ({}{})", connector.getName(), offset>0?"+":"", offset);
			history.go(offset);
		}
	}

	public void search(final String pattern) {
		LOGGER.info("{} - search ({})", connector.getName(), pattern);
		search(connector.find(pattern));
	}

	public void search(final MovieVo movieVo) {
		final String address = movieVo.getUrl();
		if (!address.isEmpty() && movieVo.getSource().equals(connector.getName())) {
			LOGGER.info("{} - load ({})", connector.getName(), address);
			view.getEngine().load(address);
		} else {
			search(movieVo.getName());
		}
	}

	public void setOnViewClicked(final EventHandler<MouseEvent> handler) {
		view.setOnMouseClicked(handler);
	}

	public String getSelection() {
		String selection = (String) view.getEngine().executeScript("window.getSelection().toString()");
		if (selection == null) {
			selection = "";
		}
		return selection;
	}

	public boolean isSearchable() {
		return connector.isSearchable();
	}

	public String getName() {
		return connector.getName();
	}
}
