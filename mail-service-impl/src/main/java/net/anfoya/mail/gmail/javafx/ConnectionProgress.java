package net.anfoya.mail.gmail.javafx;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ConnectionProgress extends Stage {

	private final Labeled progressText;
	private final ProgressBar progressBar;

	public ConnectionProgress() {
		super(StageStyle.UNDECORATED);

		progressText = new Label();
		progressText.setAlignment(Pos.CENTER);
		progressText.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);

		progressBar = new ProgressBar();
		progressBar.prefWidthProperty().bind(widthProperty());

		final BorderPane borderPane = new BorderPane(progressText, null, null, progressBar, null);

		setScene(new Scene(borderPane, 200, 50));
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
