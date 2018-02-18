package net.anfoya.mail.browser.javafx.settings;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.javafx.scene.control.RemoveLabel;
import net.anfoya.mail.browser.javafx.css.CssHelper;
import net.anfoya.mail.browser.javafx.util.UrlHelper;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;
import net.anfoya.tag.service.TagException;

public class SettingsDialog<S extends Section, T extends Tag> extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);
    private static final String DEFAULT_CSS = SettingsDialog.class.getResource("/net/anfoya/mail/css/default_browser.css").toExternalForm();
    private static final String HELP_HTML = SettingsDialog.class.getResource("/net/anfoya/mail/help.html").toExternalForm();

    private final MailService<S, T, ? extends Thread, ? extends Message, ? extends Contact> mailService;
    private final UndoService undoService;

    private final Settings settings;
	private final TabPane tabPane;

	private FlowPane hiddenSectionsPane;
	private FlowPane hiddenTagsPane;

	private final Map<PoolPriority, Collection<String>> idTasks;

	public SettingsDialog(final MailService<S, T, ? extends Thread, ? extends Message, ? extends Contact> mailService
			, final UndoService undoService
			, final Settings settings) {
		initStyle(StageStyle.UNIFIED);
		setTitle("FisherMail - profile");
		setOnCloseRequest(e -> settings.saveLater());

		this.mailService = mailService;
		this.undoService = undoService;
		this.settings = settings;

		idTasks = new ConcurrentHashMap<PoolPriority, Collection<String>>();

		tabPane = new TabPane(buildSettingsTab(), buildProxyTab(), buildAboutTab(), buildHelpTab(), buildTaskTab());
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

		final Scene scene = new Scene(tabPane, 600, 400);
		CssHelper.addCommonCss(scene);

		setScene(scene);
	}

	public void showAbout() {
		tabPane.getSelectionModel().select(2);
		show();
	}

	private Tab buildTaskTab() {
		final ListView<String> taskList = new ListView<String>();
		taskList.setPlaceholder(new Label("idle"));
		for(final PoolPriority p: PoolPriority.values()) {
			ThreadPool.getDefault().addOnChange(p, map -> Platform.runLater(() -> taskList.getItems().setAll(getTasks(p, map))));
		}
		return new Tab("tasks", taskList);
	}

	private Set<String> getTasks(PoolPriority priority, Map<Future<?>, String> map) {
		idTasks.put(priority, map.values());
		return (Set<String>) idTasks.values().stream().reduce(new LinkedHashSet<String>(), (a, c) -> a.addAll(c)? a: a);
	}

	private Tab buildHelpTab() {
		final WebView help = new WebView();
		help.getEngine().setUserStyleSheetLocation(DEFAULT_CSS);
		help.getEngine().load(HELP_HTML);

		return new Tab("help", help);
	}

	private Tab buildAboutTab() {
		final VersionHelper checker = new VersionHelper();
		final ImageView image = new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/Mail.png")));

		final Text fishermail = new Text("FisherMail                         ");
		fishermail.setFont(Font.font("Amble Cn", FontWeight.BOLD, 32));
		fishermail.setFill(Color.web("#555555"));

		final Text version = new Text(checker.getMyVersion() + "                     ");
		version.setFont(Font.font("Amble Cn", FontWeight.NORMAL, 18));
		version.setFill(Color.web("#555555"));

		final Text author = new Text("by Fred A.");
		author.setFont(Font.font("Amble Cn", FontWeight.BOLD, 18));
		author.setFill(Color.web("#555555"));

		final FlowPane textPane = new FlowPane(new VBox(8, fishermail, version, author));
		textPane.setAlignment(Pos.CENTER_LEFT);

		final GridPane gridPane = new GridPane();
		gridPane.setStyle("-fx-background-color: #bbbbbb;");

		GridPane.setRowSpan(image, 3);
		GridPane.setMargin(image, new Insets(20));
		GridPane.setVgrow(image, javafx.scene.layout.Priority.ALWAYS);
		GridPane.setValignment(image, VPos.CENTER);
		gridPane.add(image, 0, 0);

		GridPane.setVgrow(textPane, javafx.scene.layout.Priority.ALWAYS);
		gridPane.add(textPane, 1, 0);

		checker.isLastest().addListener((ov, o, n) -> {
			if (!n) {
				Platform.runLater(() -> addVersionMessage(gridPane, checker.getLatestVersion()));
			}
		});
		checker.start();

		return new Tab("about", gridPane);
	}

	private void addVersionMessage(final GridPane gridPane, final String version) {
		final Label newLabel = new Label("new version (" + version + ") available at ");
		newLabel.setTextFill(Color.WHITE);
		final Hyperlink hyperlink = new Hyperlink(Settings.DOWNLOAD_URL.split("/")[2]);
		hyperlink.setTextFill(Color.WHITE);
		hyperlink.setOnAction(e -> {
			UrlHelper.open(Settings.DOWNLOAD_URL);
			hyperlink.setVisited(false);
		});

		final FlowPane newVersionPane = new FlowPane(newLabel, hyperlink);
		newVersionPane.setPadding(new Insets(0, 0, 5, 10));
		GridPane.setColumnSpan(newVersionPane, 2);
		GridPane.setHalignment(newVersionPane, HPos.CENTER);
		gridPane.add(newVersionPane, 0, 2);
	}

	private Tab buildSettingsTab() {
		final SwitchButton toolButton = new SwitchButton();
		toolButton.setSwitchOn(settings.showToolbar().get());
		toolButton.switchOnProperty().addListener((ov, o, n) -> settings.showToolbar().set(n));

		final SwitchButton showExcButton = new SwitchButton();
		showExcButton.setSwitchOn(settings.showExcludeBox().get());
		showExcButton.switchOnProperty().addListener((ov, o, n) -> settings.showExcludeBox().set(n));

		final SwitchButton replyAllDblClickButton = new SwitchButton();
		replyAllDblClickButton.setSwitchOn(settings.replyAllDblClick().get());
		replyAllDblClickButton.switchOnProperty().addListener((ov, o, n) -> settings.replyAllDblClick().set(n));

		final SwitchButton archOnDropButton = new SwitchButton();
		archOnDropButton.setSwitchOn(settings.archiveOnDrop().get());
		archOnDropButton.switchOnProperty().addListener((ov, o, n) -> settings.archiveOnDrop().set(n));

		final TextField popupLifetimeField = new TextField("" + settings.popupLifetime().get());
		popupLifetimeField.setPrefColumnCount(3);
		popupLifetimeField.textProperty().addListener((ov, o, n) -> {
			try {
				final int delay = Integer.parseInt(n);
				settings.popupLifetime().set(delay);
			} catch (final Exception e) {
				((StringProperty)ov).setValue(o);
			}
		});

		final TextArea signatureHtml = new TextArea(settings.
				htmlSignature().get().replaceAll("<br>", "\n"));
		signatureHtml.setPrefRowCount(5);
		signatureHtml.setPrefColumnCount(20);
		signatureHtml.textProperty().addListener((ov, o, n) -> {
			settings.htmlSignature().set(n
					.replaceAll("<[Bb][Rr]>\\n", "<br>")
					.replaceAll("\\n", "<br>"));
		});

		hiddenSectionsPane = new FlowPane(3, 1);
		hiddenSectionsPane.getStyleClass().add("label-list-pane");
		hiddenSectionsPane.setMaxWidth(300);

		hiddenTagsPane = new FlowPane(3, 1);
		hiddenTagsPane.getStyleClass().add("label-list-pane");
		hiddenTagsPane.setMaxWidth(300);

		final Button refreshButton = new Button("boom!");
		refreshButton.setOnAction(e -> {
			mailService.clearCache();
			refreshHiddenLabel();
		});

		final Button resetButton = new Button("clear");
		resetButton.setOnAction(e -> {
			settings.reset();
			close();
		});

		final SwitchButton roamingSettingsButton = new SwitchButton();
		roamingSettingsButton.setSwitchOn(settings.globalSettings().get());
		roamingSettingsButton.switchOnProperty().addListener((ov, o, n) -> settings.globalSettings().set(n));
		roamingSettingsButton.setDisable(true);

		final SwitchButton confirmOnQuitButton = new SwitchButton();
		confirmOnQuitButton.setSwitchOn(settings.confirmOnQuit().get());
		confirmOnQuitButton.switchOnProperty().addListener((ov, o, n) -> settings.confirmOnQuit().set(n));

		final SwitchButton confirmOnSignoutButton = new SwitchButton();
		confirmOnSignoutButton.setSwitchOn(settings.confirmOnSignout().get());
		confirmOnSignoutButton.switchOnProperty().addListener((ov, o, n) -> settings.confirmOnSignout().set(n));

		final SwitchButton muteButton = new SwitchButton();
		muteButton.setSwitchOn(settings.mute().get());
		muteButton.switchOnProperty().addListener((ov, o, n) -> settings.mute().set(n));

		final SwitchButton proxyButton = new SwitchButton();
		proxyButton.setSwitchOn(settings.proxyEnabled().get());
		proxyButton.switchOnProperty().addListener((ov, o, n) -> settings.proxyEnabled().set(n));

		final TextField proxyHost = new TextField(settings.proxyHost().get());
		proxyHost.setPrefColumnCount(3);
		proxyHost.textProperty().addListener((ov, o, n) -> {
			try {
				settings.proxyHost().set(n);
			} catch (final Exception e) {
				((StringProperty)ov).setValue(o);
			}
		});

		final TextField proxyPort = new TextField("" + settings.proxyPort().get());
		proxyPort.setPrefColumnCount(3);
		proxyPort.textProperty().addListener((ov, o, n) -> {
			try {
				final int port = Integer.parseInt(n);
				settings.proxyPort().set(port);
			} catch (final Exception e) {
				((StringProperty)ov).setValue(o);
			}
		});

		final TextField proxyUser = new TextField(settings.proxyUser().get());
		proxyUser.setPrefColumnCount(3);
		proxyUser.textProperty().addListener((ov, o, n) -> {
			try {
				settings.proxyUser().set(n);
			} catch (final Exception e) {
				((StringProperty)ov).setValue(o);
			}
		});

		final TextField proxyPasswd = new TextField(settings.proxyPasswd().get());
		proxyPasswd.setPrefColumnCount(3);
		proxyPasswd.textProperty().addListener((ov, o, n) -> {
			try {
				settings.proxyPasswd().set(n);
			} catch (final Exception e) {
				((StringProperty)ov).setValue(o);
			}
		});

		final SwitchButton proxyBasicAuth = new SwitchButton();
		proxyBasicAuth.setSwitchOn(settings.proxyBasicAuth().get());
		proxyBasicAuth.switchOnProperty().addListener((ov, o, n) -> settings.proxyBasicAuth().set(n));

		final GridPane gridPane = new GridPane();
		gridPane.setPadding(new Insets(5));
		gridPane.setVgap(5);
		gridPane.setHgap(10);

		int i = 0;
		gridPane.add(new Label("signature (html is welcome)")					, 0, i);
		gridPane.add(signatureHtml												, 1, i, 1, 2);
		i += 2;
		gridPane.add(new Label("popup lifetime in seconds (0 for permanent)")	, 0, i);
		gridPane.add(popupLifetimeField											, 1, i);
		i++;
		gridPane.add(new Label("show tool bar")									, 0, i);
		gridPane.add(toolButton													, 1, i);
		i++;
		gridPane.add(new Label("archive on drop")								, 0, i);
		gridPane.add(archOnDropButton											, 1, i);
		i++;
		gridPane.add(new Label("roaming settings")								, 0, i);
		gridPane.add(roamingSettingsButton										, 1, i);
		i++;
		gridPane.add(new Label("mute")											, 0, i);
		gridPane.add(muteButton													, 1, i);
		i++;
		gridPane.add(new Label("confirm on quit")								, 0, i);
		gridPane.add(confirmOnQuitButton										, 1, i);
		i++;
		gridPane.add(new Label("confirm on signout")							, 0, i);
		gridPane.add(confirmOnSignoutButton										, 1, i);
		i++;
		gridPane.add(new Label("hidden section")								, 0, i);
		gridPane.add(hiddenSectionsPane											, 1, i, 1, 2);
		i += 2;
		gridPane.add(new Label("hidden tag")									, 0, i);
		gridPane.add(hiddenTagsPane												, 1, i, 1, 2);
		i += 2;
		gridPane.add(new Label("show exclude box (restart needed)")				, 0, i);
		gridPane.add(showExcButton												, 1, i);
		i++;
		gridPane.add(new Label("thread list double right click replies all")	, 0, i);
		gridPane.add(replyAllDblClickButton										, 1, i);
		i++;
		gridPane.add(new Label("reset cached data")								, 0, i);
		gridPane.add(refreshButton												, 1, i);
		i++;
		gridPane.add(new Label("reset settings")								, 0, i);
		gridPane.add(resetButton												, 1, i);
		i++;

		refreshHiddenLabel();
		mailService.addOnUpdateTagOrSection(() -> refreshHiddenLabel());

		return new Tab("settings", new ScrollPane(gridPane));
	}

	private Void refreshHiddenLabel() {
		try {
			final Set<Label> labels = new LinkedHashSet<>();
			for(final S s: mailService.getHiddenSections()) {
				final RemoveLabel label = new RemoveLabel(s.getName(), "show");
				label.setOnRemove(e -> show(s, label));
				labels.add(label);
			}
			Platform.runLater(() -> hiddenSectionsPane.getChildren().setAll(labels));
		} catch (final TagException e) {
			LOGGER.error("load hidden sections", e);
		}
		try {
			final Set<Label> labels = new LinkedHashSet<>();
			for(final T t: mailService.getHiddenTags()) {
				final RemoveLabel label = new RemoveLabel(t.getName(), "show");
				label.setOnRemove(e -> show(t, label));
				labels.add(label);
			}
			Platform.runLater(() -> hiddenTagsPane.getChildren().setAll(labels));
		} catch (final TagException e) {
			LOGGER.error("load hidden tags", e);
		}

		return null;
	}

	private Tab buildProxyTab() {
		final SwitchButton proxyButton = new SwitchButton();
		proxyButton.setSwitchOn(settings.proxyEnabled().get());
		proxyButton.switchOnProperty().addListener((ov, o, n) -> settings.proxyEnabled().set(n));

		final TextField proxyHost = new TextField(settings.proxyHost().get());
		proxyHost.setPrefColumnCount(10);
		proxyHost.editableProperty().bind(settings.proxyEnabled());
		proxyHost.textProperty().addListener((ov, o, n) -> {
			try {
				settings.proxyHost().set(n);
			} catch (final Exception e) {
				((StringProperty)ov).setValue(o);
			}
		});

		final TextField proxyPort = new TextField("" + settings.proxyPort().get());
		proxyPort.editableProperty().bind(settings.proxyEnabled());
		proxyPort.setPrefColumnCount(10);
		proxyPort.textProperty().addListener((ov, o, n) -> {
			try {
				final int port = Integer.parseInt(n);
				settings.proxyPort().set(port);
			} catch (final Exception e) {
				((StringProperty)ov).setValue(o);
			}
		});

		final TextField proxyUser = new TextField(settings.proxyUser().get());
		proxyUser.editableProperty().bind(settings.proxyEnabled());
		proxyUser.setPrefColumnCount(10);
		proxyUser.textProperty().addListener((ov, o, n) -> {
			try {
				settings.proxyUser().set(n);
			} catch (final Exception e) {
				((StringProperty)ov).setValue(o);
			}
		});

		final PasswordField proxyPasswd = new PasswordField();
		proxyPasswd.setText(settings.proxyPasswd().get());
		proxyPasswd.editableProperty().bind(settings.proxyEnabled());
		proxyPasswd.setPrefColumnCount(10);
		proxyPasswd.textProperty().addListener((ov, o, n) -> {
			try {
				settings.proxyPasswd().set(n);
			} catch (final Exception e) {
				((StringProperty)ov).setValue(o);
			}
		});

		final SwitchButton proxyBasicAuth = new SwitchButton();
		proxyBasicAuth.enabledProperty().bind(settings.proxyEnabled());
		proxyBasicAuth.setSwitchOn(settings.proxyBasicAuth().get());
		proxyBasicAuth.switchOnProperty().addListener((ov, o, n) -> settings.proxyBasicAuth().set(n));

		final GridPane gridPane = new GridPane();
		gridPane.setPadding(new Insets(5));
		gridPane.setVgap(5);
		gridPane.setHgap(10);

		int i = 0;
		gridPane.add(new Label("enable")										, 0, i);
		gridPane.add(proxyButton												, 1, i);
		i++;
		gridPane.add(new Label("host")											, 0, i);
		gridPane.add(proxyHost													, 1, i);
		i++;
		gridPane.add(new Label("port")											, 0, i);
		gridPane.add(proxyPort													, 1, i);
		i++;
		gridPane.add(new Label("user")											, 0, i);
		gridPane.add(proxyUser													, 1, i);
		i++;
		gridPane.add(new Label("pasword")										, 0, i);
		gridPane.add(proxyPasswd												, 1, i);
		i++;
		gridPane.add(new Label("basic authentication")							, 0, i);
		gridPane.add(proxyBasicAuth												, 1, i);
		i++;

		refreshHiddenLabel();
		mailService.addOnUpdateTagOrSection(() -> refreshHiddenLabel());

		return new Tab("proxy", new ScrollPane(gridPane));
	}

	private Void show(final S section, final Label label) {
		Platform.runLater(() -> hiddenSectionsPane.getChildren().remove(label));
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.show(section);
				return null;
			}
		};
		task.setOnSucceeded(e -> undoService.set(() -> mailService.hide(section), "show"));
		task.setOnFailed(ev -> LOGGER.error("show {}", section.getName(), ev.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "show " + section.getName(), task);

		return null;
	}

	private Void show(final T tag, final Label label) {
		Platform.runLater(() -> hiddenTagsPane.getChildren().remove(label));
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.show(tag);
				return null;
			}
		};
		task.setOnSucceeded(e -> undoService.set(() -> mailService.hide(tag), "show"));
		task.setOnFailed(e -> LOGGER.error("show {}", tag.getName(), e.getSource().getException()));
		ThreadPool.getDefault().submit(PoolPriority.MAX, "show " + tag.getName(), task);

		return null;
	}
}
