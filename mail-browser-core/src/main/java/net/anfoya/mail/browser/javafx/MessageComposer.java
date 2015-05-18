package net.anfoya.mail.browser.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class MessageComposer<M extends SimpleMessage> extends Stage {

	private final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService;
	private final M draft;
	private final HTMLEditor editor;
	private ComboBox<String> fromCombo;
	private TextField toField;
	private TextField ccField;
	private TextField bccField;
	private TextField subjectField;
	private GridPane headerPane;
	private HBox toBox;
	private BorderPane mainPane;
	private Label toLabel;

	public MessageComposer(
			final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService)
			throws MailException {
		this(mailService, mailService.createDraft());
	}

	public MessageComposer(
			final MailService<? extends SimpleSection, ? extends SimpleTag, ? extends SimpleThread, M> mailService,
			final M draft) {
		super(StageStyle.UNIFIED);
		setTitle("FisherMail / Agaar / Agamar / Agaram");
		getIcons().add(new Image(getClass().getResourceAsStream("entrypoint/Mail.png")));
		setScene(new Scene(new BorderPane(), 800, 600));

		this.mainPane = (BorderPane) getScene().getRoot();
		this.mailService = mailService;
		this.draft = draft;

		fromCombo = new ComboBox<String>();
		fromCombo.prefWidthProperty().bind(widthProperty());
		fromCombo.getItems().add("me");
		fromCombo.getSelectionModel().select(0);
		fromCombo.setDisable(true);

		toLabel = new Label("to");
		final Label moreLabel = new Label(" ...");
		toBox = new HBox(toLabel, moreLabel);
		toBox.setAlignment(Pos.CENTER_LEFT);
		toBox.setOnMouseClicked(event -> {
			if (headerPane.getChildren().contains(fromCombo)) {
				toMiniHeader();
			} else {
				toFullHeader();
			}
		});
		toField = new TextField();

		ccField = new TextField();

		bccField = new TextField();

		subjectField = new TextField();

		final ColumnConstraints widthConstraints = new ColumnConstraints(80);
		final ColumnConstraints growConstraints = new ColumnConstraints();
		growConstraints.setHgrow(Priority.ALWAYS);

		headerPane = new GridPane();
		headerPane.setVgap(5);
		headerPane.setHgap(5);
		headerPane.setPadding(new Insets(5));
		headerPane.prefWidthProperty().bind(widthProperty());
		headerPane.getColumnConstraints().add(widthConstraints);
		headerPane.getColumnConstraints().add(growConstraints);
		mainPane.setTop(headerPane);
		toMiniHeader();

		editor = new HTMLEditor();
		mainPane.setCenter(editor);

		final Button discardButton = new Button("discard");
		discardButton.setOnAction(event -> {
			try {
				mailService.remove(draft);
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			close();
		});

		final HBox buttonBox = new HBox(5, discardButton, new Button("save"), new Button("send"));
		buttonBox.setAlignment(Pos.CENTER_RIGHT);
		buttonBox.setPadding(new Insets(5));
		mainPane.setBottom(buttonBox);

		show();
	}

	private void toMiniHeader() {
		headerPane.getChildren().clear();
		headerPane.addRow(0, toBox, toField);
		headerPane.addRow(1, new Label("subject"), subjectField);
	}

	private void toFullHeader() {
		headerPane.getChildren().clear();
//		headerPane.addRow(0, new Label("from"), fromCombo);
		headerPane.addRow(0, toLabel, toField);
		headerPane.addRow(1, new Label("cc"), ccField);
		headerPane.addRow(2, new Label("bcc"), bccField);
		headerPane.addRow(3, new Label("subject"), subjectField);
	}
}
