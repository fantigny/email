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
import net.anfoya.downloads.javafx.allocine.AllocineQsResult;
import net.anfoya.javafx.scene.control.TitledProgressBar;
import net.anfoya.tools.model.Website;

public class SearchTab extends Tab {
	private final Website website;

	private final LocationPane locationPane;
	private final WebView view;

	public SearchTab(final Website website) {
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
		locationPane.setOnHomeAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent arg0) {
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
		search(text, AllocineQsResult.getEmptyResult());
	}

	public void search(final String text, final AllocineQsResult qsResult) {
		if (website.getName().equals("AlloCine") && !qsResult.getId().isEmpty()) {
			String searchPattern;
			if (qsResult.isPerson()) {
				searchPattern = "http://www.allocine.fr/personne/fichepersonne_gen_cpersonne=%s.html";
			} else if (qsResult.isSerie()) {
				searchPattern = "http://www.allocine.fr/series/ficheserie_gen_cserie=%s.html";
			} else {
				searchPattern = "http://www.allocine.fr/film/fichefilm_gen_cfilm=%s.html";
			}
			final String url = String.format(searchPattern, qsResult.getId());
			view.getEngine().load(url);
		} else if (!text.isEmpty() && website.isSearchable()) {
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
