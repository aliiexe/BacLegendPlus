package com.emsi.baclegend.controller;

import com.emsi.baclegend.App;
import com.emsi.baclegend.service.ServiceReseau;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyController {

    @FXML
    private Label lblStatusHost;
    @FXML
    private Label lblRoomCode;

    @FXML
    private TextField txtCode;
    @FXML
    private Label lblStatusJoin;

    @FXML
    private VBox vboxPlayers;
    @FXML
    private Button btnStartGame;

    private boolean isHost = false;
    private Set<String> players = ConcurrentHashMap.newKeySet();

    @FXML
    private void handleHost() {
        String pseudo = App.currentUser;
        if (pseudo == null || pseudo.trim().isEmpty())
            pseudo = "Host";

        lblStatusHost.setText("Démarrage du serveur...");
        isHost = true;
        players.clear();
        players.add(pseudo.trim());

        App.networkService.setMyPseudo(pseudo.trim());
        setupNetworking();

        // Use fixed port to save space in short code
        int port = 9999;
        try {
            App.networkService.demarrerServeur(port);
        } catch (Exception e) {
            // Fallback if 9999 is taken
            try {
                App.networkService.demarrerServeur(0);
            } catch (IOException ex) {
                lblStatusHost.setText("Erreur fatale port: " + ex.getMessage());
                return;
            }
        }

        port = App.networkService.getLocalPort();

        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            ip = "127.0.0.1";
        }

        // Use Short Code Compression
        String code = com.emsi.baclegend.util.CodeUtils.compress(ip, port);

        lblRoomCode.setText(code);
        lblStatusHost.setText("Serveur démarré !");
        lblStatusHost.setStyle("-fx-text-fill: #00ff87;");

        updatePlayerListUI();
        btnStartGame.setVisible(true);
        btnStartGame.setDisable(true);
    }

    @FXML
    private void handleJoin() {
        String pseudo = App.currentUser;
        if (pseudo == null || pseudo.trim().isEmpty())
            pseudo = "Guest";

        String code = txtCode.getText();

        if (code == null || code.trim().isEmpty()) {
            lblStatusJoin.setText("Entrez le code");
            return;
        }
        lblStatusJoin.setText("Connexion…");
        isHost = false;
        players.clear();
        players.add(pseudo.trim());

        App.networkService.setMyPseudo(pseudo.trim());
        setupNetworking();

        try {
            // Decompress Short Code
            String decoded = com.emsi.baclegend.util.CodeUtils.decompress(code.trim());

            if (decoded != null && decoded.contains(":")) {
                String[] parts = decoded.split(":");
                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);
                App.networkService.connecterAuServeur(ip, port);
            } else {
                lblStatusJoin.setText("Code invalide !");
            }
        } catch (Exception e) {
            lblStatusJoin.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void handleCopyCode() {
        String code = lblRoomCode.getText();
        if (code != null && !code.isEmpty() && !code.equals("...")) {
            final javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            final javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(code);
            clipboard.setContent(content);

            String oldText = lblStatusHost.getText();
            lblStatusHost.setText("Copié !");
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                }
                Platform.runLater(() -> lblStatusHost.setText(oldText));
            }).start();
        }
    }

    private void setupNetworking() {
        App.networkService.setMessageCallback(new ServiceReseau.MessageCallback() {
            @Override
            public void onMessageReceived(String message) {
                Platform.runLater(() -> handleMessage(message));
            }

            @Override
            public void onConnectionEstablished() {
                Platform.runLater(() -> {
                    if (isHost) {
                        // Client joined
                        lblStatusHost.setText("Nouveau client connecté !");
                        // We wait for NAME:... to add to list
                    } else {
                        // Connected to server
                        lblStatusJoin.setText("Connecté ! En attente de l'hôte...");
                        // Send my name
                        App.networkService.envoyerMessage("NAME:" + App.networkService.getMyPseudo());
                    }
                });
            }

            @Override
            public void onConnectionFailed(String error) {
                Platform.runLater(() -> {
                    if (isHost)
                        lblStatusHost.setText("Erreur: " + error);
                    else
                        lblStatusJoin.setText("Erreur: " + error);
                });
            }
        });
    }

    private void handleMessage(String message) {
        if (message.startsWith("NAME:")) {
            String newName = message.substring(5).trim();
            if (isHost) {
                players.add(newName);
                broadcastPlayerList();
                if (players.size() > 1)
                    btnStartGame.setDisable(false);
            }
        } else if (message.startsWith("PLAYERS:")) {
            String list = message.substring(8);
            players.clear();
            for (String p : list.split(",")) {
                if (!p.isBlank())
                    players.add(p.trim());
            }
            updatePlayerListUI();
        } else if (message.equals("START")) {
            goToGame();
        }
    }

    private void broadcastPlayerList() {
        if (!isHost)
            return;
        String list = String.join(",", players);
        App.networkService.broadcast("PLAYERS:" + list);
        updatePlayerListUI(); // Update host UI too
    }

    private void updatePlayerListUI() {
        vboxPlayers.getChildren().clear();
        for (String p : players) {
            Label l = new Label(p);
            l.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            vboxPlayers.getChildren().add(l);
        }
    }

    @FXML
    private void handleStartGame() {
        if (!isHost)
            return;
        // Broadcast Start
        App.networkService.broadcast("START");
        goToGame();
    }

    private void goToGame() {
        try {
            App.setRoot("view/game");
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText("Erreur lancement jeu: " + e.getMessage());
            a.show();
        }
    }

    @FXML
    private void handleRetour() throws IOException {
        App.networkService.fermerConnexion();
        App.setRoot("view/main");
    }
}
