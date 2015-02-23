package net.anfoya.downloads.javafx;

import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import net.anfoya.downloads.model.Config;
import net.anfoya.tools.model.Website;

public class SearchTabs extends TabPane {
	private ContextMenu menu;
	private Callback<String, Void> searchCallBack;

	public SearchTabs() {
		setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);

		for(final Website website: new Config().getWebsites()) {
			final SearchTab tab = new SearchTab(website);
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
	}

	public void init() {
		for(final Tab tab: getTabs()) {
			((SearchTab) tab).goHome();
		}
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
								searchTab.search(getSelection());
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

	public void search(final String text) {
		search(getTabs(), text);
	}

	private void search(final List<Tab> tabs, final String text) {
		searchCallBack.call(text);
		for(final Tab tab: tabs) {
			((SearchTab)tab).search(text);
		}
	}

	public Callback<String, Void> getSearchCallBack() {
		return searchCallBack;
	}

	public void setOnSearchAction(final Callback<String, Void> callBack) {
		this.searchCallBack = callBack;
	}

	public EventHandler<ActionEvent> getOnSearchAction() {
		return new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				final TextField searchField = (TextField) event.getSource();
				search(getTabs(), searchField.getText());
			}
		};
	}
}
