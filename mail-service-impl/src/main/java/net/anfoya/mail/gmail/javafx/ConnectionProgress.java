package net.anfoya.mail.gmail.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ConnectionProgress extends Stage {

	private final Labeled progressText;
	private final ProgressBar progressBar;

	public ConnectionProgress() {
		super(StageStyle.UNDECORATED);

		final ImageView image = new ImageView(new Image(getClass().getResourceAsStream("googlemail-64.png")));
		BorderPane.setAlignment(image, Pos.CENTER);
		BorderPane.setMargin(image, new Insets(5));

		progressText = new Label();
		progressText.setAlignment(Pos.CENTER);
		progressText.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);

		progressBar = new ProgressBar();
		progressBar.prefWidthProperty().bind(widthProperty());

		final BorderPane borderPane = new BorderPane(progressText, null, null, progressBar, image);
		borderPane.setPadding(new Insets(5));

		setScene(new Scene(borderPane, 300, 110));
		sizeToScene();
	}

	public ConnectionProgress setValue(final double progress, final String text) {
		if (!isShowing()) {
			show();
		}
		progressBar.setProgress(progress);
		progressText.setText(text);

		return this;
	}
}
