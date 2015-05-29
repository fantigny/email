package net.anfoya.mail.browser.javafx.settings;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import net.anfoya.mail.model.SimpleContact;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class Settings extends Stage {

	public Settings(final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, ? extends SimpleMessage, ? extends SimpleContact> mailService) {

		final Button logoutButton = new Button("logout and quit");
		logoutButton.setOnAction(event -> {
			mailService.logout();
			System.exit(0);
		});

		final GridPane gridPane = new GridPane();
		gridPane.setPadding(new Insets(5));
		gridPane.addRow(0, logoutButton);
		setScene(new Scene(gridPane, 600, 800));
	}
}
