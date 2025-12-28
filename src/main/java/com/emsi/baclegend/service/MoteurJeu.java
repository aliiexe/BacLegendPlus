package com.emsi.baclegend.service;

import com.emsi.baclegend.dao.CategorieDAO;
import com.emsi.baclegend.model.Categorie;
import com.emsi.baclegend.model.SessionJeu;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoteurJeu {

    private SessionJeu sessionCourante;
    private ServiceValidation serviceValidation;
    private CategorieDAO categorieDAO;
    private int score;
    private int scoreAdversaire;
    private boolean gameStopped; // Added variable to track if the game is stopped

    public MoteurJeu() {
        this.serviceValidation = new ServiceValidation();
        this.categorieDAO = new CategorieDAO();
        this.score = 0;
        this.scoreAdversaire = 0;
        this.gameStopped = false; // Initialize as false
    }

    public void demarrerNouvellePartie() {
        List<Categorie> categories = categorieDAO.obtenirToutes();
        this.sessionCourante = new SessionJeu(categories);
        this.sessionCourante.demarrerPartie();
        this.score = 0;
        this.scoreAdversaire = 0;
        this.gameStopped = false; // Reset gameStopped when starting a new game
    }

    public Map<String, Boolean> soumettreReponses(Map<Categorie, String> reponses) {
        Map<String, Boolean> resultats = new HashMap<>();
        if (sessionCourante == null) {
            throw new IllegalStateException("Session inexistante !");
        }
        // Proceed even if not en cours; timing can race with STOP.
        char lettre = sessionCourante.getLettreCourante();

        for (Map.Entry<Categorie, String> entry : reponses.entrySet()) {
            Categorie cat = entry.getKey();
            String mot = entry.getValue() != null ? entry.getValue().trim() : "";
            boolean estValide = false;
            if (!mot.isEmpty() && Character.toUpperCase(mot.charAt(0)) == Character.toUpperCase(lettre)) {
                if (serviceValidation.validerMot(mot, cat)) {
                    estValide = true;
                    this.score += 10;
                }
            }
            resultats.put(cat.getNom(), estValide);
        }
        sessionCourante.arreterPartie();
        return resultats;
    }

    public void ajusterScore(int delta) {
        this.score += delta;
        if (this.score < 0) this.score = 0;
    }

    public SessionJeu getSessionCourante() { return sessionCourante; }
    public int getScore() { return score; }
    public int getScoreAdversaire() { return scoreAdversaire; }
    public void setScoreAdversaire(int score) { this.scoreAdversaire = score; }
}
