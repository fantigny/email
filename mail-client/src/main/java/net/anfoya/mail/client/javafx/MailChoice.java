package net.anfoya.mail.client.javafx;


import java.util.Arrays;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.anfoya.mail.client.App;
import net.anfoya.mail.gmail.GmailServiceInfo;
import net.anfoya.mail.outlook.OutlookServiceInfo;
import net.anfoya.mail.service.MailServiceInfo;
import net.anfoya.mail.yahoo.YahooServiceInfo;

public class MailChoice {
	private static final String TITLE = "New connection";
	private static final String TEXT = "Welcome to FisherMail, please select the email provider you want to use";
	private static final double ICON_SIZE = 56;

	private static final MailServiceInfo[] SERVICES = new MailServiceInfo[] {
			new GmailServiceInfo(),
			new YahooServiceInfo(),
			new OutlookServiceInfo()
	};

	private final Stage stage;

	private MailServiceInfo info;

	public MailChoice() {
		stage = new Stage(StageStyle.DECORATED);
	}

	public MailServiceInfo getMailServiceInfo() {
		Label header = new Label(TEXT);
		header.setPadding(new Insets(20, 20, 30, 20));
		header.setStyle("-fx-font-size:14px;");
		header.setWrapText(true);

		final ListView<MailServiceInfo> services = new ListView<>();
		services.setCellFactory(l -> buildCell());
		services.setFocusTraversable(false);
		Arrays.stream(SERVICES).forEach(s -> services.getItems().add(s));

		final MultipleSelectionModel<MailServiceInfo> selectionModel = services.getSelectionModel();
		selectionModel.setSelectionMode(SelectionMode.SINGLE);

		Button select = new Button("select");
		select.setOnAction(e -> {
			info = selectionModel.getSelectedItem();
			stage.close();
		});
		select.disableProperty().bind(services.getSelectionModel().selectedItemProperty().isNull());

		Button cancel = new Button("cancel");
		cancel.setOnAction(e -> stage.close());

		HBox footer = new HBox(10, cancel, select);
		footer.setPadding(new Insets(20));
		footer.setAlignment(Pos.CENTER_RIGHT);

		BorderPane pane = new BorderPane(services, header, null, footer, null);

		stage.setScene(new Scene(pane, 300, 400));
		stage.getIcons().add(App.getIcon());
		stage.setResizable(false);
		stage.setTitle(TITLE);
		stage.showAndWait();

		return info;
	}

	private ListCell<MailServiceInfo> buildCell() {
		return new ListCell<MailServiceInfo>() {
			@Override public void updateItem(MailServiceInfo info, boolean empty) {
				super.updateItem(info, empty);
				if (empty) {
					setText(null);
					setGraphic(null);
					return;
				}

				setOnMouseClicked(e -> {
					if (e.getClickCount() > 1) {
						MailChoice.this.info = info;
						stage.close();
					}
				});

				setStyle("-fx-font-size:20px; -fx-padding:14px 10px 14px 50px;");
				setText("   " + info.getName());

				ImageView icon = new ImageView(info.getIcon());
				icon.setPreserveRatio(true);
				icon.setFitWidth(ICON_SIZE);
				icon.setFitHeight(ICON_SIZE);
				icon.setPreserveRatio(true);
				icon.setSmooth(true);
				icon.setCache(true);
				setGraphic(icon);
			}
		};
	}
}
