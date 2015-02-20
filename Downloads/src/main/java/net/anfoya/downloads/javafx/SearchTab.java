package net.anfoya.downloads.javafx;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ListBinding;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebHistory.Entry;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import net.anfoya.downloads.javafx.model.MovieWebsite;
import net.anfoya.tools.javafx.scene.control.TitledProgressBar;

public class SearchTab extends Tab {
	private final MovieWebsite website;

	private final LocationPane locationPane;
	private final WebView view;

	public SearchTab(final MovieWebsite website) {
		this.website = website;

		final BorderPane content = new BorderPane();
		setContent(content);

		view = new WebView();
		view.setContextMenuEnabled(false);
		view.getEngine().setCreatePopupHandler(new Callback<PopupFeatures, WebEngine>() {
			@Override
			public WebEngine call(final PopupFeatures popupFeatures) {
				return null;
			}
		});
		content.setCenter(view);

		WebHistory history = view.getEngine().getHistory();
		BooleanBinding backwardDisableProperty = Bindings.equal(0, history.currentIndexProperty());
		BooleanBinding forwardDisableProperty = Bindings.equal(new ListBinding<Entry>() {
		  protected ObservableList<Entry> computeValue() {
		     return history.getEntries();
		  }
		}.sizeProperty(), Bindings.add(1, history.currentIndexProperty()));
		
		locationPane = new LocationPane();
		locationPane.locationProperty().bind(view.getEngine().locationProperty());
		locationPane.runningProperty().bind(view.getEngine().getLoadWorker().runningProperty());
		locationPane.backwardDisableProperty().bind(backwardDisableProperty);
		locationPane.forwardDisableProperty().bind(forwardDisableProperty);
		locationPane.setOnHomeAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				goHome();
			}
		});
		locationPane.setOnReloadAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				view.getEngine().reload();
			}
		});
		locationPane.setOnBackAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				goHistory(-1);
			}
		});
		locationPane.setOnForwardAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				goHistory(1);
			}
		});
		locationPane.setOnStopAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				view.getEngine().getLoadWorker().cancel();
			}
		});
		content.setTop(locationPane);

		final TitledProgressBar progressTitle = new TitledProgressBar(website.getName());
		progressTitle.setPrefWidth(120);
		progressTitle.progressProperty().bind(view.getEngine().getLoadWorker().progressProperty());
		progressTitle.stateProperty().bind(view.getEngine().getLoadWorker().stateProperty());
		setGraphic(progressTitle);
	}

	public void goHome() {
		view.getEngine().load(website.getHomeUrl());
	}

	public void goHistory(final int offset) {
		final WebHistory history = view.getEngine().getHistory();
		final int index = history.getCurrentIndex() + offset;
		if (index >= 0 && index < history.getEntries().size()) {
			history.go(offset);
		}
	}

	public void search(final String text) {
		if (!text.isEmpty() && website.isSearchable()) {
			view.getEngine().load(website.getSearchUrl(text));
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
		return website.isSearchable();
	}

	public String getName() {
		return website.getName();
	}
}
