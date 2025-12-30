package com.emsi.baclegend.controller;

import com.emsi.baclegend.App;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import java.io.IOException;

public class SettingsController {

    @FXML
    private Slider sliderTime;

    @FXML
    private Label lblTimeValue;

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
        App.setRoot("view/main");
    }

    @FXML
    private void handleRetour() throws IOException {
        App.setRoot("view/main");
    }
}
