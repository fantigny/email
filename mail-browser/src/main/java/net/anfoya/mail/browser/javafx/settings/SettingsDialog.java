package net.anfoya.mail.browser.javafx.settings;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

public class SettingsDialog extends Stage {
	private final MailService<? extends Section, ? extends Tag, ? extends Thread, ? extends Message, ? extends Contact> mailService;
	private final EventHandler<ActionEvent> logoutHandler;

	public SettingsDialog(final MailService<? extends Section, ? extends Tag, ? extends Thread, ? extends Message, ? extends Contact> mailService, final EventHandler<ActionEvent> logoutHandler) {
		initStyle(StageStyle.UNIFIED);
		initModality(Modality.APPLICATION_MODAL);
		setOnCloseRequest(e -> Settings.getSettings().save());

		this.mailService = mailService;
		this.logoutHandler = logoutHandler;

		final TextArea textArea = new TextArea("Take your mail bait and fish some action!");
		final Tab helpTab = new Tab("help", textArea);

		final TabPane tabPane = new TabPane(buildSettingsTab(), helpTab, buildAboutTab());
		tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

		setScene(new Scene(tabPane, 600, 400));
	}

	private Tab buildAboutTab() {

		final Text helpText = new Text("FisherMail [1.0] by Frederic Antigny");
		helpText.setFont(Font.font("Amble Cn", FontWeight.BOLD, 24));
		helpText.setFill(Color.WHITE);
		helpText.setStroke(Color.web("#7080A0"));

		final StackPane stack = new StackPane();
		stack.getChildren().addAll(helpText);
		stack.setAlignment(Pos.CENTER);     // Right-justify nodes in stack

		HBox.setHgrow(stack, Priority.ALWAYS);    // Give stack any extra space
		final HBox hbox = new HBox(stack);            // Add to HBox from Example 1-2

		return new Tab("about", hbox);
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

		final GridPane gridPane = new GridPane();
		gridPane.setPadding(new Insets(5));
		gridPane.setVgap(5);
		gridPane.setHgap(10);
		int i = 0;
		gridPane.addRow(i++, new Label("disconnect"), logoutButton);
		gridPane.addRow(i++, new Label("clear cache"), clearCacheButton);
		gridPane.addRow(i++, new Label("show tool bar"), toolButton);

		return new Tab("settings", gridPane);
	}
}
