package net.anfoya.javafx.scene.animation;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

public class DelayTimeline {
	private final Timeline delegate;

	public DelayTimeline(final Duration duration, final EventHandler<ActionEvent> onFinished) {
		delegate = new Timeline(new KeyFrame(duration, onFinished));
		delegate.setCycleCount(1);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	public final ObservableList<KeyFrame> getKeyFrames() {
		return delegate.getKeyFrames();
	}

	@Override
	public boolean equals(final Object obj) {
		return delegate.equals(obj);
	}

	public void stop() {
		delegate.stop();
	}

	public final void setRate(final double value) {
		delegate.setRate(value);
	}

	public final double getRate() {
		return delegate.getRate();
	}

	public final DoubleProperty rateProperty() {
		return delegate.rateProperty();
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	public final double getCurrentRate() {
		return delegate.getCurrentRate();
	}

	public final ReadOnlyDoubleProperty currentRateProperty() {
		return delegate.currentRateProperty();
	}

	public final Duration getCycleDuration() {
		return delegate.getCycleDuration();
	}

	public final ReadOnlyObjectProperty<Duration> cycleDurationProperty() {
		return delegate.cycleDurationProperty();
	}

	public final Duration getTotalDuration() {
		return delegate.getTotalDuration();
	}

	public final ReadOnlyObjectProperty<Duration> totalDurationProperty() {
		return delegate.totalDurationProperty();
	}

	public final Duration getCurrentTime() {
		return delegate.getCurrentTime();
	}

	public final ReadOnlyObjectProperty<Duration> currentTimeProperty() {
		return delegate.currentTimeProperty();
	}

	public final void setDelay(final Duration value) {
		delegate.setDelay(value);
	}

	public final Duration getDelay() {
		return delegate.getDelay();
	}

	public final ObjectProperty<Duration> delayProperty() {
		return delegate.delayProperty();
	}

	public final int getCycleCount() {
		return delegate.getCycleCount();
	}

	public final IntegerProperty cycleCountProperty() {
		return delegate.cycleCountProperty();
	}

	public final void setAutoReverse(final boolean value) {
		delegate.setAutoReverse(value);
	}

	public final boolean isAutoReverse() {
		return delegate.isAutoReverse();
	}

	public final BooleanProperty autoReverseProperty() {
		return delegate.autoReverseProperty();
	}

	public final Status getStatus() {
		return delegate.getStatus();
	}

	public final ReadOnlyObjectProperty<Status> statusProperty() {
		return delegate.statusProperty();
	}

	public final double getTargetFramerate() {
		return delegate.getTargetFramerate();
	}

	public final void setOnFinished(final EventHandler<ActionEvent> value) {
		delegate.setOnFinished(value);
	}

	public final EventHandler<ActionEvent> getOnFinished() {
		return delegate.getOnFinished();
	}

	public final ObjectProperty<EventHandler<ActionEvent>> onFinishedProperty() {
		return delegate.onFinishedProperty();
	}

	public final ObservableMap<String, Duration> getCuePoints() {
		return delegate.getCuePoints();
	}

	public void jumpTo(final Duration time) {
		delegate.jumpTo(time);
	}

	public void jumpTo(final String cuePoint) {
		delegate.jumpTo(cuePoint);
	}

	public void playFrom(final String cuePoint) {
		delegate.playFrom(cuePoint);
	}

	public void playFrom(final Duration time) {
		delegate.playFrom(time);
	}

	public void play() {
		delegate.play();
	}

	public void playFromStart() {
		delegate.playFromStart();
	}

	public void pause() {
		delegate.pause();
	}

}
