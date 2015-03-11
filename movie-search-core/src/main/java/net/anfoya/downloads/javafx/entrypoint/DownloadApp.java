package net.anfoya.downloads.javafx.entrypoint;

import java.awt.SplashScreen;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.anfoya.downloads.javafx.ComponentBuilder;
import net.anfoya.downloads.javafx.SearchPane;
import net.anfoya.downloads.javafx.SearchTabs;
import net.anfoya.downloads.javafx.allocine.QuickSearchVo;
import net.anfoya.java.net.PersistentCookieStore;
import net.anfoya.java.net.filtered.engine.RuleSet;
import net.anfoya.java.net.torrent.TorrentHandlerFactory;

public class DownloadApp extends Application {

	public static void main(final String[] args) {
        final SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) {
        	splash.close();
        }
		launch(args);
	}

	private final PersistentCookieStore cookieStore;
	private final RuleSet ruleSet;

	private final SearchTabs searchTabs;
	private final SearchPane searchPane;

	public DownloadApp() {
		final ComponentBuilder compBuilder = new ComponentBuilder();
		cookieStore = compBuilder.buildCookieStore();
		ruleSet = compBuilder.buildRuleSet();
		searchTabs = compBuilder.buildSearchTabs();
		searchPane = compBuilder.buildSearchPane();
	}

	@Override
    public void start(final Stage primaryStage) {
		cookieStore.load();
		CookieHandler.setDefault(new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL));

		ruleSet.load();
		ruleSet.setWithException(false);
		URL.setURLStreamHandlerFactory(new TorrentHandlerFactory(ruleSet));

		initGui(primaryStage);
		initData();
	}

	private void initGui(final Stage primaryStage) {
	    final BorderPane mainPane = new BorderPane();
	    mainPane.setTop(searchPane);
	    mainPane.setCenter(searchTabs);

	    searchTabs.setOnSearched(new Callback<String, Void>() {
			@Override
			public Void call(final String search) {
				searchPane.setSearched(search);
				return null;
			}
		});
	    searchPane.setOnSearchAction(new Callback<QuickSearchVo, Void>() {
	    	@Override
	    	public Void call(final QuickSearchVo resultVo) {
				searchTabs.search(resultVo);
	    		return null;
	    	}
		});

		final Scene scene = new Scene(mainPane, 1150, 800);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/button_flat.css").toExternalForm());
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/combo_noarrow.css").toExternalForm());

		primaryStage.setTitle("Movie Search");
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("Downloads.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
	}

	private void initData() {
		searchTabs.init();
	}
}

