package net.anfoya.javafx.scene.dnd;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import net.anfoya.javafx.scene.animation.DelayTimeline;

public class DndPaneTranslationHelper {

	private final Pane dndPane;

	private DelayTimeline moveDelay;

	public DndPaneTranslationHelper(final Pane dndPane) {
		this.dndPane = dndPane;

		dndPane.setOnDragEntered(e -> delayedMove());
		dndPane.setOnDragExited(e -> stopDelayedMove());
	}

	private void delayedMove() {
		stopDelayedMove();
		moveDelay = new DelayTimeline(Duration.millis(1000), e -> move());
		moveDelay.play();
	}

	private void stopDelayedMove() {
		if (moveDelay != null) {
			moveDelay.stop();
		}
	}

	private void move() {
		Platform.runLater(() -> {
			final TranslateTransition translate = new TranslateTransition(Duration.millis(50), dndPane);
			translate.setInterpolator(Interpolator.EASE_BOTH);
			translate.setCycleCount(1);
			translate.setByY(((int)dndPane.getTranslateY() == 0? -1: 1) * dndPane.getHeight());
			translate.play();
		});
	}
}
