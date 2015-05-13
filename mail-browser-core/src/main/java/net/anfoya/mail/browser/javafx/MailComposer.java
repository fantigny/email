package net.anfoya.mail.browser.javafx;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class MailComposer<M extends SimpleMessage> {

	private final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService;
	private final M draft;

	public MailComposer(final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService, final M draft) {
		this.mailService = mailService;
		this.draft = draft;

        final BorderPane mainPane = new BorderPane(new Label("new mail"));

		final Stage stage = new Stage();
		stage.setTitle("FisherMail / Agaar / Agamar / Agaram");
//		stage.getIcons().add(new Image(getClass().getResourceAsStream("Mail.png")));
		stage.setScene(new Scene(mainPane, 800, 600));
        stage.show();
	}

	public MailComposer(final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService) throws MailException {
		this(mailService, mailService.createDraft());
	}
}
