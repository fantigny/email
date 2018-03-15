package net.anfoya.javafx.scene.control;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

public class TitledProgressBar extends StackPane {
	private final Text title;
	private final ObjectProperty<State> stateProperty;
	private final ProgressBar bar;

	public TitledProgressBar(final String text) {
        getStylesheets().add(getClass().getResource("progress.css").toExternalForm());

		title = new Text(text);

        bar = new ProgressBar();
        bar.setEventDispatcher(title.getEventDispatcher());
		bar.prefWidthProperty().bind(widthProperty());

		stateProperty = new SimpleObjectProperty<State>();
		stateProperty.addListener(new ChangeListener<State>() {
			@Override
			public void changed(final ObservableValue<? extends State> ov, final State oldVal, final State newVal) {
				switch(newVal) {
				case RUNNING:
				case SCHEDULED:
					bar.setVisible(true);
					break;
				default:
					bar.setVisible(false);
				}
			}
		});

		getChildren().setAll(title, bar);
	}

	public DoubleProperty progressProperty() {
		return bar.progressProperty();
	}

	public ObjectProperty<State> stateProperty() {
		return stateProperty;
	}
}

