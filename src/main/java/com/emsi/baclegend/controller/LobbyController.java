package com.emsi.baclegend.controller;

import com.emsi.baclegend.App;
import com.emsi.baclegend.service.ServiceReseau;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
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

    @FXML
    private ComboBox<String> comboTime;

    @FXML
    private ComboBox<String> comboLanguage;

    private boolean isHost = false;
    private Set<String> players = ConcurrentHashMap.newKeySet();

    @FXML
    public void initialize() {
        // Initialize time options
        comboTime.setItems(FXCollections.observableArrayList(
                "30 secondes",
                "45 secondes",
                "60 secondes",
                "90 secondes",
                "120 secondes",
                "180 secondes"));
        comboTime.setValue("60 secondes");

        // Style the combo box
        comboTime.setStyle("-fx-background-color: rgba(15, 52, 96, 0.8); -fx-text-fill: white;");

        // Initialize language options
        comboLanguage.setItems(FXCollections.observableArrayList("Français", "English"));
        comboLanguage.setValue("Français"); // Default to French
        comboLanguage.setStyle("-fx-background-color: rgba(15, 52, 96, 0.8); -fx-text-fill: white;");
    }

    private int getSelectedTime() {
        String selected = comboTime.getValue();
        if (selected == null)
            return 60;
        // Extract number from string like "60 secondes"
        String[] parts = selected.split(" ");
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return 60;
        }
    }

    @FXML
    private void handleHost() {
        String pseudo = App.currentUser;
        if (pseudo == null || pseudo.trim().isEmpty())
            pseudo = "Host";

        lblStatusHost.setText("Démarrage du serveur...");
        isHost = true;
        players.clear();
        players.add(pseudo.trim());

        // Set the game time from combo box
        App.gameTimeDuration = getSelectedTime();

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

        String ip = getLocalNetworkIP();
        System.out.println("IP réseau détectée pour le code: " + ip);
        System.out.println("Port utilisé: " + port);

        // Use Short Code Compression
        String code = com.emsi.baclegend.util.CodeUtils.compress(ip, port);

        lblRoomCode.setText(code);
        lblStatusHost.setText("Serveur démarré ! IP: " + ip + ", Port: " + port + ", Durée: " + getSelectedTime() + "s");
        lblStatusHost.getStyleClass().add("success-text");

        // Disable time and language selection after server started
        comboTime.setDisable(true);
        comboLanguage.setDisable(true);

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
            System.out.println("Code décompressé: " + decoded);

            if (decoded != null && decoded.contains(":")) {
                String[] parts = decoded.split(":");
                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);
                System.out.println("Tentative de connexion à IP: " + ip + ", Port: " + port);
                App.networkService.connecterAuServeur(ip, port);
            } else {
                lblStatusJoin.setText("Code invalide !");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la décompression/connexion: " + e.getMessage());
            e.printStackTrace();
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

    @FXML
    private void handleStartGame() {
        if (!isHost)
            return;
        
        // Set language preference from host selection
        String selectedLang = comboLanguage.getValue();
        if (selectedLang != null) {
            if (selectedLang.equals("Français")) {
                App.gameLanguage = "FR";
            } else {
                App.gameLanguage = "EN";
            }
        }
        
        // Broadcast time setting first
        App.networkService.broadcast("TIME:" + App.gameTimeDuration);
        
        // Broadcast language setting
        App.networkService.broadcast("LANGUAGE:" + App.gameLanguage);
        
        // Show language notification to all players
        String langDisplay = App.gameLanguage.equals("FR") ? "Français" : "English";
        App.networkService.broadcast("LANGUAGE_NOTIFICATION:" + langDisplay);

        // Generate letter
        char lettre = (char) ('A' + new java.util.Random().nextInt(26));
        App.sharedLetter = lettre;

        // Broadcast Start with letter
        App.networkService.broadcast("START:" + lettre);
        goToGame();
    }

    private void handleMessage(String message) {
        if (message.startsWith("NAME:")) {
            // ... (unchanged)
            String newName = message.substring(5).trim();
            if (isHost) {
                players.add(newName);
                broadcastPlayerList();
                if (players.size() > 1)
                    btnStartGame.setDisable(false);
            }
        } else if (message.startsWith("PLAYERS:")) {
            // ... (unchanged)
            String list = message.substring(8);
            players.clear();
            for (String p : list.split(",")) {
                if (!p.isBlank())
                    players.add(p.trim());
            }
            updatePlayerListUI();
        } else if (message.startsWith("TIME:")) {
            // ... (unchanged)
            try {
                int time = Integer.parseInt(message.substring(5).trim());
                App.gameTimeDuration = time;
                lblStatusJoin.setText("Durée de la manche: " + time + "s");
            } catch (NumberFormatException e) {
                // Ignore invalid time
            }
        } else if (message.startsWith("LANGUAGE:")) {
            // Receive language setting from host
            String lang = message.substring(9).trim();
            App.gameLanguage = lang;
            String langDisplay = lang.equals("FR") ? "Français" : "English";
            lblStatusJoin.setText("Langue: " + langDisplay);
        } else if (message.startsWith("LANGUAGE_NOTIFICATION:")) {
            // Language notification - will be handled in GameController
            // No action needed here
        } else if (message.startsWith("START")) {
            // Handle START or START:X
            if (message.contains(":")) {
                String l = message.split(":")[1];
                if (l.length() > 0) {
                    App.sharedLetter = l.charAt(0);
                }
            }
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
            l.getStyleClass().add("label");
            l.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            vboxPlayers.getChildren().add(l);
        }
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

    /**
     * Gets the local network IP address (not localhost) for LAN connections.
     * Returns the first non-loopback, non-link-local IPv4 address found.
     * Falls back to localhost if no network interface is found.
     */
    private String getLocalNetworkIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    // Skip loopback and link-local addresses
                    if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
                        continue;
                    }
                    
                    // Prefer IPv4 addresses (for LAN compatibility)
                    if (address.getHostAddress().contains(".") && !address.getHostAddress().startsWith("127.")) {
                        String ip = address.getHostAddress();
                        System.out.println("Found network IP: " + ip + " on interface: " + networkInterface.getName());
                        return ip;
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Error finding network IP: " + e.getMessage());
        }
        
        // Fallback to localhost if no network interface found
        try {
            String localhost = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Using localhost IP: " + localhost);
            return localhost;
        } catch (Exception e) {
            System.err.println("Error getting localhost IP: " + e.getMessage());
            return "127.0.0.1";
        }
    }
}
