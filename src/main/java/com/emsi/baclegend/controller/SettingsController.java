package com.emsi.baclegend.controller;

import com.emsi.baclegend.App;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import java.io.IOException;

public class SettingsController {

    @FXML
    private Slider sliderTime;

    @FXML
    private Label lblTimeValue;

    @FXML
    private ComboBox<String> comboLanguage;

    @FXML
    public void initialize() {
        // Load current value
        sliderTime.setValue(App.gameTimeDuration);
        updateLabel(App.gameTimeDuration);

        // Listen to changes
        sliderTime.valueProperty().addListener((obs, oldVal, newVal) -> {
            int seconds = newVal.intValue();
            updateLabel(seconds);
        });

        // Initialize language selection
        comboLanguage.setItems(FXCollections.observableArrayList("Français", "English"));
        if (App.gameLanguage.equals("FR")) {
            comboLanguage.setValue("Français");
        } else {
            comboLanguage.setValue("English");
        }
    }

    private void updateLabel(int seconds) {
        if (seconds >= 60) {
            int mins = seconds / 60;
            int secs = seconds % 60;
            if (secs == 0) {
                lblTimeValue.setText(mins + " minute" + (mins > 1 ? "s" : ""));
            } else {
                lblTimeValue.setText(mins + "m " + secs + "s");
            }
        } else {
            lblTimeValue.setText(seconds + " secondes");
        }
    }

    @FXML
    private void handleSave() throws IOException {
        App.gameTimeDuration = (int) sliderTime.getValue();
        
        // Save language preference
        String selectedLang = comboLanguage.getValue();
        if (selectedLang != null) {
            if (selectedLang.equals("Français")) {
                App.gameLanguage = "FR";
            } else {
                App.gameLanguage = "EN";
            }
        }
        
        App.setRoot("view/main");
    }

    @FXML
    private void handleRetour() throws IOException {
        App.setRoot("view/main");
    }
}
