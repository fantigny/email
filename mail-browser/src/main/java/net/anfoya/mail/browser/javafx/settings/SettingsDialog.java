package net.anfoya.mail.browser.javafx.settings;

import java.util.Map;
import java.util.concurrent.Future;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

public class SettingsDialog extends Stage {
	private final MailService<? extends Section, ? extends Tag, ? extends Thread, ? extends Message, ? extends Contact> mailService;
	private final EventHandler<ActionEvent> logoutHandler;

	private final ListView<String> taskList;
	private final TabPane tabPane;

	public SettingsDialog(final MailService<? extends Section, ? extends Tag, ? extends Thread, ? extends Message, ? extends Contact> mailService, final EventHandler<ActionEvent> logoutHandler) {
		initStyle(StageStyle.UNIFIED);
		setOnCloseRequest(e -> Settings.getSettings().save());

		this.mailService = mailService;
		this.logoutHandler = logoutHandler;

		final TextArea textArea = new TextArea("Take your mail bait and fish some action!");
		final Tab helpTab = new Tab("help", textArea);

		taskList = new ListView<String>();
		final Tab taskTab = new Tab("Tasks", taskList);
		ThreadPool.getInstance().setOnChange(map -> {
			Platform.runLater(() -> refreshTasks(map));
			return null;
		});

		tabPane = new TabPane(buildSettingsTab(), helpTab, buildAboutTab(), taskTab);
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

		setScene(new Scene(tabPane, 600, 400));
	}

	public void showAbout() {
		tabPane.getSelectionModel().select(2);
		show();
	}

	private void refreshTasks(final Map<? extends Future<?>, ? extends String> futureDesc) {
		final ObservableList<String> items = taskList.getItems();
		items.clear();
		for(final String desc: futureDesc.values()) {
			items.add(desc);
		}
		if (items.isEmpty()) {
			items.add("idle");
		}
	}

	private Tab buildAboutTab() {
		final ImageView image = new ImageView(new Image(getClass().getResourceAsStream("/net/anfoya/mail/image/Mail.png")));

		final Text text = new Text("FisherMail 1.0\u03B1\rby Frederic Antigny");
		text.setFont(Font.font("Amble Cn", FontWeight.BOLD, 24));
		text.setFill(Color.web("#bbbbbb"));

		final GridPane gridPane = new GridPane();
		gridPane.setStyle("-fx-background-color: #4d4d4d;");

		gridPane.addColumn(0, image);
		GridPane.setMargin(image, new Insets(20));
		GridPane.setVgrow(image, Priority.ALWAYS);
		GridPane.setValignment(image, VPos.CENTER);

		gridPane.addColumn(1, text);
		GridPane.setVgrow(text, Priority.ALWAYS);
		GridPane.setValignment(text, VPos.CENTER);

		return new Tab("about", gridPane);
	}

	private Tab buildSettingsTab() {
		final Button logoutButton = new Button("logout");
		logoutButton.setOnAction(e -> {
			close();
			logoutHandler.handle(null);
		});

		final Button clearCacheButton = new Button("clear cache");
		clearCacheButton.setOnAction(e -> mailService.clearCache());

		final SwitchButton toolButton = new SwitchButton();
		toolButton.setSwitchOn(Settings.getSettings().showToolbar().get());
		toolButton.switchOnProperty().addListener((ov, o, n) -> Settings.getSettings().showToolbar().set(n));

		final SwitchButton showExcButton = new SwitchButton();
		showExcButton.setSwitchOn(Settings.getSettings().showExcludeBox().get());
		showExcButton.switchOnProperty().addListener((ov, o, n) -> Settings.getSettings().showExcludeBox().set(n));

		final SwitchButton archOnDropButton = new SwitchButton();
		archOnDropButton.setSwitchOn(Settings.getSettings().showExcludeBox().get());
		archOnDropButton.switchOnProperty().addListener((ov, o, n) -> Settings.getSettings().archiveOnDrop().set(n));


		final GridPane gridPane = new GridPane();
		gridPane.setPadding(new Insets(5));
		gridPane.setVgap(5);
		gridPane.setHgap(10);
		int i = 0;
		gridPane.addRow(i++, new Label("logout from this account"), logoutButton);
		gridPane.addRow(i++, new Label("clear cache"), clearCacheButton);
		gridPane.addRow(i++, new Label("show tool bar"), toolButton);
		gridPane.addRow(i++, new Label("show exclude box (restart needed)"), showExcButton);
		gridPane.addRow(i++, new Label("archive on drop"), archOnDropButton);

		return new Tab("settings", gridPane);
	}
}
