package com.emsi.baclegend.model;

public class ReponseJoueur {
    private int points;
    // La réponse est liée à une catégorie, un mot et un joueur
    private String motSaisi;
    private boolean estValide;

    public ReponseJoueur(String motSaisi) {
        this.motSaisi = motSaisi;
        this.points = 0;
        this.estValide = false;
    }

    // Méthode du diagramme
    public boolean validerReponse() {
        // La logique réelle est souvent déléguée au ServiceValidation,
        // mais cette méthode peut retourner l'état actuel.
        return estValide;
    }

    public void setValide(boolean valide) {
        this.estValide = valide;
        // 10 points si valide, 0 sinon (règle par défaut)
        this.points = valide ? 10 : 0;
    }

    public int getPoints() {
        return points;
    }

    public String getMotSaisi() {
        return motSaisi;
    }
}
