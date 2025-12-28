package com.emsi.baclegend.model;

public class Joueur {
    private String pseudo;
    private int scorePartie;
    private String adresseIp;

    public Joueur(String pseudo) {
        this.pseudo = pseudo;
        this.scorePartie = 0;
        this.adresseIp = "127.0.0.1"; // Default or detected
    }

    public void mettreAJourScore(int points) {
        this.scorePartie += points;
    }

    // Getters & Setters
    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public int getScorePartie() {
        return scorePartie;
    }

    public String getAdresseIp() {
        return adresseIp;
    }

    public void setAdresseIp(String adresseIp) {
        this.adresseIp = adresseIp;
    }
}
