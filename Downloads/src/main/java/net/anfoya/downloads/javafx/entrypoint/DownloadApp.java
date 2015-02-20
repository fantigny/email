package net.anfoya.downloads.javafx.entrypoint;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;

import net.anfoya.downloads.javafx.ComponentBuilder;
import net.anfoya.downloads.javafx.SearchPane;
import net.anfoya.downloads.javafx.SearchTabs;
import net.anfoya.downloads.javafx.util.TorrentHandlerFactory;
import net.anfoya.tools.net.PersistentCookieStore;
import net.anfoya.tools.net.UrlFilter;
import net.anfoya.tools.util.ThreadPool;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

public class DownloadApp extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	private final PersistentCookieStore cookieStore;
	private final UrlFilter urlFilter;

	private final SearchTabs searchTabs;
	private final SearchPane searchPane;

	public DownloadApp() {
		final ComponentBuilder compBuilder = new ComponentBuilder();
		cookieStore = compBuilder.buildCookieStore();
		urlFilter = compBuilder.buildUrlFilter();
		searchTabs = compBuilder.buildSearchTabs();
		searchPane = compBuilder.buildSearchPane();
	}

	@Override
    public void start(final Stage primaryStage) {
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(final WindowEvent event) {
				cookieStore.save();
				ThreadPool.getInstance().shutdown();
			}
		});

		cookieStore.load();
		CookieHandler.setDefault(new CookieManager(cookieStore, null));

		urlFilter.loadFilters();
		URL.setURLStreamHandlerFactory(new TorrentHandlerFactory(urlFilter));

		initGui(primaryStage);
		initData();
	}

	private void initGui(final Stage primaryStage) {
	    final BorderPane mainPane = new BorderPane();
	    mainPane.setTop(searchPane);
	    mainPane.setCenter(searchTabs);

	    searchTabs.setOnSearchAction(new Callback<String, Void>() {
			@Override
			public Void call(final String search) {
				searchPane.setSearch(search);
				return null;
			}
		});
	    searchPane.setOnSearchAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				searchTabs.search(searchPane.getSearch());
			}
		});

		final Scene scene = new Scene(mainPane, 1150, 800);
		String css = getClass().getResource("/net/anfoya/tools/javafx/scene/control/button_flat.css").toExternalForm();
		scene.getStylesheets().add(css);

		primaryStage.setTitle("Movie Search");
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("Downloads.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
	}

	private void initData() {
		searchTabs.init();
	}
}

