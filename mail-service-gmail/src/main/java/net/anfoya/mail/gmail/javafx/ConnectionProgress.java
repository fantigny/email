package net.anfoya.mail.gmail.javafx;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
		getIcons().add(new Image(getClass().getResourceAsStream("/net/anfoya/mail/img/Mail.png")));
		setScene(new Scene(new BorderPane(), width, 110));
		sizeToScene();
		show();

		final ImageView image = new ImageView(new Image(getClass().getResourceAsStream("googlemail-64.png")));
		image.setFitHeight(64);
		image.setFitWidth(64);

		progressText = new Label();
		progressText.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);

		progressBar = new Label("");
		progressBar.setStyle("-fx-background-color: blue;");
		progressBar.setMinSize(0, 3);
		progressBar.setPrefSize(0, 3);

		final BorderPane borderPane = (BorderPane) getScene().getRoot();
		borderPane.setStyle("-fx-background-color: #EEEEEE");
		BorderPane.setAlignment(image, Pos.CENTER);
		BorderPane.setMargin(image, new Insets(0,0,0, 20));
		borderPane.setLeft(image);
		borderPane.setCenter(progressText);
		borderPane.setBottom(progressBar);
	}

	@Override
	public void hide() {
		final Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), new KeyValue(opacityProperty(), 0)));
		timeline.setOnFinished(e -> super.hide());
		timeline.playFromStart();
	}

	public ConnectionProgress setValue(final double progress, final String text) {
		if (progress < 0) {
			startKITTmode();
		} else {
			stopKITTmode();
			progressBar.setPrefWidth( getWidth() * progress );
		}
		progressText.setText(text);
		new DelayTimeline(Duration.millis(500), e -> progressText.setText(text)).play();

		return this;
	}

	private synchronized void stopKITTmode() {
		if (undeterminate == null) {
			return;
		}
		undeterminate.stop();
		undeterminate = null;
		progressBar.setTranslateX(0);
	}

	private synchronized void startKITTmode() {
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
