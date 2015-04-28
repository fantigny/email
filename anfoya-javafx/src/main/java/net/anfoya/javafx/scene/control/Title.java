package net.anfoya.javafx.scene.control;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class Title extends HBox {

	private final Text main;
	private final Text sub;

	public Title(final String main, final String sub) {
		setAlignment(Pos.CENTER_LEFT);

		this.main = new Text(main);
		this.main.setFont(Font.font(null, this.main.getFont().getSize() + 2));

		this.sub = new Text(sub);
		this.sub.setTextAlignment(TextAlignment.RIGHT);
		this.sub.setFont(Font.font("Arial", FontWeight.NORMAL, this.sub.getFont().getSize()));

		getChildren().setAll(this.main, this.sub);
	}

	public Title(final String text) {
		this(text, "");
	}

	public void setMain(final String main) {
		this.main.setText(main);
	}

	public void setSub(final String sub) {
		this.sub.setText(sub);
	}

	public void set(final String main, final String sub) {
		setMain(main);
		setSub(sub);
	}
}
