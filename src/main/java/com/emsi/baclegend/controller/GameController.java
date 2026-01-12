package com.emsi.baclegend.controller;

import com.emsi.baclegend.App;
import com.emsi.baclegend.dao.ScoreDAO;
import com.emsi.baclegend.model.Categorie;
import com.emsi.baclegend.service.MoteurJeu;
import com.emsi.baclegend.service.ServiceReseau;
import com.emsi.baclegend.util.TranslationUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
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
    @FXML
    private VBox notificationArea;
    @FXML
    private Label lblCountdown;
    @FXML
    private javafx.scene.layout.StackPane countdownOverlay;
    @FXML
    private Label lblLetterLabel;
    @FXML
    private Label lblTimeLabel;
    @FXML
    private Label lblScoreLabel;
    @FXML
    private Label lblLanguage;
    @FXML
    private Label lblLanguageLabel;
    @FXML
    private VBox vboxLanguage;
    @FXML
    private Button btnQuit;
    @FXML
    private Button btnQuitOverlay;

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
    private boolean roundOver = false;

    // Multiplayer Host Logic
    private Map<String, Map<String, String>> allSubmittedAnswers = new ConcurrentHashMap<>();
    private Set<String> finishedPlayers = Collections.synchronizedSet(new HashSet<>());
    private Set<String> disconnectedPlayers = Collections.synchronizedSet(new HashSet<>());
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

        // In multiplayer, wait for categories from host (they will be set via CATEGORIES: message)
        // For solo mode or if categories already received, start the game
        if (isMultiplayer && !isHost && App.sharedCategories == null) {
            // Client waiting for categories - will be handled when CATEGORIES: message arrives
            System.out.println("CLIENT: Waiting for categories from host...");
        } else {
            // Use shared categories if available (from host), otherwise load from local DB
            List<Categorie> categoriesToUse = App.sharedCategories != null ? App.sharedCategories : null;
            
            // Use shared letter if available (set by LobbyController)
            if (App.sharedLetter != null) {
                if (categoriesToUse != null) {
                    moteurJeu.demarrerNouvellePartie(categoriesToUse, App.sharedLetter);
                } else {
                    moteurJeu.demarrerNouvellePartie(App.sharedLetter);
                }
                App.sharedLetter = null; // Clear it
            } else {
                if (categoriesToUse != null) {
                    moteurJeu.demarrerNouvellePartie(categoriesToUse);
                } else {
                    moteurJeu.demarrerNouvellePartie();
                }
            }
            
            // Clear shared categories after use (for next round, new categories will be sent)
            if (isMultiplayer && !isHost) {
                App.sharedCategories = null;
            }
        }

        updateTranslations();

        if (isMultiplayer && isHost) {
            startNewRoundHost();
        } else {
            // Solo mode: start with countdown
            char lettre = moteurJeu.getSessionCourante().getLettreCourante();
            runCountdown(lettre);
        }
    }
    
    private void updateTranslations() {
        Platform.runLater(() -> {
            if (lblLetterLabel != null)
                lblLetterLabel.setText("🔤 " + TranslationUtil.translate("game.letter"));
            if (lblTimeLabel != null)
                lblTimeLabel.setText("⏱️ " + TranslationUtil.translate("game.time"));
            if (lblScoreLabel != null)
                lblScoreLabel.setText("⭐ " + TranslationUtil.translate("game.score"));
            if (btnValider != null)
                btnValider.setText("✅ " + TranslationUtil.translate("game.validate"));
            if (btnQuit != null)
                btnQuit.setText("❌ " + TranslationUtil.translate("game.quit"));
            if (btnQuitOverlay != null)
                btnQuitOverlay.setText("❌ " + TranslationUtil.translate("game.quit"));
            if (btnNextRound != null)
                btnNextRound.setText("➡️ " + TranslationUtil.translate("game.nextRound"));
            if (lblLanguage != null)
                lblLanguage.setText(TranslationUtil.getLanguageDisplayName());
            if (lblLanguageLabel != null)
                lblLanguageLabel.setText("🌐 " + TranslationUtil.translate("game.language"));
            // Show language indicator only in multiplayer
            if (vboxLanguage != null)
                vboxLanguage.setVisible(isMultiplayer);
            
            // Regenerate category fields to update translated labels
            if (moteurJeu != null && moteurJeu.getSessionCourante() != null) {
                genererChampsSaisie();
            }
        });
    }

    private void startNewRoundHost() {
        allSubmittedAnswers.clear();
        finishedPlayers.clear();
        // Keep disconnectedPlayers to prevent re-notification

        // Load categories from host's database (always use host's categories)
        com.emsi.baclegend.dao.CategorieDAO categorieDAO = new com.emsi.baclegend.dao.CategorieDAO();
        List<Categorie> categories = categorieDAO.obtenirToutes();
        
        // Serialize categories to JSON
        java.util.List<java.util.Map<String, Object>> categoriesData = new java.util.ArrayList<>();
        for (Categorie cat : categories) {
            java.util.Map<String, Object> catData = new java.util.HashMap<>();
            catData.put("id", cat.getId());
            catData.put("nom", cat.getNom());
            catData.put("estActive", cat.isEstActive());
            categoriesData.add(catData);
        }
        String categoriesJson = gson.toJson(categoriesData);
        
        // Broadcast categories to all clients (for each new round)
        App.networkService.broadcast("CATEGORIES:" + categoriesJson);
        System.out.println("HOST: Broadcasting " + categories.size() + " categories to all clients for new round");
        
        // Generate new letter locally first
        moteurJeu.demarrerNouvellePartie(categories);
        
        char lettre = moteurJeu.getSessionCourante().getLettreCourante();
        System.out.println("HOST: Starting round with letter " + lettre + " and " + 
                          moteurJeu.getSessionCourante().getCategories().size() + " categories");

        // Broadcast to clients
        App.networkService.broadcast("LETTER:" + lettre);

        // Process locally immediately (Host also sees countdown)
        handleNetworkMessage("LETTER:" + lettre);
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
                    // If connection failed, it might be because the other player left in a 2-player game
                    // Show appropriate message
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle(TranslationUtil.translate("game.quit"));
                    alert.setHeaderText(null);
                    alert.setContentText(TranslationUtil.translate("game.ended.solo"));
                    alert.showAndWait();
                    try {
                        if (countdown != null)
                            countdown.stop();
                        App.networkService.fermerConnexion();
                        App.setRoot("view/main");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onClientDisconnected(String pseudo) {
                Platform.runLater(() -> {
                    // Skip if we've already handled this disconnection
                    if (disconnectedPlayers.contains(pseudo)) {
                        return;
                    }
                    disconnectedPlayers.add(pseudo);

                    // Show non-blocking notification to all players
                    showNotification(TranslationUtil.translate("player.left", pseudo));

                    if (isHost) {
                        // Remove player from tracking
                        allSubmittedAnswers.remove(pseudo);
                        finishedPlayers.remove(pseudo);

                        // Broadcast disconnect to all clients so they also show notification
                        App.networkService.broadcast("PLAYER_DISCONNECTED:" + pseudo);

                        // Recalculate total players (excluding disconnected ones)
                        int connected = App.networkService.getClientCount();
                        int total = connected + 1; // +1 for Host

                        // If only 2 players total and one left, end the game
                        if (total == 1) {
                            // Only host remains - end the game
                            endGameForSoloPlayer();
                            return;
                        }

                        // If we're waiting for results and all remaining players have finished
                        if (gameStopped && !roundOver) {
                            if (finishedPlayers.size() >= total && total > 1) {
                                // Everyone remaining has finished, calculate results
                                calculateAndBroadcastResults();
                            }
                        }
                    }
                    // Client side: if host disconnects or connection is lost, onConnectionFailed will handle it
                });
            }
        });
    }

    private void handleNetworkMessage(String message) {
        if (message.startsWith("CATEGORIES:")) {
            // Client receives categories from host
            String categoriesJson = message.substring(11);
            System.out.println("CLIENT: Received categories from host");
            
            try {
                // Parse JSON categories
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> categoriesData = gson.fromJson(categoriesJson, listType);
                
                // Convert to Categorie objects
                List<Categorie> categories = new ArrayList<>();
                for (Map<String, Object> catData : categoriesData) {
                    int id = ((Double) catData.get("id")).intValue();
                    String nom = (String) catData.get("nom");
                    boolean estActive = (Boolean) catData.get("estActive");
                    categories.add(new Categorie(id, nom, estActive));
                }
                
                // Store categories for use (will be used when LETTER: arrives)
                App.sharedCategories = categories;
                System.out.println("CLIENT: Loaded " + categories.size() + " categories from host");
                
                // Store categories - they will be used when LETTER: message arrives
                // If we already have a letter, start the game immediately
                if (App.sharedLetter != null) {
                    // Update session with new categories and letter
                    moteurJeu.demarrerNouvellePartie(categories, App.sharedLetter);
                    App.sharedCategories = null; // Clear after use
                    System.out.println("CLIENT: Started game with received categories and letter " + App.sharedLetter);
                }
                // Otherwise, categories will be used when LETTER: message arrives
            } catch (Exception e) {
                System.err.println("CLIENT: Error parsing categories: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (message.startsWith("LETTER:")) {
            if (message.length() > 7) {
                char lettre = message.charAt(7);
                
                // If we have shared categories, use them; otherwise use existing session or create new one
                if (App.sharedCategories != null) {
                    // We received categories, use them with the letter
                    moteurJeu.demarrerNouvellePartie(App.sharedCategories, lettre);
                    App.sharedCategories = null; // Clear after use
                    System.out.println("CLIENT: Started game with categories and letter " + lettre);
                } else if (moteurJeu.getSessionCourante() != null) {
                    // Update existing session with new letter
                    moteurJeu.getSessionCourante().setLettreCourante(lettre);
                    System.out.println("CLIENT: Updated existing session with letter " + lettre);
                } else {
                    // Fallback: create session with local categories (shouldn't happen in multiplayer)
                    moteurJeu.demarrerNouvellePartie(lettre);
                    System.out.println("CLIENT: WARNING - Created session with local categories (fallback)");
                }
                
                lblLettre.setText(String.valueOf(lettre));

                if (overlayResults != null)
                    overlayResults.setVisible(false);

                gameStopped = false;
                roundOver = false;

                // Regenerate category fields with new categories
                genererChampsSaisie();

                // Start Countdown instead of starting timer immediately
                runCountdown(lettre);
            }
        } else if (message.equals("SERVER_STOP")) {
            Platform.runLater(() -> {
                // Host stopped the game - this could be because only 2 players and one left
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(TranslationUtil.translate("game.quit"));
                alert.setHeaderText(null);
                alert.setContentText(TranslationUtil.translate("game.ended.solo"));
                alert.showAndWait();
                try {
                    // Force quiet return
                    if (countdown != null)
                        countdown.stop();
                    App.networkService.fermerConnexion();
                    App.setRoot("view/main");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else if (message.equals("STOP")) {
            if (!gameStopped) {
                stopAndSubmit(false); // Auto-submit
            }
            // Update overlay to show we are calculating/waiting
            showWaitingOverlay("Calcul des résultats...");
        } else if (message.startsWith("SUBMIT:")) {
            if (isHost)
                handleSubmission(message);
        } else if (message.startsWith("RESULTS:")) {
            roundOver = true;
            showScoreboardOverlay(message.substring(8), true);
        } else if (message.startsWith("PLAYER_DISCONNECTED:")) {
            // Client receives notification that another player disconnected
            String disconnectedPseudo = message.substring(20);
            showNotification(TranslationUtil.translate("player.left", disconnectedPseudo));
            
            // Check if we're now alone (only host + this client = 2, if one left, we're alone)
            // As a client, we can't know exact count, but if host sends GAME_ENDED_SOLO, we'll handle it
        } else if (message.equals("GAME_ENDED_SOLO")) {
            // Host ended the game because only one player remains
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(TranslationUtil.translate("game.quit"));
                alert.setHeaderText(null);
                alert.setContentText(TranslationUtil.translate("game.ended.solo"));
                alert.showAndWait();
                try {
                    if (countdown != null)
                        countdown.stop();
                    App.networkService.fermerConnexion();
                    App.setRoot("view/main");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else if (message.startsWith("LANGUAGE_NOTIFICATION:")) {
            // Show language notification
            String langDisplay = message.substring(22);
            showNotification("🌐 " + TranslationUtil.translate("game.language") + ": " + langDisplay);
            // Update language display
            if (lblLanguage != null) {
                lblLanguage.setText(langDisplay);
            }
            // Refresh translations
            updateTranslations();
        } else if (message.startsWith("DISCONNECT:")) {
            // Host receives explicit disconnect message from client
            String disconnectedPseudo = message.substring(11);
            // Mark as disconnected and trigger handler
            disconnectedPlayers.add(disconnectedPseudo);
            // Trigger the disconnection handler (same logic as onClientDisconnected)
            Platform.runLater(() -> {
                showNotification(TranslationUtil.translate("player.left", disconnectedPseudo));
                
                if (isHost) {
                    // Remove player from tracking
                    allSubmittedAnswers.remove(disconnectedPseudo);
                    finishedPlayers.remove(disconnectedPseudo);
                    
                    // Broadcast disconnect to all clients
                    App.networkService.broadcast("PLAYER_DISCONNECTED:" + disconnectedPseudo);
                    
                    // Recalculate total players
                    int connected = App.networkService.getClientCount();
                    int total = connected + 1; // +1 for Host
                    
                    // If only 2 players total and one left, end the game
                    if (total == 1) {
                        // Only host remains - end the game
                        endGameForSoloPlayer();
                        return;
                    }
                    
                    // If we're waiting for results and all remaining players have finished
                    if (gameStopped && !roundOver) {
                        if (finishedPlayers.size() >= total && total > 1) {
                            calculateAndBroadcastResults();
                        }
                    }
                }
            });
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
                Platform.runLater(() -> {
                    stopAndSubmit(false);
                    // showWaitingOverlay("Calcul des résultats..."); // Removed to avoid race
                    // condition
                });
            }
        }

        // Recalculate total players dynamically (in case someone disconnected)
        int connectedClients = App.networkService.getClientCount();
        int totalPlayers = connectedClients + 1; // +1 for Host

        // Only calculate if we have at least 2 players (host + at least 1 client)
        // and all remaining players have finished
        if (totalPlayers >= 2 && finishedPlayers.size() >= totalPlayers) {
            calculateAndBroadcastResults();
        } else if (totalPlayers == 1) {
            // Only host remains, show notification
            Platform.runLater(() -> {
                showNotification("Tous les joueurs sont partis.");
            });
        }
    }

    private void calculateAndBroadcastResults() {
        // Show loading state immediately on UI thread
        Platform.runLater(() -> showWaitingOverlay(TranslationUtil.translate("game.calculating")));

        new Thread(() -> {
            Map<String, Integer> scores = new HashMap<>();
            Map<String, Boolean> validationCache = new HashMap<>();
            Map<String, Map<String, List<String>>> dupeMap = new HashMap<>();

            // 1. Validate all words (Heavy Operation)
            for (String pseudo : allSubmittedAnswers.keySet()) {
                scores.put(pseudo, 0);
                Map<String, String> userAns = allSubmittedAnswers.get(pseudo);

                for (Map.Entry<String, String> entry : userAns.entrySet()) {
                    String catName = entry.getKey();
                    String word = entry.getValue().toLowerCase().trim();
                    if (word.isEmpty())
                        continue;

                    // Find category - try exact match first, then try reverse translation
                    Categorie catObj = moteurJeu.getSessionCourante().getCategories().stream()
                            .filter(c -> {
                                String originalName = c.getNom().trim();
                                String translatedName = TranslationUtil.translateCategory(originalName);
                                return originalName.equalsIgnoreCase(catName.trim()) || 
                                       translatedName.equalsIgnoreCase(catName.trim());
                            })
                            .findFirst().orElse(null);
                    if (catObj == null) {
                        System.out.println("HOST VALIDATION: Category not found: '" + catName + "'. Available categories: " + 
                            moteurJeu.getSessionCourante().getCategories().stream()
                                .map(c -> c.getNom() + "/" + TranslationUtil.translateCategory(c.getNom()))
                                .reduce((a, b) -> a + ", " + b).orElse("none"));
                        continue;
                    }

                    boolean isValid;
                    // Use original category name for cache key (not translated)
                    String originalCatName = catObj.getNom();
                    String key = word + "|" + originalCatName + "|" + App.gameLanguage;
                    
                    System.out.println("HOST VALIDATION: Checking cache for key: " + key);
                    if (validationCache.containsKey(key)) {
                        isValid = validationCache.get(key);
                        System.out.println("HOST VALIDATION: Found in cache: " + isValid);
                    } else {
                        char currentLetter = moteurJeu.getSessionCourante().getLettreCourante();
                        if (word.length() > 0
                                && Character.toUpperCase(word.charAt(0)) == Character.toUpperCase(currentLetter)) {
                            System.out.println("HOST VALIDATION: First letter matches, calling validation service...");
                            // Instantiate new ServiceValidation for thread safety if valid
                            com.emsi.baclegend.service.ServiceValidation sv = new com.emsi.baclegend.service.ServiceValidation();
                            isValid = sv.validerMot(word, catObj);
                            System.out.println("HOST VALIDATION: '" + word + "' in '" + originalCatName + "' (lang: " + App.gameLanguage + ") -> " + isValid);
                            validationCache.put(key, isValid);
                        } else {
                            System.out.println("HOST VALIDATION: Invalid start match: word='" + word + "', letter='"
                                    + currentLetter + "'");
                            isValid = false;
                            validationCache.put(key, false);
                        }
                    }

                    if (isValid) {
                        // Use original category name for dupeMap to match score calculation lookup
                        dupeMap.putIfAbsent(originalCatName, new HashMap<>());
                        dupeMap.get(originalCatName).putIfAbsent(word, new ArrayList<>());
                        dupeMap.get(originalCatName).get(word).add(pseudo);
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

                    // Find the category object (catName might be translated)
                    Categorie catObj = moteurJeu.getSessionCourante().getCategories().stream()
                            .filter(c -> {
                                String originalName = c.getNom().trim();
                                String translatedName = TranslationUtil.translateCategory(originalName);
                                return originalName.equalsIgnoreCase(catName.trim()) || 
                                       translatedName.equalsIgnoreCase(catName.trim());
                            })
                            .findFirst().orElse(null);
                    if (catObj == null) {
                        System.out.println("SCORE CALC: Category not found: '" + catName + "'");
                        continue;
                    }

                    // Include language in cache key to match validation key
                    // Use the same cache key format as validation (with original category name)
                    // catName might be translated, but we need to use original for cache lookup
                    String originalCatName = catObj.getNom();
                    String key = word + "|" + originalCatName + "|" + App.gameLanguage;
                    
                    // If word was not valid (not in cache or false), skip
                    if (!validationCache.getOrDefault(key, false)) {
                        System.out.println("SCORE CALC: Word not valid (not in cache): " + key);
                        continue;
                    }

                    // Check duplicates - use original category name for dupeMap
                    Map<String, List<String>> categoryWords = dupeMap.get(originalCatName);
                    if (categoryWords != null) {
                        List<String> validUsers = categoryWords.get(word);
                        if (validUsers != null && validUsers.size() > 1) {
                            score += 5; // Duplicate penalty: 5 pts
                            System.out.println("SCORE CALC: Duplicate word, +5 pts");
                        } else if (validUsers != null) {
                            score += 10; // Unique: 10 pts
                            System.out.println("SCORE CALC: Unique word, +10 pts");
                        }
                    } else {
                        System.out.println("SCORE CALC: Category not in dupeMap: " + originalCatName);
                    }
                }
                scores.put(pseudo, score);
            }

            String jsonScores = gson.toJson(scores);

            // Update UI and Broadcast on Main Thread
            Platform.runLater(() -> {
                App.networkService.broadcast("RESULTS:" + jsonScores);
                // CRITICAL: Host must also show results locally
                roundOver = true;
                showScoreboardOverlay(jsonScores, true);
            });

        }).start();
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
            showWaitingOverlay("En attente des autres joueurs...");
        }
    }

    private void showWaitingOverlay(String msg) {
        if (overlayResults == null || roundOver)
            return;
        Platform.runLater(() -> {
            lblResultTitle.setText(TranslationUtil.translate("game.waiting"));
            String waitingMsg = TranslationUtil.translate("game.waiting.msg");
            txtResultDetails.setText(msg + "\n" + waitingMsg);
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
            row.getStyleClass().add("category-card");

            // Translate category name based on current language
            String categoryName = TranslationUtil.translateCategory(cat.getNom());
            Label label = new Label(categoryName + " :");
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
            lblResultTitle.setText(TranslationUtil.translate("game.results"));
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
            lblResultTitle.setText(TranslationUtil.translate("game.results"));
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
            // Solo: Restart with countdown
            overlayResults.setVisible(false);
            moteurJeu.demarrerNouvellePartie();
            gameStopped = false;
            char lettre = moteurJeu.getSessionCourante().getLettreCourante();
            runCountdown(lettre);
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

    private void showNotification(String message) {
        if (notificationArea == null)
            return;
        notificationArea.toFront(); // Ensure it's on top

        Label toast = new Label(message);
        toast.getStyleClass().add("notification-toast");
        toast.setFont(new Font(14));

        notificationArea.getChildren().add(toast);

        // Auto-hide after 3 seconds
        Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    notificationArea.getChildren().remove(toast);
                }));
        fadeOut.play();
    }

    private void endGameForSoloPlayer() {
        // Stop timers and game
        if (countdown != null)
            countdown.stop();
        gameStopped = true;
        roundOver = true;
        
        // Broadcast to all clients that game ended
        if (isHost) {
            App.networkService.broadcast("GAME_ENDED_SOLO");
        }
        
        // Show alert
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(TranslationUtil.translate("game.quit"));
            alert.setHeaderText(null);
            alert.setContentText(TranslationUtil.translate("game.ended.solo"));
            alert.showAndWait();
            
            // Return to main menu
            try {
                App.networkService.fermerConnexion();
                App.setRoot("view/main");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleRetour() throws IOException {
        if (isHost) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmer la sortie");
            alert.setHeaderText("Arrêter la partie ?");
            alert.setContentText("Voulez-vous vraiment quitter ? Cela arrêtera la partie pour tout le monde.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                App.networkService.broadcast("SERVER_STOP");
                if (countdown != null)
                    countdown.stop();
                App.networkService.fermerConnexion();
                App.setRoot("view/main");
            }
            // Else do nothing
            return;
        }

        if (countdown != null)
            countdown.stop();
        if (isMultiplayer) {
            // Send disconnect message to server before closing
            String myPseudo = App.networkService.getMyPseudo();
            if (myPseudo != null && !myPseudo.isEmpty()) {
                App.networkService.envoyerMessage("DISCONNECT:" + myPseudo);
            }
            // Small delay to ensure message is sent
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
            // If Client quits, close connection
            App.networkService.fermerConnexion();
        }
        App.setRoot("view/main");
    }

    private void runCountdown(char lettre) {
        if (lblCountdown == null || countdownOverlay == null) {
            // Fallback if UI not ready
            if (btnValider != null)
                btnValider.setDisable(false);
            startTimer();
            updateUI();
            return;
        }

        // Disable everything during countdown
        if (btnValider != null)
            btnValider.setDisable(true);
        if (champsSaisie != null)
            champsSaisie.values().forEach(tf -> tf.setDisable(true));

        // Show overlay and countdown
        countdownOverlay.setVisible(true);
        countdownOverlay.toFront();
        lblCountdown.setVisible(true);
        lblCountdown.toFront();

        // Create scale and fade animations
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), lblCountdown);
        scaleIn.setFromX(0.3);
        scaleIn.setFromY(0.3);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), lblCountdown);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), lblCountdown);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        // Sequence: READY (0s) -> SET (1s) -> GO (2s) -> LETTER (3s) -> START (4.5s)
        Timeline sequence = new Timeline(
                new KeyFrame(Duration.seconds(0), e -> {
                    lblCountdown.setText("READY");
                    lblCountdown.getStyleClass().clear();
                    lblCountdown.getStyleClass().add("countdown-ready");
                    scaleIn.play();
                    fadeIn.play();
                }),
                new KeyFrame(Duration.seconds(0.8), e -> {
                    fadeOut.play();
                }),
                new KeyFrame(Duration.seconds(1), e -> {
                    lblCountdown.setText("SET");
                    lblCountdown.getStyleClass().clear();
                    lblCountdown.getStyleClass().add("countdown-set");
                    fadeIn.play();
                    scaleIn.play();
                }),
                new KeyFrame(Duration.seconds(1.8), e -> {
                    fadeOut.play();
                }),
                new KeyFrame(Duration.seconds(2), e -> {
                    lblCountdown.setText("GO!");
                    lblCountdown.getStyleClass().clear();
                    lblCountdown.getStyleClass().add("countdown-go");
                    fadeIn.play();
                    scaleIn.play();
                }),
                new KeyFrame(Duration.seconds(2.8), e -> {
                    fadeOut.play();
                }),
                new KeyFrame(Duration.seconds(3), e -> {
                    lblCountdown.setText(String.valueOf(lettre));
                    lblCountdown.getStyleClass().clear();
                    lblCountdown.getStyleClass().add("countdown-letter");
                    fadeIn.play();
                    scaleIn.play();
                }),
                // Keep letter visible for 1.5 seconds
                new KeyFrame(Duration.seconds(4.3), e -> {
                    fadeOut.play();
                }),
                new KeyFrame(Duration.seconds(4.5), e -> {
                    countdownOverlay.setVisible(false);
                    lblCountdown.setVisible(false);
                    // Start Game
                    if (btnValider != null)
                        btnValider.setDisable(false);
                    updateUI(); // Generate fields
                    startTimer();
                }));
        sequence.play();
    }
}
