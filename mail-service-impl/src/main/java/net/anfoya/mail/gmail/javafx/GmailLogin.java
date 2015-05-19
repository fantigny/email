package net.anfoya.mail.gmail.javafx;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class GmailLogin extends Stage {
	private String code;

    public GmailLogin(final Window owner, final String url) {
        super();
        initOwner(owner);
        setTitle("title");
        initModality(Modality.APPLICATION_MODAL);
        final Group root = new Group();
        final Scene scene = new Scene(root, 250, 150, Color.WHITE);
        setScene(scene);

        final GridPane gridpane = new GridPane();
        gridpane.setPadding(new Insets(5));
        gridpane.setHgap(5);
        gridpane.setVgap(5);

        final Label userNameLbl = new Label("User Name: ");
        gridpane.add(userNameLbl, 0, 1);

        final Label passwordLbl = new Label("Password: ");
        gridpane.add(passwordLbl, 0, 2);
        final TextField userNameFld = new TextField("Admin");
        gridpane.add(userNameFld, 1, 1);

        final PasswordField passwordFld = new PasswordField();
        passwordFld.setText("password");
        gridpane.add(passwordFld, 1, 2);

        final Button login = new Button("Change");
        login.setOnAction(event -> close());
        gridpane.add(login, 1, 3);
        GridPane.setHalignment(login, HPos.RIGHT);
        root.getChildren().add(gridpane);
    }

	public String getCode() {
		return code;
	}
}