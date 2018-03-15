package net.anfoya.mail.gmail.javafx;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import net.anfoya.javafx.scene.animation.DelayTimeline;

public class ConnectionProgress extends Stage {
	private static final int DEFAULT_WIDTH = 300;
	private final Labeled progressText;
	private final Label progressBar;

	private TranslateTransition undeterminate;

	public ConnectionProgress() {
		this(DEFAULT_WIDTH);
	}

	public ConnectionProgress(final int width) {
		super(StageStyle.UNDECORATED);

		final ImageView image = new ImageView(new Image(getClass().getResourceAsStream("googlemail-64.png")));
		image.setFitHeight(64);
		image.setFitWidth(64);
		BorderPane.setAlignment(image, Pos.CENTER);
		BorderPane.setMargin(image, new Insets(0,0,0, 20));

		progressText = new Label();
		progressText.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);

		progressBar = new Label("");
		progressBar.setStyle("-fx-background-color: blue;");
		progressBar.setMinSize(0, 3);
		progressBar.setPrefSize(0, 3);

		final BorderPane borderPane = new BorderPane(progressText, null, null, progressBar, image);
		borderPane.setStyle("-fx-background-color: white");

		setScene(new Scene(borderPane, width, 110));
		sizeToScene();
	}
	
	@Override
	public void hide() {
		new DelayTimeline(Duration.millis(500), e -> super.hide()).play();
	}

	public ConnectionProgress setValue(final double progress, final String text) {
		if (!isShowing()) {
			show();
		}

		if (progress < 0) {
			startUndeterminate();
		} else {
			stopUndeterminate();
			progressBar.setPrefWidth( getWidth() * progress );
		}
		progressText.setText(text);

		return this;
	}

	private synchronized void stopUndeterminate() {
		if (undeterminate == null) {
			return;
		}
		undeterminate.stop();
		undeterminate = null;
		progressBar.setTranslateX(0);
	}

	private synchronized void startUndeterminate() {
		if (undeterminate != null) {
			return;
		}
		progressBar.setPrefWidth(getWidth() / 6d);
		undeterminate = new TranslateTransition(Duration.seconds(2), progressBar);
		undeterminate.setInterpolator(Interpolator.EASE_BOTH);
		undeterminate.setCycleCount(Animation.INDEFINITE);
		undeterminate.setAutoReverse(true);
		undeterminate.setByX(getWidth() - getWidth() / 6d);
		undeterminate.play();
	}
}
