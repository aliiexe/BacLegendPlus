package com.emsi.baclegend.model;

import java.util.List;
import java.util.Random;

public class SessionJeu {
    private String idSession;
    private char lettreCourante;
    private boolean enCours;
    private int tempsLimite; // En secondes, ex: 60
    private List<Categorie> categories;

    public SessionJeu(List<Categorie> categories) {
        this.categories = categories;
        this.enCours = false;
        this.idSession = java.util.UUID.randomUUID().toString();
        this.tempsLimite = 60; // Valeur par défaut
    }

    public SessionJeu(List<Categorie> categories, int tempsLimite) {
        this.categories = categories;
        this.enCours = false;
        this.idSession = java.util.UUID.randomUUID().toString();
        this.tempsLimite = tempsLimite;
    }

    public void demarrerPartie() {
        this.lettreCourante = genererLettre();
        this.enCours = true;
    }

    public void arreterPartie() {
        this.enCours = false;
    }

    private char genererLettre() {
        Random r = new Random();
        // Génère une lettre majuscule entre A et Z
        return (char) (r.nextInt(26) + 'A');
    }

    public void setLettreCourante(char lettre) {
        this.lettreCourante = lettre;
    }

    public void setTempsLimite(int tempsLimite) {
        this.tempsLimite = tempsLimite;
    }

    // Getters
    public String getIdSession() {
        return idSession;
    }

    public int getTempsLimite() {
        return tempsLimite;
    }

    public char getLettreCourante() {
        return lettreCourante;
    }

    public boolean isEnCours() {
        return enCours;
    }

    public List<Categorie> getCategories() {
        return categories;
    }
}
