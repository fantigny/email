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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
import net.anfoya.mail.browser.javafx.util.UrlHelper;

public class SettingsDialog extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(SettingsDialog.class);

	private static final String CSS_DATA = "<style> body {"
			+ " margin: 7;"
			+ " padding: 0;"
			+ " font-family: Arial, Helvetica, sans-serif;"
			+ " font-size: 12px;"
			+ "} </style>";

	private final TabPane tabPane;

	public SettingsDialog() {
		initStyle(StageStyle.UNIFIED);
		setOnCloseRequest(e -> Settings.getSettings().save());

		tabPane = new TabPane(buildSettingsTab(), buildAboutTab(), buildHelpTab(), buildTaskTab());
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

		setScene(new Scene(tabPane, 600, 400));
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
		help.getEngine().loadContent("<html>"
				+ CSS_DATA
				+ "<h4>in theory</h4>"
				+ "<ul>	<li>tags are grouped by section</li>"
				+ "		<li>messages are grouped by thread</li>"
				+ "		<li>threads are meant to be sorted</li>"
				+ "</ul>"
				+ "<h4>drag to drop</h4>"
				+ "<ul>	<li><b>drag down</b> your thread to reply, forward, archive</li>"
				+ "		<li>drag a thread on the left to <b>sort</b></li>"
				+ "		<li>drag a <b>tag</b> to customize</li>"
				+ "		<li>...</li>"
				+ "</ul>"
				+ "<h4>press tab</h4>"
				+ "<ul>	<li>once to search mails</li>"
				+ "		<li>again to search tags</li>"
				+ "</ul>"
				+ "<h4>blue</h4>"
				+ "<ul>	<li>is a color for <b>system</b> tags</li>"
				+ "		<li>can't move them</li>"
				+ "		<li>can't rename them</li>"
				+ "		<li>can't delete them</li>"
				+ "		<li>pfff...</li>"
				+ "</ul>"
				+ "<h4>unread me</h4>"
				+ "<ul>	<li>by dragging the thread to <b>Unread</b> tag</li>"
				+ "</ul>"
				+ "<h4>be advanced</h4>"
				+ "<ul>	<li><b>check</b> a tag to keep it in the selection</li>"
				+ "		<li>check another tag to reduce the thread set</li>"
				+ "</ul>"
				+ "<h4>double click</h4>"
				+ "<ul>	<li>on the threadlist to <b>reply</b> a thread</li>"
				+ "		<li><b>Reply all</b> to be activated in settings</li>"
				+ "</ul>"
				+ "<h4>be extreme</h4>"
				+ "<ul>	<li>show exclude box in settings and restart</li>"
				+ "		<li>include and exclude tags as you wish</li>"
				+ "		<li>deactivate archive on drop from settings</li>"
				+ "		<li>hide toolbar if you dare</li>"
				+ "		<li>cruise extreme mode</li>"
				+ "</ul>"
				+ "</html>");

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

		final GridPane gridPane = new GridPane();
		gridPane.setPadding(new Insets(5));
		gridPane.setVgap(5);
		gridPane.setHgap(10);

		int i = 0;
		gridPane.addRow(i++, new Label("signature (html is welcome)"), signatureHtml);
		gridPane.addRow(i++, new Label("popup lifetime in seconds (0 for permanent)"), popupLifetimeField);
		gridPane.addRow(i++, new Label("show tool bar"), toolButton);
		gridPane.addRow(i++, new Label("show exclude box (restart needed)"), showExcButton);
		gridPane.addRow(i++, new Label("thread list double click replies all"), replyAllDblClickButton);
		gridPane.addRow(i++, new Label("archive on drop"), archOnDropButton);

		return new Tab("settings", gridPane);
	}
}
