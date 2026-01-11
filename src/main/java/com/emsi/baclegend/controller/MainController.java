package com.emsi.baclegend.controller;

import com.emsi.baclegend.App;
import com.emsi.baclegend.util.TranslationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import java.io.IOException;

public class MainController {

    @FXML
    private TextField txtPseudo;
    @FXML
    private Label lblTitle;
    @FXML
    private Label lblSubtitle;
    @FXML
    private Label lblPseudo;
    @FXML
    private javafx.scene.control.Button btnSolo;
    @FXML
    private javafx.scene.control.Button btnMulti;
    @FXML
    private javafx.scene.control.Button btnCategories;
    @FXML
    private javafx.scene.control.Button btnSettings;
    @FXML
    private javafx.scene.control.Button btnQuit;

    @FXML
    public void initialize() {
        if (txtPseudo != null) {
            txtPseudo.setText(App.currentUser);
        }
        updateTranslations();
    }
    
    private void updateTranslations() {
        if (lblTitle != null)
            lblTitle.setText(TranslationUtil.translate("game.title"));
        if (lblSubtitle != null)
            lblSubtitle.setText(TranslationUtil.translate("game.subtitle"));
        if (lblPseudo != null)
            lblPseudo.setText(TranslationUtil.translate("game.pseudo"));
        if (txtPseudo != null)
            txtPseudo.setPromptText(TranslationUtil.translate("game.pseudo.placeholder"));
        if (btnSolo != null)
            btnSolo.setText("🎮 " + TranslationUtil.translate("game.solo"));
        if (btnMulti != null)
            btnMulti.setText("👥 " + TranslationUtil.translate("game.multiplayer"));
        if (btnCategories != null)
            btnCategories.setText("📋 " + TranslationUtil.translate("game.categories"));
        if (btnSettings != null)
            btnSettings.setText("⚙️ " + TranslationUtil.translate("game.settings"));
        if (btnQuit != null)
            btnQuit.setText("❌ " + TranslationUtil.translate("game.quit"));
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
