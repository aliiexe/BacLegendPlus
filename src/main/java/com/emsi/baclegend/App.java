package com.emsi.baclegend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    public static com.emsi.baclegend.service.ServiceReseau networkService = new com.emsi.baclegend.service.ServiceReseau();
    public static String currentUser = "Joueur" + (int) (Math.random() * 10000); // Default Unique ID

    // Game settings
    // Game settings
    public static int gameTimeDuration = 60; // Default 60 seconds
    public static Character sharedLetter = null;
    public static String gameLanguage = "FR"; // Default French, can be "FR" or "EN"
    public static java.util.List<com.emsi.baclegend.model.Categorie> sharedCategories = null; // Categories from host in multiplayer

    @Override
    public void start(Stage stage) throws IOException {
        // Initialiser la base de données dès le démarrage
        com.emsi.baclegend.dao.GestionnaireBaseDeDonnees.getConnexion();

        scene = new Scene(loadFXML("view/main"), 1000, 800);
        scene.getStylesheets().add(App.class.getResource("style.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("BacLegend");

        // Fermer les connexions réseau à la fermeture de l'application
        stage.setOnCloseRequest(event -> {
            networkService.fermerConnexion();
        });

        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
