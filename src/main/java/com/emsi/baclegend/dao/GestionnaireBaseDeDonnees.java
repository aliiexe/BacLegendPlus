package com.emsi.baclegend.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class GestionnaireBaseDeDonnees {

    private static final String URL = "jdbc:sqlite:baclezend.db";
    private static Connection connexion = null;

    public static Connection getConnexion() {
        try {
            if (connexion == null || connexion.isClosed()) {
                connexion = DriverManager.getConnection(URL);
                initialiserTables();
            }
        } catch (SQLException e) {
            System.err.println("Erreur de connexion à la base de données : " + e.getMessage());
        }
        return connexion;
    }

    private static void initialiserTables() {
        String sqlCategories = "CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, nom TEXT NOT NULL UNIQUE, est_active INTEGER DEFAULT 1)";
        String sqlMots = "CREATE TABLE IF NOT EXISTS mots (id INTEGER PRIMARY KEY AUTOINCREMENT, contenu TEXT NOT NULL, categorie_id INTEGER, est_valide INTEGER DEFAULT 1, FOREIGN KEY(categorie_id) REFERENCES categories(id))";
        String sqlScores = "CREATE TABLE IF NOT EXISTS scores (id INTEGER PRIMARY KEY AUTOINCREMENT, pseudo TEXT NOT NULL, score INTEGER NOT NULL, date_utc INTEGER NOT NULL)";
        try (Statement stmt = connexion.createStatement()) {
            stmt.execute(sqlCategories);
            stmt.execute(sqlMots);
            stmt.execute(sqlScores);
            stmt.execute("INSERT OR IGNORE INTO categories (nom) VALUES ('Pays')");
            stmt.execute("INSERT OR IGNORE INTO categories (nom) VALUES ('Ville')");
            stmt.execute("INSERT OR IGNORE INTO categories (nom) VALUES ('Animal')");
            stmt.execute("INSERT OR IGNORE INTO categories (nom) VALUES ('Plante')");
            stmt.execute("INSERT OR IGNORE INTO categories (nom) VALUES ('Métier')");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'initialisation des tables : " + e.getMessage());
        }
    }

    public static void fermerConnexion() {
        try {
            if (connexion != null && !connexion.isClosed()) {
                connexion.close();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la fermeture de la connexion : " + e.getMessage());
        }
    }
}
