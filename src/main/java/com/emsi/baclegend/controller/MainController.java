package com.emsi.baclegend.controller;

import com.emsi.baclegend.App;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import java.io.IOException;

public class MainController {

    @FXML
    private TextField txtPseudo;

    @FXML
    public void initialize() {
        if (txtPseudo != null) {
            txtPseudo.setText(App.currentUser);
        }
    }

    private boolean validerPseudo() {
        String p = txtPseudo.getText();
        if (p == null || p.trim().isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Pseudo requis");
            a.setContentText("Veuillez entrer votre pseudo avant de jouer.");
            a.show();
            return false;
        }
        App.currentUser = p.trim();
        return true;
    }

    @FXML
    private void handleSolo() throws IOException {
        if (!validerPseudo())
            return;
        System.out.println("Lancement du Mode Solo...");
        App.setRoot("view/game");
    }

    @FXML
    private void handleMulti() throws IOException {
        if (!validerPseudo())
            return;
        App.setRoot("view/lobby");
    }

    @FXML
    private void handleCategories() throws IOException {
        App.setRoot("view/categories");
    }

    @FXML
    private void handleSettings() throws IOException {
        App.setRoot("view/settings");
    }

    @FXML
    private void handleQuit() {
        Platform.exit();
        System.exit(0);
    }
}
