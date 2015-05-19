package net.anfoya.javafx.scene.control;

import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.scene.Group;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;

/**
 *
 * @author Lawrence PremKumar
 */
public class ResetTextField extends Group {

    TextField tc = null;
    Group closeButton = null;

    public ResetTextField() {
        tc = new TextField();
        tc.setStyle(".text-field,.text-field-focused");
        tc.setOnKeyTyped(new EventHandler<KeyEvent>() {
            @Override
            public void handle(final KeyEvent event) {

                System.out.println(tc.getText().length());
                if ((tc.textProperty().get().length() < 0) || (tc.textProperty().get().equals(""))) {                     closeButton.setVisible(false);                 } else if (tc.textProperty().get().length() > -1) {
                    closeButton.setVisible(true);
                }
            }
        });

        closeButton = getCloseButton();
        closeButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(final MouseEvent t) {
                closeButton.setVisible(false);
                tc.clear();
            }
        });

        final GridPane grid = new GridPane();
        grid.setLayoutX(1.0);
        grid.setLayoutY(1.0);
        grid.setHgap(0.0);
        final ColumnConstraints txtComponent = new ColumnConstraints();
        txtComponent.setHalignment(HPos.RIGHT);
        grid.getColumnConstraints().add(txtComponent);
        final ColumnConstraints closeBtn = new ColumnConstraints();
        closeBtn.setHalignment(HPos.LEFT);
        grid.getColumnConstraints().add(closeBtn);
        grid.add(tc, 0, 0);
        grid.add(closeButton, 1, 0);

        final Rectangle focusBorder = new Rectangle();
        focusBorder.setFill(null);
        focusBorder.setStrokeWidth(2.0);
        focusBorder.setStroke(Color.web("#097dda"));
        focusBorder.widthProperty().bind(grid.widthProperty().add(2));
        focusBorder.heightProperty().bind(grid.heightProperty().add(2));
        focusBorder.visibleProperty().bind(tc.focusedProperty());

        this.getChildren().addAll(focusBorder, grid);
    }

    private Group getCloseButton() {
        final Group grp = new Group();
        final Circle circle = new Circle();
        circle.setRadius(7.0);
        final Rectangle r1 = getCard();
        r1.setRotate(-45);
        final Rectangle r2 = getCard();
        r2.setRotate(45);
        grp.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(final MouseEvent t) {
                circle.setFill(Color.web("#097dda"));
                r1.setFill(Color.WHITE);
                r2.setFill(Color.WHITE);
            }
        });
        grp.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(final MouseEvent t) {
                circle.setFill(Color.web("#4c4c4c"));
                r1.setFill(Color.web("#868686"));
                r2.setFill(Color.web("#868686"));
            }
        });
        grp.setVisible(false);
        circle.setFill(Color.web("#4c4c4c"));
        grp.getChildren().addAll(circle, r1, r2);
        return grp;
    }

    private Rectangle getCard() {
        final Rectangle card = RectangleBuilder.create()
                .x(-4)
                .y(-1)
                .width(8)
                .height(2)
                .strokeWidth(1.0)
                .fill(Color.web("#868686"))
                .build();
        return card;
    }
}
