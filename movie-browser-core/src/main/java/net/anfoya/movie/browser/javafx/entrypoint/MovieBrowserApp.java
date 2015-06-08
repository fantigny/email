package net.anfoya.movie.browser.javafx.entrypoint;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker.State;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.anfoya.cluster.StatusManager;
import net.anfoya.cluster.UpdateManager;
import net.anfoya.java.net.cookie.PersistentCookieStore;
import net.anfoya.java.net.url.CustomHandlerFactory;
import net.anfoya.java.net.url.filter.Matcher;
import net.anfoya.java.net.url.filter.RuleSet;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.movie.browser.javafx.ComponentBuilder;
import net.anfoya.movie.browser.javafx.consolidation.FileConsolidationService;
import net.anfoya.movie.browser.javafx.consolidation.MovieConsolidationService;
import net.anfoya.movie.browser.javafx.movie.MoviePane;
import net.anfoya.movie.browser.javafx.movielist.MovieListPane;
import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.model.Profile;
import net.anfoya.movie.browser.model.Section;
import net.anfoya.movie.browser.model.Tag;
import net.anfoya.movie.browser.service.MovieService;
import net.anfoya.tag.javafx.scene.section.SectionListPane;

import org.slf4j.bridge.SLF4JBridgeHandler;

public class MovieBrowserApp extends Application {
	private static final Duration CONSOLIDATION_DELAY = Duration.seconds(5);
	private static final Duration CONSOLIDATION_PERIOD = Duration.minutes(5);

	public static void main(final String[] args) {
		// easing JGroup clustering
		System.setProperty("java.net.preferIPv4Stack", "true");

		// JGroup logging
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		launch(args);
	}

	private final Profile profile;
	private final MovieService movieService;

	private final StatusManager statusMgr;
	private final UpdateManager updateMgr;
	private final MovieConsolidationService movieConsoService;
	private final FileConsolidationService fileConsoService;

	private final SectionListPane<Section, Tag> sectionListPane;
	private final MovieListPane movieListPane;
	private final MoviePane moviePane;

	private final PersistentCookieStore cookieStore;
	private final RuleSet ruleSet;

	public MovieBrowserApp() {
		final ComponentBuilder compBuilder = new ComponentBuilder();
		this.profile = compBuilder.buildProfile();
		this.movieService = compBuilder.buildMovieService();
		this.sectionListPane = compBuilder.buildSectionListPane();
		this.movieListPane = compBuilder.buildMovieListPane();
		this.moviePane = compBuilder.buildMoviePane();
		this.statusMgr = compBuilder.buildStatusManager();
		this.updateMgr = compBuilder.buildUpdateManager();
		this.movieConsoService = compBuilder.buildMovieConsolidationService();
		this.fileConsoService = compBuilder.buildFileConsolidationService();
		this.cookieStore = compBuilder.buildCookieStore();
		this.ruleSet = compBuilder.buildUrlFilter();
	}

	@Override
    public void start(final Stage primaryStage) {
		primaryStage.setOnCloseRequest(event -> {
			ThreadPool.getInstance().shutdown();
			statusMgr.shutdown();
		});

		ruleSet.load();
		ruleSet.setWithException(false);
		URL.setURLStreamHandlerFactory(new CustomHandlerFactory(new Matcher(ruleSet)));

		cookieStore.load();
		CookieHandler.setDefault(new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL));

		initGui(primaryStage);
		initData();
	}

	private void initGui(final Stage primaryStage) {
		final BorderPane mainPane = new BorderPane();
		mainPane.setPadding(new Insets(5));

		final Scene scene = new Scene(mainPane, 1524, 780);
		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/excludebox.css").toExternalForm());
//		scene.getStylesheets().add(getClass().getResource("/net/anfoya/javafx/scene/control/button_flat.css").toExternalForm());

		final HBox selectionPane = new HBox();
		mainPane.setLeft(selectionPane);

		/* tag list */ {
			sectionListPane.setPrefWidth(250);
			sectionListPane.prefHeightProperty().bind(selectionPane.heightProperty());
			sectionListPane.setLazyCount(false);
			sectionListPane.setSectionDisableWhenZero(true);
			sectionListPane.setTagChangeListener((ov, oldVal, newVal) -> {
				refreshMovieList();
			});
			sectionListPane.setUpdateSectionCallback(v -> {
				updateMovieCount();
				return null;
			});
			if (profile != Profile.RESTRICTED) {
				selectionPane.getChildren().add(sectionListPane);
			}
		}

		/* movie list */ {
			movieListPane.setPrefWidth(250);
			movieListPane.prefHeightProperty().bind(selectionPane.heightProperty());
			movieListPane.addSelectionListener((ov, oldVal, newVal) -> {
				if (!movieListPane.isRefreshing()) {
					// update movie details when (a) movie(s) is/are selected
					refreshMovie();
				}
			});
			movieListPane.addChangeListener(change -> {
				// update movie count when a new movie list is loaded
				updateMovieCount();
				if (!movieListPane.isRefreshing()) {
					// update movie details in case no movie is selected
					refreshMovie();
				}
			});

			selectionPane.getChildren().add(movieListPane);
		}

		/* movie panel */ {
			moviePane.prefHeightProperty().bind(mainPane.heightProperty());
			moviePane.setOnAddTag(event -> {
				refreshSectionList();
				refreshMovieList();
			});
			moviePane.setOnDelTag(event -> {
				refreshSectionList();
				refreshMovieList();
			});
			moviePane.setOnCreateTag(event -> {
				refreshSectionList();
				refreshMovieList();
			});
			moviePane.setOnUpdateMovie(event -> refreshMovieList());

			mainPane.setCenter(moviePane);
		}

		primaryStage.setTitle("Movie browser");
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("Movies.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
	}

	private void initData() {
		refreshSectionList();
		if (profile == Profile.RESTRICTED) {
			sectionListPane.selectTag(Section.MEI_LIN, Tag.MEI_LIN_NAME);
		} else  {
			sectionListPane.selectTag(Section.TO_WATCH, Tag.TO_WATCH_NAME);
			sectionListPane.expand(new Section("Style"));
		}

		fileConsoService.reset();
		fileConsoService.setOnMovieUpdated(movieMap -> {
			Platform.runLater(() -> consolidateFolders(movieMap));
			return null;
		});
		fileConsoService.setOnFailed(event -> displayConsolidationError(event));

		movieConsoService.reset();
		movieConsoService.setOnToManyDelete(movies -> {
			Platform.runLater(() -> confirmDeletion(movies));
			return null;
		});
		movieConsoService.setOnSucceeded(event -> {
			if ((boolean) event.getSource().getValue()) {
				refresh();
			}
			if (profile == Profile.ADMINISTRATOR) {
				if (fileConsoService.getState() == State.READY) {
					fileConsoService.start();
				} else {
					fileConsoService.restart();
				}
			}
		});
		movieConsoService.setOnFailed(event -> displayConsolidationError(event));
		movieConsoService.setDelay(CONSOLIDATION_DELAY);
		movieConsoService.setPeriod(CONSOLIDATION_PERIOD);
		movieConsoService.start();

		updateMgr.addOnUpdate(status -> {
			Platform.runLater(() -> refresh());
			return null;
		});
	}

	private void displayConsolidationError(final WorkerStateEvent event) {
		final Alert alertDialog = new Alert(AlertType.ERROR,  event.getSource().getException().getMessage());
		alertDialog.setTitle("Consolidation error");
		alertDialog.show();
	}

	private void refresh() {
		refreshSectionList();
		refreshMovieList();
		refreshMovie();
	}

	private void refreshSectionList() {
		// refresh tag list
		sectionListPane.refreshAsync();
		// refresh available tags in movie tag list
		moviePane.refreshTags();
	}

	private void refreshMovieList() {
		movieListPane.refreshWithTags(sectionListPane.getAllTags(), sectionListPane.getIncludedTags(), sectionListPane.getExcludedTags());
	}

	private void updateMovieCount() {
		final int currentCount = movieListPane.getMovieCount();
		final Set<Tag> availableTags = movieListPane.getMoviesTags();
		final String namePattern = movieListPane.getNamePattern();
		sectionListPane.updateCount(currentCount, availableTags, namePattern);
	}

	private void refreshMovie() {
		final Set<Movie> selectedMovies = movieListPane.getSelectedMovies();
		moviePane.load(selectedMovies);
	}

	private void consolidateFolders(final Map<Movie, String> movieMap) {
		boolean updated = false;
		for(final Entry<Movie, String> entry: movieMap.entrySet()) {
			final Movie movie = entry.getKey();
			final String filenames = entry.getValue();
			final Alert confirmDialog = new Alert(AlertType.CONFIRMATION, "(folder is empty)", new ButtonType[] { ButtonType.OK, ButtonType.CANCEL });
			confirmDialog.setTitle("Folder consolidation");
			confirmDialog.setHeaderText("Do you want to delete foler: " + movie.getPath().substring(0, movie.getPath().lastIndexOf('/')));
			if (!filenames.isEmpty()) {
				confirmDialog.setContentText("folder contains: " + filenames);
			}
			final Optional<ButtonType> response = confirmDialog.showAndWait();
			if (response.isPresent() && response.get() == ButtonType.OK) {
				movieService.consolidateFolder(movie);
				updated = true;
			}
		}
		if (updated) {
			refreshMovieList();
		}
	}

	private void confirmDeletion(final Set<Movie> movies) {
		final Alert confirmDialog = new Alert(AlertType.CONFIRMATION, movies.toString(), new ButtonType[] { ButtonType.OK, ButtonType.CANCEL });
		confirmDialog.setTitle("Movie consolidation");
		confirmDialog.setHeaderText("Do you want to delete");
		final Optional<ButtonType> response = confirmDialog.showAndWait();
		if (response.isPresent() && response.get() == ButtonType.OK) {
			movieService.delMovies(movies);
			refreshMovieList();
		}
	}
}
