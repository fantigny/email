package net.anfoya.movie.search.javafx;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.movie.connector.MovieConnector;
import net.anfoya.movie.connector.MovieVo;
import net.anfoya.movie.search.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchTabs extends TabPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(SearchTabs.class);

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
//		getSelectionModel().select(1);
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

	private void search(final List<Tab> tabs, final String text) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				search(tabs, new MovieVo(text));
				return null;
			}
		};
		task.setOnSucceeded(event -> searchedCallBack.call(text));
		ThreadPool.getInstance().submitHigh(task);
	}

	private void search(final List<Tab> tabs, final MovieVo movieVo) {
		Platform.runLater(() -> {
			LOGGER.info("search \"{}\" (source=\"{}\", id=\"{}\")", movieVo.getName(), movieVo.getSource(), movieVo.getId());
			for(final Tab tab: tabs) {
				((SearchTab)tab).search(movieVo);
			}
		});
	}

	private ContextMenu buildContextMenu() {
		final ContextMenu menu = new ContextMenu();
		{
			final Tab selectedTab = getSelectionModel().getSelectedItem();
			final Menu item = new Menu("Search Others");
			item.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					if (event.getTarget() instanceof Menu) {
						@SuppressWarnings("serial")
						final List<Tab> others = new ArrayList<Tab>(getTabs()) { { remove(selectedTab); } };
						search(others, getSelection());
						menu.hide();
					}
				}
			});
			for(final Tab tab: getTabs()) {
				if (tab != selectedTab) {
					final SearchTab searchTab = (SearchTab) tab;
					if (searchTab.isSearchable()) {
						final MenuItem subItem = new MenuItem(searchTab.getName());
						subItem.setOnAction(new EventHandler<ActionEvent>() {
							@Override
							public void handle(final ActionEvent event) {
								final String selection = getSelection();
								Platform.runLater(() -> {
									searchTab.search(new MovieVo(selection));
								});
								getSelectionModel().select(searchTab);
							}
						});
						item.getItems().add(subItem);
					}
				}
			}
			menu.getItems().add(item);
		}
		{
			final int nextIndex = getSelectionModel().getSelectedIndex() + 1;
			if (nextIndex < getTabs().size()) {
				final List<Tab> tabs = getTabs().subList(nextIndex, getTabs().size()-1);
				final MenuItem item = new MenuItem("Search Next Tabs");
				item.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(final ActionEvent event) {
						search(tabs, getSelection());
						if (tabs.size() > 0) {
							getSelectionModel().select(tabs.get(0));
						}
					}
				});
				menu.getItems().add(item);
			}
		}
		{
			final MenuItem item = new MenuItem("Search All");
			item.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					search(getTabs(), getSelection());
				}
			});
			menu.getItems().add(item);
		}

		return menu;
	}

	private String getSelection() {
		return ((SearchTab) getSelectionModel().getSelectedItem()).getSelection();
	}
}
