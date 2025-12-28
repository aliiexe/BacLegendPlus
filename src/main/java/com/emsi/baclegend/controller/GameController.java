package com.emsi.baclegend.controller;

import com.emsi.baclegend.App;
import com.emsi.baclegend.dao.ScoreDAO;
import com.emsi.baclegend.model.Categorie;
import com.emsi.baclegend.service.MoteurJeu;
import com.emsi.baclegend.service.ServiceReseau;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameController {

    @FXML
    private Label lblLettre;
    @FXML
    private Label lblScore;
    @FXML
    private ProgressBar progressTimer;

    @FXML
    private VBox vboxCategories;
    @FXML
    private Button btnValider;

    // Overlay Elements
    @FXML
    private VBox overlayResults;
    @FXML
    private Label lblResultTitle;
    @FXML
    private TextArea txtResultDetails;
    @FXML
    private Button btnNextRound;
    @FXML
    private Label lblWaiting;

    // Deprecated but optional to keep for FXML compatibility if needed
    @FXML
    private Label lblAdversaireTitle;
    @FXML
    private Label lblAdversaireScore;

    private MoteurJeu moteurJeu;
    private Map<Categorie, TextField> champsSaisie;
    private boolean isMultiplayer = false;
    private boolean isHost = false;

    private Timeline countdown;
    private boolean gameStopped = false; // Input frozen

    // Multiplayer Host Logic
    private Map<String, Map<String, String>> allSubmittedAnswers = new ConcurrentHashMap<>();
    private Set<String> finishedPlayers = Collections.synchronizedSet(new HashSet<>());
    private Gson gson = new Gson();

    @FXML
    public void initialize() {
        moteurJeu = new MoteurJeu();
        champsSaisie = new HashMap<>();
        isMultiplayer = App.networkService.isConnected();
        isHost = App.networkService.isServerRunning();

        if (overlayResults != null)
            overlayResults.setVisible(false);

        if (isMultiplayer)
            setupMultiplayerCallbacks();

        moteurJeu.demarrerNouvellePartie();
        startTimer();
        updateUI();

        if (isMultiplayer && isHost) {
            startNewRoundHost();
        }
    }

    private void startNewRoundHost() {
        allSubmittedAnswers.clear();
        finishedPlayers.clear();
        char lettre = moteurJeu.getSessionCourante().getLettreCourante();
        System.out.println("HOST: Starting round with letter " + lettre);
        App.networkService.broadcast("LETTER:" + lettre);
    }

    private void setupMultiplayerCallbacks() {
        App.networkService.setMessageCallback(new ServiceReseau.MessageCallback() {
            @Override
            public void onMessageReceived(String message) {
                Platform.runLater(() -> handleNetworkMessage(message));
            }

            @Override
            public void onConnectionEstablished() {
            }

            @Override
            public void onConnectionFailed(String error) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("Erreur réseau: " + error);
                    alert.showAndWait();
                });
            }
        });
    }

    private void handleNetworkMessage(String message) {
        if (message.startsWith("LETTER:")) {
            if (message.length() > 7) {
                char lettre = message.charAt(7);
                moteurJeu.getSessionCourante().setLettreCourante(lettre);
                lblLettre.setText(String.valueOf(lettre));

                // Close overlay if open
                if (overlayResults != null)
                    overlayResults.setVisible(false);

                gameStopped = false;
                if (btnValider != null)
                    btnValider.setDisable(false);
                startTimer();
                updateUI();
            }
        } else if (message.equals("STOP")) {
            if (!gameStopped) {
                stopAndSubmit(false); // Auto-submit
            }
        } else if (message.startsWith("SUBMIT:")) {
            if (isHost)
                handleSubmission(message);
        } else if (message.startsWith("RESULTS:")) {
            showScoreboardOverlay(message.substring(8), true);
        }
    }

    private void handleSubmission(String message) {
        int firstColon = message.indexOf(':');
        int secondColon = message.indexOf(':', firstColon + 1);
        if (secondColon < 0)
            return;

        String pseudo = message.substring(firstColon + 1, secondColon);
        String json = message.substring(secondColon + 1);

        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> answers = gson.fromJson(json, type);

        // Prevent duplicate entries if logic loops
        if (allSubmittedAnswers.containsKey(pseudo))
            return;

        allSubmittedAnswers.put(pseudo, answers);
        finishedPlayers.add(pseudo);

        if (finishedPlayers.size() == 1) {
            // First player finished! Stop everyone.
            App.networkService.broadcast("STOP");

            // CRITICAL: Host does not receive broadcast, so Host must stop manually.
            if (!gameStopped) {
                Platform.runLater(() -> stopAndSubmit(false));
            }
        }

        int connectedClients = App.networkService.getClientCount();
        int totalPlayers = connectedClients + 1; // +1 for Host

        if (finishedPlayers.size() >= totalPlayers) {
            calculateAndBroadcastResults();
        }
    }

    private void calculateAndBroadcastResults() {
        Map<String, Integer> scores = new HashMap<>();
        Map<String, Boolean> validationCache = new HashMap<>();
        Map<String, Map<String, List<String>>> dupeMap = new HashMap<>();

        // 1. Validate all words
        for (String pseudo : allSubmittedAnswers.keySet()) {
            scores.put(pseudo, 0);
            Map<String, String> userAns = allSubmittedAnswers.get(pseudo);

            for (Map.Entry<String, String> entry : userAns.entrySet()) {
                String catName = entry.getKey();
                String word = entry.getValue().toLowerCase().trim();
                if (word.isEmpty())
                    continue;

                Categorie catObj = moteurJeu.getSessionCourante().getCategories().stream()
                        .filter(c -> c.getNom().equals(catName))
                        .findFirst().orElse(null);
                if (catObj == null)
                    continue;

                boolean isValid;
                String key = word + "|" + catName;
                if (validationCache.containsKey(key)) {
                    isValid = validationCache.get(key);
                } else {
                    char currentLetter = moteurJeu.getSessionCourante().getLettreCourante();
                    if (word.length() > 0
                            && Character.toUpperCase(word.charAt(0)) == Character.toUpperCase(currentLetter)) {
                        com.emsi.baclegend.service.ServiceValidation sv = new com.emsi.baclegend.service.ServiceValidation();
                        isValid = sv.validerMot(word, catObj);
                    } else {
                        isValid = false;
                    }
                    validationCache.put(key, isValid);
                }

                if (isValid) {
                    dupeMap.putIfAbsent(catName, new HashMap<>());
                    dupeMap.get(catName).putIfAbsent(word, new ArrayList<>());
                    dupeMap.get(catName).get(word).add(pseudo);
                }
            }
        }

        // 2. Calculate scores (with duplicate penalty)
        for (String pseudo : allSubmittedAnswers.keySet()) {
            int score = 0;
            Map<String, String> userAns = allSubmittedAnswers.get(pseudo);
            for (Map.Entry<String, String> entry : userAns.entrySet()) {
                String catName = entry.getKey();
                String word = entry.getValue().toLowerCase().trim();
                if (word.isEmpty())
                    continue;

                String key = word + "|" + catName;
                if (!validationCache.getOrDefault(key, false))
                    continue;

                // Check duplicates
                List<String> validUsers = dupeMap.get(catName).get(word);
                if (validUsers != null && validUsers.size() > 1) {
                    score += 5; // Duplicate penalty: 5 pts
                } else {
                    score += 10; // Unique: 10 pts
                }
            }
            scores.put(pseudo, score);
        }

        String jsonScores = gson.toJson(scores);
        App.networkService.broadcast("RESULTS:" + jsonScores);

        // CRITICAL: Host must also show results locally
        showScoreboardOverlay(jsonScores, true);
    }

    private void startTimer() {
        if (countdown != null)
            countdown.stop();

        final double totalSeconds = moteurJeu.getSessionCourante().getTempsLimite() > 0
                ? moteurJeu.getSessionCourante().getTempsLimite()
                : 60;
        final double steps = totalSeconds * 10;

        if (progressTimer != null) {
            progressTimer.setProgress(1.0);
            progressTimer.setStyle("-fx-accent: #00ff87;");
        }

        countdown = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            if (progressTimer == null)
                return;
            double current = progressTimer.getProgress();
            double decrement = 1.0 / steps;
            double next = current - decrement;

            if (next <= 0) {
                progressTimer.setProgress(0);
                stopAndSubmit(true);
            } else {
                progressTimer.setProgress(next);
                if (next < 0.25) {
                    progressTimer.setStyle("-fx-accent: #ff1744;");
                } else if (next < 0.5) {
                    progressTimer.setStyle("-fx-accent: #f5af19;");
                }
            }
        }));
        countdown.setCycleCount((int) steps + 1);
        countdown.play();
    }

    private void stopAndSubmit(boolean timeout) {
        if (gameStopped)
            return;
        gameStopped = true;

        if (countdown != null)
            countdown.stop();
        if (btnValider != null)
            btnValider.setDisable(true);

        // Disable all text fields
        champsSaisie.values().forEach(tf -> tf.setDisable(true));

        handleValiderHelper();

        if (isMultiplayer) {
            showWaitingOverlay();
        }
    }

    private void showWaitingOverlay() {
        if (overlayResults == null)
            return;
        Platform.runLater(() -> {
            lblResultTitle.setText("EN ATTENTE...");
            txtResultDetails
                    .setText("En attente des autres joueurs...\nLa manche se terminera quand tout le monde aura fini.");
            if (btnNextRound != null)
                btnNextRound.setVisible(false);
            overlayResults.setVisible(true);
        });
    }

    private void updateUI() {
        if (moteurJeu.getSessionCourante() == null)
            return;
        lblLettre.setText(String.valueOf(moteurJeu.getSessionCourante().getLettreCourante()));
        lblScore.setText(String.valueOf(moteurJeu.getScore()));
        genererChampsSaisie();
    }

    private void genererChampsSaisie() {
        vboxCategories.getChildren().clear();
        champsSaisie.clear();
        List<Categorie> categories = moteurJeu.getSessionCourante().getCategories();
        if (categories == null || categories.isEmpty())
            return;

        for (Categorie cat : categories) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("game-card");
            row.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-padding: 10;");

            Label label = new Label(cat.getNom() + " :");
            label.setPrefWidth(150);
            label.setFont(new Font(16));
            label.getStyleClass().add("label");

            TextField textField = new TextField();
            textField.setPromptText(moteurJeu.getSessionCourante().getLettreCourante() + "...");
            textField.setPrefWidth(300);
            textField.getStyleClass().add("text-field");

            row.getChildren().addAll(label, textField);
            vboxCategories.getChildren().add(row);
            champsSaisie.put(cat, textField);
        }
    }

    @FXML
    private void handleValider() {
        stopAndSubmit(false);
    }

    private void handleValiderHelper() {
        Map<Categorie, String> reponsesObjects = new HashMap<>();
        Map<String, String> reponsesStrings = new HashMap<>();

        for (Map.Entry<Categorie, TextField> entry : champsSaisie.entrySet()) {
            reponsesObjects.put(entry.getKey(), entry.getValue().getText());
            reponsesStrings.put(entry.getKey().getNom(), entry.getValue().getText());
        }

        if (isMultiplayer) {
            String pseudo = App.networkService.getMyPseudo();
            String json = gson.toJson(reponsesStrings);
            // Send answer to host (or broadcast if I am host, but handleSubmission handles
            // logic)
            App.networkService.envoyerMessage("SUBMIT:" + pseudo + ":" + json);

            // If I am host, I must also process my own submission locally immediately
            if (isHost) {
                handleSubmission("SUBMIT:" + pseudo + ":" + json);
            }
        } else {
            Map<String, Boolean> resultats = moteurJeu.soumettreReponses(reponsesObjects);
            lblScore.setText(String.valueOf(moteurJeu.getScore()));
            afficherResultatsSoloOverlay(resultats);
            saveSoloScore();
        }
    }

    private void showScoreboardOverlay(String jsonScores, boolean isMulti) {
        if (overlayResults == null)
            return;

        Type type = new TypeToken<Map<String, Integer>>() {
        }.getType();
        Map<String, Integer> scores = gson.fromJson(jsonScores, type);

        StringBuilder sb = new StringBuilder();
        // Header
        sb.append(String.format("%-20s | %s\n", "JOUEUR", "SCORE"));
        sb.append("------------------------------\n");

        scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> sb.append(String.format("%-20s | %d pts\n", e.getKey(), e.getValue())));

        Platform.runLater(() -> {
            lblResultTitle.setText("CLASSEMENT");
            txtResultDetails.setText(sb.toString());

            // Only Host can see "Next Round"
            if (btnNextRound != null) {
                btnNextRound.setVisible(isHost);
                btnNextRound.setText(isHost ? "Tour Suivant" : "En attente de l'hôte...");
                btnNextRound.setDisable(!isHost);
            }

            overlayResults.setVisible(true);
        });
    }

    private void afficherResultatsSoloOverlay(Map<String, Boolean> resultats) {
        if (overlayResults == null)
            return;

        StringBuilder sb = new StringBuilder();
        resultats.forEach((k, v) -> sb.append(String.format("%-20s : %s\n", k, v ? "✅ +10" : "❌")));

        Platform.runLater(() -> {
            lblResultTitle.setText("RÉSULTATS DE LA MANCHE");
            txtResultDetails.setText(sb.toString());
            if (btnNextRound != null) {
                btnNextRound.setVisible(true);
                btnNextRound.setText("Continuer");
                btnNextRound.setDisable(false);
            }
            overlayResults.setVisible(true);
        });
    }

    @FXML
    private void handleCloseResults() {
        // Called when button "Continue" is clicked in overlay
        if (isMultiplayer) {
            if (isHost) {
                startNewRoundHost();
            }
        } else {
            // Solo: Restart immediately
            overlayResults.setVisible(false);
            moteurJeu.getSessionCourante().demarrerPartie();
            gameStopped = false;
            if (btnValider != null)
                btnValider.setDisable(false);
            startTimer();
            updateUI();
        }
    }

    private void saveSoloScore() {
        String pseudo = App.currentUser;
        if (pseudo == null)
            pseudo = "Joueur";
        ScoreDAO sdao = new ScoreDAO();
        sdao.saveScore(pseudo, moteurJeu.getScore());
    }

    @FXML
    private void handleRetour() throws IOException {
        if (countdown != null)
            countdown.stop();
        if (isMultiplayer)
            App.networkService.fermerConnexion();
        App.setRoot("view/main");
    }
}
