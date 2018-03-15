package net.anfoya.javafx.scene.dnd;

import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import net.anfoya.javafx.scene.animation.DelayTimeline;

public class DndPaneTranslationHelper {
	private static final Duration DELAY = Duration.seconds(2);

	private final Pane dndPane;

	private DelayTimeline moveDelay;

	public DndPaneTranslationHelper(final Pane dndPane) {
		this.dndPane = dndPane;

		dndPane.setOnDragOver(e -> startDelayedMove());
		dndPane.setOnDragExited(e -> stopDelayedMove());
	}

	public void reset() {
		stopDelayedMove();
	}

//	private void resetPosition() {
//		stopDelayedMove();
//		if (dndPane.getTranslateY() != 0) {
//			move();
//		}
//	}

	private void startDelayedMove() {
		stopDelayedMove();
		moveDelay = new DelayTimeline(DELAY, e -> move());
		moveDelay.play();
	}

	private void stopDelayedMove() {
		if (moveDelay != null) {
			moveDelay.stop();
		}
	}

	private void move() {
		final TranslateTransition translate = new TranslateTransition(Duration.millis(50), dndPane);
		translate.setCycleCount(1);
		translate.setByY(((int)dndPane.getTranslateY() == 0? -1: 1) * dndPane.getHeight());
		translate.play();
	}
}
