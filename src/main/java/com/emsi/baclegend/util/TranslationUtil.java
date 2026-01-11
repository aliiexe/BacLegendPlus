package com.emsi.baclegend.util;

import com.emsi.baclegend.App;
import java.util.HashMap;
import java.util.Map;

public class TranslationUtil {
    
    private static final Map<String, Map<String, String>> translations = new HashMap<>();
    
    static {
        // French translations
        Map<String, String> fr = new HashMap<>();
        fr.put("game.title", "BacLegend");
        fr.put("game.subtitle", "Le jeu du petit bac ultime");
        fr.put("game.pseudo", "Votre Pseudo:");
        fr.put("game.pseudo.placeholder", "Entrez votre nom");
        fr.put("game.solo", "Mode Solo");
        fr.put("game.multiplayer", "Multijoueur");
        fr.put("game.categories", "Gérer les Catégories");
        fr.put("game.settings", "Paramètres");
        fr.put("game.quit", "Quitter");
        fr.put("game.letter", "Lettre");
        fr.put("game.time", "Temps Restant");
        fr.put("game.score", "Score");
        fr.put("game.validate", "VALIDER !");
        fr.put("game.results", "RÉSULTATS DU TOUR");
        fr.put("game.nextRound", "Tour Suivant / Rejouer");
        fr.put("game.waiting", "EN ATTENTE...");
        fr.put("game.waiting.msg", "En attente des autres joueurs...");
        fr.put("game.calculating", "Calcul des résultats en cours...");
        fr.put("game.language", "Langue");
        fr.put("game.language.fr", "Français");
        fr.put("game.language.en", "English");
        fr.put("category.manage", "Gestion des Catégories");
        fr.put("category.new", "Nouvelle catégorie...");
        fr.put("category.add", "Ajouter");
        fr.put("category.back", "Retour Menu");
        fr.put("settings.title", "PARAMÈTRES");
        fr.put("settings.time", "Durée d'une manche");
        fr.put("settings.language", "Langue du jeu");
        fr.put("settings.language.desc", "Choisissez la langue pour la validation des mots:");
        fr.put("settings.save", "Sauvegarder");
        fr.put("settings.back", "Retour");
        fr.put("lobby.title", "MULTIJOUEUR");
        fr.put("lobby.create", "CRÉER UNE PARTIE");
        fr.put("lobby.create.host", "(Vous serez l'hôte)");
        fr.put("lobby.duration", "Durée de la manche:");
        fr.put("lobby.language", "Langue du jeu:");
        fr.put("lobby.create.server", "Créer le Serveur");
        fr.put("lobby.join", "REJOINDRE");
        fr.put("lobby.join.code", "(Entrez le code de l'hôte)");
        fr.put("lobby.join.button", "Rejoindre");
        fr.put("lobby.players", "JOUEURS CONNECTÉS");
        fr.put("lobby.start", "LANCER LA PARTIE");
        fr.put("lobby.back", "Retour");
        fr.put("player.left", "Joueur {0} a quitté la partie");
        fr.put("player.all.left", "Tous les joueurs sont partis.");
        fr.put("game.ended.solo", "Partie terminée : Le multijoueur nécessite au moins 2 joueurs.");
        
        // English translations
        Map<String, String> en = new HashMap<>();
        en.put("game.title", "BacLegend");
        en.put("game.subtitle", "The ultimate Scattergories game");
        en.put("game.pseudo", "Your Username:");
        en.put("game.pseudo.placeholder", "Enter your name");
        en.put("game.solo", "Solo Mode");
        en.put("game.multiplayer", "Multiplayer");
        en.put("game.categories", "Manage Categories");
        en.put("game.settings", "Settings");
        en.put("game.quit", "Quit");
        en.put("game.letter", "Letter");
        en.put("game.time", "Time Remaining");
        en.put("game.score", "Score");
        en.put("game.validate", "VALIDATE !");
        en.put("game.results", "ROUND RESULTS");
        en.put("game.nextRound", "Next Round / Play Again");
        en.put("game.waiting", "WAITING...");
        en.put("game.waiting.msg", "Waiting for other players...");
        en.put("game.calculating", "Calculating results...");
        en.put("game.language", "Language");
        en.put("game.language.fr", "Français");
        en.put("game.language.en", "English");
        en.put("category.manage", "Category Management");
        en.put("category.new", "New category...");
        en.put("category.add", "Add");
        en.put("category.back", "Back to Menu");
        en.put("settings.title", "SETTINGS");
        en.put("settings.time", "Round Duration");
        en.put("settings.language", "Game Language");
        en.put("settings.language.desc", "Choose the language for word validation:");
        en.put("settings.save", "Save");
        en.put("settings.back", "Back");
        en.put("lobby.title", "MULTIPLAYER");
        en.put("lobby.create", "CREATE GAME");
        en.put("lobby.create.host", "(You will be the host)");
        en.put("lobby.duration", "Round Duration:");
        en.put("lobby.language", "Game Language:");
        en.put("lobby.create.server", "Create Server");
        en.put("lobby.join", "JOIN");
        en.put("lobby.join.code", "(Enter the host's code)");
        en.put("lobby.join.button", "Join");
        en.put("lobby.players", "CONNECTED PLAYERS");
        en.put("lobby.start", "START GAME");
        en.put("lobby.back", "Back");
        en.put("player.left", "Player {0} has left the game");
        en.put("player.all.left", "All players have left.");
        en.put("game.ended.solo", "Game ended: Multiplayer requires at least 2 players.");
        
        translations.put("FR", fr);
        translations.put("EN", en);
    }
    
    // Category translations
    private static final Map<String, Map<String, String>> categoryTranslations = new HashMap<>();
    
    static {
        Map<String, String> frCategories = new HashMap<>();
        frCategories.put("Pays", "Pays");
        frCategories.put("Ville", "Ville");
        frCategories.put("Animal", "Animal");
        frCategories.put("Plante", "Plante");
        frCategories.put("Métier", "Métier");
        frCategories.put("Nom fille", "Nom fille");
        frCategories.put("Nom garçon", "Nom garçon");
        
        Map<String, String> enCategories = new HashMap<>();
        enCategories.put("Pays", "Country");
        enCategories.put("Ville", "City");
        enCategories.put("Animal", "Animal");
        enCategories.put("Plante", "Plant");
        enCategories.put("Métier", "Profession");
        enCategories.put("Nom fille", "Girl's Name");
        enCategories.put("Nom garçon", "Boy's Name");
        
        categoryTranslations.put("FR", frCategories);
        categoryTranslations.put("EN", enCategories);
    }
    
    public static String translate(String key) {
        String lang = App.gameLanguage;
        Map<String, String> langMap = translations.get(lang);
        if (langMap != null && langMap.containsKey(key)) {
            return langMap.get(key);
        }
        // Fallback to French if not found
        Map<String, String> frMap = translations.get("FR");
        return frMap != null && frMap.containsKey(key) ? frMap.get(key) : key;
    }
    
    public static String translate(String key, String... args) {
        String text = translate(key);
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", args[i]);
        }
        return text;
    }
    
    public static String translateCategory(String categoryName) {
        String lang = App.gameLanguage;
        Map<String, String> langMap = categoryTranslations.get(lang);
        if (langMap != null && langMap.containsKey(categoryName)) {
            return langMap.get(categoryName);
        }
        // If category not in translation map, return original
        return categoryName;
    }
    
    public static String getLanguageDisplayName() {
        return App.gameLanguage.equals("FR") ? "Français" : "English";
    }
}

