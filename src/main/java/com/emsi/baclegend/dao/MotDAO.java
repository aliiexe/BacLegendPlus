package com.emsi.baclegend.dao;

import com.emsi.baclegend.model.Mot;
import java.sql.*;

public class MotDAO {

    public Mot trouverParContenu(String contenu, int categorieId) {
        String sql = "SELECT * FROM mots WHERE LOWER(TRIM(contenu)) = LOWER(TRIM(?)) AND categorie_id = ?";

        try (Connection conn = GestionnaireBaseDeDonnees.getConnexion();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String normalized = contenu.trim().toLowerCase();
            pstmt.setString(1, normalized);
            pstmt.setInt(2, categorieId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Mot found = new Mot(
                        rs.getInt("id"),
                        rs.getString("contenu"),
                        rs.getInt("categorie_id"),
                        rs.getInt("est_valide") == 1);
                System.out.println("DATABASE: Found word='" + found.getContenu() + "', valid=" + found.isEstValide());
                return found;
            } else {
                System.out.println("DATABASE: Word not found: '" + normalized + "' for category_id=" + categorieId);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche du mot : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void sauvegarder(Mot mot) {
        String sql = "INSERT INTO mots(contenu, categorie_id, est_valide) VALUES(?, ?, ?)";

        try (Connection conn = GestionnaireBaseDeDonnees.getConnexion();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, mot.getContenu().toLowerCase().trim());
            pstmt.setInt(2, mot.getCategorieId());
            pstmt.setInt(3, mot.isEstValide() ? 1 : 0);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erreur lors de la sauvegarde du mot : " + e.getMessage());
        }
    }
}
