package net.anfoya.movie.search.javafx;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import net.anfoya.movie.connector.MovieConnector;
import net.anfoya.movie.connector.MovieVo;
import net.anfoya.movie.search.Config;

public class SearchTabs extends TabPane {
	private ContextMenu menu;
	private Callback<String, Void> searchedCallBack;

	public SearchTabs() {
		setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);

		for(final MovieConnector connector: new Config().getMovieConnectors()) {
			final SearchTab tab = new SearchTab(connector);
			tab.setOnViewClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(final MouseEvent event) {
					if (event.getButton() == MouseButton.SECONDARY && !getSelection().isEmpty()) {
						menu = buildContextMenu();
						menu.show((Node)event.getSource(), event.getScreenX(), event.getScreenY());
					} else {
						if (menu != null) {
							menu.hide();
						}
					}
				}
			});
			getTabs().add(tab);
		}
		getSelectionModel().select(1);
	}

	public void init() {
		for(final Tab tab: getTabs()) {
			((SearchTab) tab).goHome();
		}
	}

	public void setOnSearched(final Callback<String, Void> callBack) {
		this.searchedCallBack = callBack;
	}

	public void search(final MovieVo resultVo) {
		search(getTabs(), resultVo);
	}

	private void search(final List<Tab> tabs, final MovieVo movieVo) {
		for(final Tab tab: tabs) {
			Platform.runLater(() -> ((SearchTab)tab).search(movieVo));
		}
	}

	private void search(final List<Tab> tabs, final String name) {
		for(final Tab tab: tabs) {
			search(tab, name);
		}
	}

	private void search(final Tab tab, final String name) {
		Platform.runLater(() -> ((SearchTab)tab).search(name));
	}

	private ContextMenu buildContextMenu() {
		final ContextMenu menu = new ContextMenu();
		/* search next tabs */ {
			final int nextIndex = getSelectionModel().getSelectedIndex() + 1;
			if (nextIndex < getTabs().size()) {
				final List<Tab> tabs = getTabs().subList(nextIndex, getTabs().size());
				final MenuItem item = new MenuItem("Next Tabs");
				item.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(final ActionEvent event) {
						search(tabs, getSelection());
						getSelectionModel().select(tabs.get(0));
					}
				});
				menu.getItems().add(item);
			}
		}
		/* search others */ {
			final List<Tab> others = new ArrayList<Tab>(getTabs());
			others.remove(getSelectionModel().getSelectedItem());
			final MenuItem item = new MenuItem("Others");
			item.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					search(others, new MovieVo(getSelection()));
					getSelectionModel().select(others.get(0));
				}
			});
			menu.getItems().add(item);
		}
		/* search all */ {
			final MenuItem item = new MenuItem("All");
			item.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					final String selection = getSelection();
					search(getTabs(), new MovieVo(selection));
					searchedCallBack.call(selection);
				}
			});
			menu.getItems().add(item);
		}
		menu.getItems().add(new SeparatorMenuItem());
		/* search individual tabs */ {
			for(final Tab tab: getTabs()) {
				final SearchTab searchTab = (SearchTab) tab;
				if (searchTab.isSearchable()) {
					final MenuItem subItem = new MenuItem(searchTab.getName());
					subItem.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(final ActionEvent event) {
							search(searchTab, getSelection());
							getSelectionModel().select(searchTab);
						}
					});
					menu.getItems().add(subItem);
				}
			}
		}

		return menu;
	}

	private String getSelection() {
		return ((SearchTab) getSelectionModel().getSelectedItem()).getSelection();
	}
}
