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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker.State;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Duration;
import net.anfoya.cluster.Status;
import net.anfoya.cluster.StatusManager;
import net.anfoya.cluster.UpdateManager;
import net.anfoya.java.net.PersistentCookieStore;
import net.anfoya.java.net.filtered.FilteredHandlerFactory;
import net.anfoya.java.net.filtered.engine.RuleSet;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.movie.browser.javafx.ComponentBuilder;
import net.anfoya.movie.browser.javafx.consolidation.FileConsolidationService;
import net.anfoya.movie.browser.javafx.consolidation.MovieConsolidationService;
import net.anfoya.movie.browser.javafx.movie.MoviePane;
import net.anfoya.movie.browser.javafx.movielist.MovieListPane;
import net.anfoya.movie.browser.javafx.taglist.SectionListPane;
import net.anfoya.movie.browser.model.Movie;
import net.anfoya.movie.browser.model.Profile;
import net.anfoya.movie.browser.model.Section;
import net.anfoya.movie.browser.model.Tag;
import net.anfoya.movie.browser.service.MovieService;

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

	private final SectionListPane sectionListPane;
	private final MovieListPane movieListPane;
	private final MoviePane moviePane;

	private final PersistentCookieStore cookieStore;
	private final RuleSet urlFilter;

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
		this.urlFilter = compBuilder.buildUrlFilter();
	}

	@Override
    public void start(final Stage primaryStage) {
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(final WindowEvent event) {
				ThreadPool.getInstance().shutdown();
				statusMgr.shutdown();
			}
		});

		urlFilter.load();
		urlFilter.setWithException(false);
		URL.setURLStreamHandlerFactory(new FilteredHandlerFactory(urlFilter));

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
			sectionListPane.setTagChangeListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(final ObservableValue<? extends Boolean> ov, final Boolean oldVal, final Boolean newVal) {
					// refresh movie list when a tag is (un)selected
					refreshMovieList();
				}
			});
			sectionListPane.setUpdateSectionCallback(new Callback<Void, Void>() {
				@Override
				public Void call(final Void v) {
					updateMovieCount();
					return null;
				}
			});
			if (profile != Profile.RESTRICTED) {
				selectionPane.getChildren().add(sectionListPane);
			}
		}

		/* movie list */ {
			movieListPane.setPrefWidth(250);
			movieListPane.prefHeightProperty().bind(selectionPane.heightProperty());
			movieListPane.addSelectionListener(new ChangeListener<Movie>() {
				@Override
				public void changed(final ObservableValue<? extends Movie> ov, final Movie oldVal, final Movie newVal) {
					if (!movieListPane.isRefreshing()) {
						// update movie details when (a) movie(s) is/are selected
						refreshMovie();
					}
				}
			});
			movieListPane.addChangeListener(new ListChangeListener<Movie>() {
				@Override
				public void onChanged(final ListChangeListener.Change<? extends Movie> change) {
					// update movie count when a new movie list is loaded
					updateMovieCount();
					if (!movieListPane.isRefreshing()) {
						// update movie details in case no movie is selected
						refreshMovie();
					}
				}
			});

			selectionPane.getChildren().add(movieListPane);
		}

		/* movie panel */ {
			moviePane.prefHeightProperty().bind(mainPane.heightProperty());
			moviePane.setOnAddTag(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					refreshSectionList();
					refreshMovieList();
				}
			});
			moviePane.setOnDelTag(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					refreshSectionList();
					refreshMovieList();
				}
			});
			moviePane.setOnCreateTag(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					refreshSectionList();
					refreshMovieList();
				}
			});
			moviePane.setOnUpdateMovie(new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent event) {
					refreshMovieList();
				}
			});

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
		fileConsoService.setOnMovieUpdated(new Callback<Map<Movie, String>, Void>() {
			@Override
			public Void call(final Map<Movie, String> movieMap) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						consolidateFolders(movieMap);
					}
				});
				return null;
			}
		});
		fileConsoService.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(final WorkerStateEvent event) {
				displayConsolidationError(event);
			}
		});

		movieConsoService.reset();
		movieConsoService.setOnToManyDelete(new Callback<Set<Movie>, Void>() {
			@Override
			public Void call(final Set<Movie> movies) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						confirmDeletion(movies);
					}
				});
				return null;
			}
		});
		movieConsoService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(final WorkerStateEvent event) {
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
			}
		});
		movieConsoService.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(final WorkerStateEvent event) {
				displayConsolidationError(event);
			}
		});
		movieConsoService.setDelay(CONSOLIDATION_DELAY);
		movieConsoService.setPeriod(CONSOLIDATION_PERIOD);
		movieConsoService.start();

		updateMgr.addOnUpdate(new Callback<Status, Void>() {
			@Override
			public Void call(final Status status) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						refresh();
					}
				});
				return null;
			}
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
		sectionListPane.refresh();
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
		sectionListPane.updateMovieCount(currentCount, availableTags, namePattern);
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
