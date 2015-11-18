package net.anfoya.mail.browser.javafx.settings;

import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.RemoveLabel;
import net.anfoya.mail.browser.javafx.css.StyleHelper;
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

	private final MailService<S, T, ? extends Thread, ? extends Message, ? extends Contact> mailService;
	private final TabPane tabPane;

	private FlowPane hiddenSectionsPane;
	private FlowPane hiddenTagsPane;

	public SettingsDialog(MailService<S, T, ? extends Thread, ? extends Message, ? extends Contact> mailService) {
		initStyle(StageStyle.UNIFIED);
		setTitle("FisherMail - profile");
		setOnCloseRequest(e -> Settings.getSettings().save());

		this.mailService = mailService;

		tabPane = new TabPane(buildSettingsTab(), buildAboutTab(), buildHelpTab(), buildTaskTab());
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

		final Scene scene = new Scene(tabPane, 600, 400);
		StyleHelper.addCommonCss(scene);

		setScene(scene);
	}

	public void showAbout() {
		tabPane.getSelectionModel().select(2);
		show();
	}

	private Tab buildTaskTab() {
		final ListView<String> taskList = new ListView<String>();
		ThreadPool.getInstance().setOnChange(map -> refreshTasks(taskList.getItems(), map));
		return new Tab("tasks", taskList);
	}

	private Tab buildHelpTab() {
		final WebView help = new WebView();
		help.getEngine().load(getClass().getResource("/net/anfoya/mail/javafx/settings/help.html").toString());

		return new Tab("help", help);
	}

	private Void refreshTasks(ObservableList<String> taskList, final Map<? extends Future<?>, ? extends String> futureDesc) {
		Platform.runLater(() -> {
			taskList.clear();
			for(final String desc: futureDesc.values()) {
				taskList.add(desc);
			}
			if (taskList.isEmpty()) {
				taskList.add("idle");
			}
		});
		return null;
	}

	private Tab buildAboutTab() {
		final VersionChecker checker = new VersionChecker();
		final ImageView image = new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/Mail.png")));

		final Text fishermail = new Text("FisherMail ");
		fishermail.setFont(Font.font("Amble Cn", FontWeight.BOLD, 24));
		fishermail.setFill(Color.web("#bbbbbb"));

		final Text version = new Text(checker.getVersion());
		version.setFont(Font.font("Amble Cn", FontWeight.BOLD, 14));
		version.setFill(Color.web("#bbbbbb"));

		final Text author = new Text("by Frederic Antigny");
		author.setFont(Font.font("Amble Cn", FontWeight.BOLD, 24));
		author.setFill(Color.web("#bbbbbb"));

		final FlowPane textPane = new FlowPane(fishermail, version, author);
		textPane.setAlignment(Pos.CENTER_LEFT);

		final GridPane gridPane = new GridPane();
		gridPane.setStyle("-fx-background-color: #4d4d4d;");

		GridPane.setRowSpan(image, 2);
		GridPane.setMargin(image, new Insets(20));
		GridPane.setVgrow(image, Priority.ALWAYS);
		GridPane.setValignment(image, VPos.CENTER);
		gridPane.add(image, 0, 0);

		GridPane.setVgrow(textPane, Priority.ALWAYS);
		gridPane.add(textPane, 1, 0);

		final Task<Boolean> isLatestTask = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				return checker.isLastVersion();
			}
		};
		isLatestTask.setOnSucceeded(e -> {
			if (!(boolean)e.getSource().getValue()) {
				addVersionMessage(gridPane, checker.getLastestVesion());
			}
		});
		isLatestTask.setOnFailed(e -> LOGGER.error("getting latest version info", e));
		ThreadPool.getInstance().submitLow(isLatestTask, "checking version");

		return new Tab("about", gridPane);
	}

	private void addVersionMessage(GridPane gridPane, String version) {
		final Label newLabel = new Label("new version (" + version + ") available at ");
		newLabel.setTextFill(Color.WHITE);
		final Label urlLabel = new Label(Settings.URL);
		urlLabel.setTextFill(Color.WHITE);
		urlLabel.setCursor(Cursor.HAND);
		urlLabel.setOnMouseClicked(e -> {
			if (e.getClickCount() == 1 && e.getButton() == MouseButton.PRIMARY) {
				UrlHelper.open("http://" + Settings.URL);
			}
		});

		final FlowPane newVersionPane = new FlowPane(newLabel, urlLabel);
		newVersionPane.setPadding(new Insets(0, 0, 5, 10));
		GridPane.setColumnSpan(newVersionPane, 2);
		GridPane.setHalignment(newVersionPane, HPos.CENTER);
		gridPane.add(newVersionPane, 0, 2);
	}

	private Tab buildSettingsTab() {
		final SwitchButton toolButton = new SwitchButton();
		toolButton.setSwitchOn(Settings.getSettings().showToolbar().get());
		toolButton.switchOnProperty().addListener((ov, o, n) -> Settings.getSettings().showToolbar().set(n));

		final SwitchButton showExcButton = new SwitchButton();
		showExcButton.setSwitchOn(Settings.getSettings().showExcludeBox().get());
		showExcButton.switchOnProperty().addListener((ov, o, n) -> Settings.getSettings().showExcludeBox().set(n));

		final SwitchButton replyAllDblClickButton = new SwitchButton();
		replyAllDblClickButton.setSwitchOn(Settings.getSettings().replyAllDblClick().get());
		replyAllDblClickButton.switchOnProperty().addListener((ov, o, n) -> Settings.getSettings().replyAllDblClick().set(n));

		final SwitchButton archOnDropButton = new SwitchButton();
		archOnDropButton.setSwitchOn(Settings.getSettings().archiveOnDrop().get());
		archOnDropButton.switchOnProperty().addListener((ov, o, n) -> Settings.getSettings().archiveOnDrop().set(n));

		final TextField popupLifetimeField = new TextField("" + Settings.getSettings().popupLifetime().get());
		popupLifetimeField.setPrefColumnCount(3);
		popupLifetimeField.textProperty().addListener((ov, o, n) -> {
			try {
				final int delay = Integer.parseInt(n);
				Settings.getSettings().popupLifetime().set(delay);
			} catch (final Exception e) {
				((StringProperty)ov).setValue(o);
			}
		});

		final TextArea signatureHtml = new TextArea(Settings.getSettings().
				htmlSignature().get().replaceAll("<br>", "\n"));
		signatureHtml.setPrefRowCount(5);
		signatureHtml.setPrefColumnCount(20);
		signatureHtml.textProperty().addListener((ov, o, n) -> {
			Settings.getSettings().htmlSignature().set(n
					.replaceAll("<[Bb][Rr]>\\n", "<br>")
					.replaceAll("\\n", "<br>"));
		});

		hiddenSectionsPane = new FlowPane(3, 1);
		hiddenSectionsPane.setMaxWidth(300);
		hiddenSectionsPane.getStyleClass().add("label-list-pane");

		hiddenTagsPane = new FlowPane(3, 1);
		hiddenTagsPane.setMaxWidth(300);
		hiddenTagsPane.getStyleClass().add("label-list-pane");

		final Button refreshButton = new Button("boom!");
		refreshButton.setOnAction(e -> {
			mailService.clearCache();
			refreshHidden();
		});

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
		gridPane.add(new Label("show exclude box (restart needed)")				, 0, i);
		gridPane.add(showExcButton												, 1, i);
		i++;
		gridPane.add(new Label("thread list double click replies all")			, 0, i);
		gridPane.add(replyAllDblClickButton										, 1, i);
		i++;
		gridPane.add(new Label("archive on drop")								, 0, i);
		gridPane.add(archOnDropButton											, 1, i);
		i++;
		gridPane.add(new Label("hidden section")								, 0, i);
		gridPane.add(hiddenSectionsPane											, 1, i, 1, 2);
		i += 2;
		gridPane.add(new Label("hidden tag")									, 0, i);
		gridPane.add(hiddenTagsPane												, 1, i, 1, 2);
		i += 2;
		gridPane.add(new Label("reset cached data")								, 0, i);
		gridPane.add(refreshButton												, 1, i);

		refreshHidden();

		return new Tab("settings", new ScrollPane(gridPane));
	}

	private void refreshHidden() {
		try {
			hiddenSectionsPane.getChildren().clear();
			mailService.getHiddenSections().forEach(s -> {
				final RemoveLabel label = new RemoveLabel(s.getName(), "show");
				label.setOnMouseClicked(e -> {
					try {
						mailService.show(s);
						refreshHidden();
					} catch (final Exception ex) {
						LOGGER.error("showing {}", s.getName());
					}
				});
				hiddenSectionsPane.getChildren().add(label);
			});
		} catch (final TagException e) {
			LOGGER.error("loading hidden sections", e);
		}
		try {
			hiddenTagsPane.getChildren().clear();
			mailService.getHiddenTags().forEach(t -> {
				final RemoveLabel label = new RemoveLabel(t.getPath(), "show");
				label.setOnMouseClicked(e -> {
					try {
						mailService.show(t);
						refreshHidden();
					} catch (final Exception ex) {
						LOGGER.error("showing {}", t.getName());
					}
				});
				hiddenTagsPane.getChildren().add(label);
			});
		} catch (final TagException e) {
			LOGGER.error("loading hidden tags", e);
		}
	}
}
