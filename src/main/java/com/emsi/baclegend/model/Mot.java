package com.emsi.baclegend.model;

public class Mot {
    private int id;
    private String contenu;
    private int categorieId;
    private boolean estValide;

    public Mot(int id, String contenu, int categorieId, boolean estValide) {
        this.id = id;
        this.contenu = contenu;
        this.categorieId = categorieId;
        this.estValide = estValide;
    }

    public Mot(String contenu, int categorieId) {
        this.contenu = contenu;
        this.categorieId = categorieId;
        this.estValide = true; // Par défaut, si on l'ajoute, on le considère valide/connu
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public int getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(int categorieId) {
        this.categorieId = categorieId;
    }

    public boolean isEstValide() {
        return estValide;
    }

    public void setEstValide(boolean estValide) {
        this.estValide = estValide;
    }
}
