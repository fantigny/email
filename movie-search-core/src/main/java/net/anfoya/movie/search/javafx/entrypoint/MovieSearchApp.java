package net.anfoya.movie.search.javafx.entrypoint;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.net.URLStreamHandlerFactory;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import net.anfoya.java.net.cookie.PersistentCookieStore;
import net.anfoya.java.net.url.filter.RuleSet;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.movie.search.javafx.ComponentBuilder;
import net.anfoya.movie.search.javafx.SearchPane;
import net.anfoya.movie.search.javafx.SearchTabs;

public class MovieSearchApp extends Application {

	public static void main(final String[] args) {
		launch(args);
	}

	private final PersistentCookieStore cookieStore;
	private final RuleSet ruleSet;

	private final SearchTabs searchTabs;
	private final SearchPane searchPane;
	private final URLStreamHandlerFactory torrentHandlerFactory;

	public MovieSearchApp() {
		final ComponentBuilder compBuilder = new ComponentBuilder();
		cookieStore = compBuilder.buildCookieStore();
		ruleSet = compBuilder.buildRuleSet();
		torrentHandlerFactory = compBuilder.buildTorrentHandlerFactory();
		searchTabs = compBuilder.buildSearchTabs();
		searchPane = compBuilder.buildSearchPane();
	}

	@Override
	public void init() throws Exception {
		super.init();
	}

	@Override
    public void start(final Stage mainStage) {
		cookieStore.load();
		CookieHandler.setDefault(new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL));

		ruleSet.load();
		ruleSet.setWithException(false);
		URL.setURLStreamHandlerFactory(torrentHandlerFactory);

		initGui(mainStage);
		initData();
	}

	private void initGui(final Stage mainStage) {
	    final BorderPane mainPane = new BorderPane();
	    mainPane.setTop(searchPane);
	    mainPane.setCenter(searchTabs);

	    searchTabs.setOnSearched(search -> {
			searchPane.setSearched(search);
			return null;
		});
	    searchPane.setOnSearchAction(resultVo -> {
			searchTabs.search(resultVo);
			return null;
		});

		final Scene scene = new Scene(mainPane, 1150, 800);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/button_flat.css").toExternalForm());
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/combo_noarrow.css").toExternalForm());

		mainStage.setTitle("Movie Search");
		mainStage.getIcons().add(new Image(getClass().getResourceAsStream("MovieSearch.png")));
        mainStage.setScene(scene);
		mainStage.show();
	}

	private void initData() {
		searchTabs.init();
	}
}

