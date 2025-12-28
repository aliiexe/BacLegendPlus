package com.emsi.baclegend.model;

public class Categorie {
    private int id;
    private String nom;
    private boolean estActive;

    public Categorie(int id, String nom, boolean estActive) {
        this.id = id;
        this.nom = nom;
        this.estActive = estActive;
    }

    public Categorie(String nom) {
        this.nom = nom;
        this.estActive = true;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public boolean isEstActive() {
        return estActive;
    }

    public void setEstActive(boolean estActive) {
        this.estActive = estActive;
    }

    @Override
    public String toString() {
        return nom;
    }
}
