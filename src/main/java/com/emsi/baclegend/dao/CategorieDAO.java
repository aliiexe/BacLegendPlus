package com.emsi.baclegend.dao;

import com.emsi.baclegend.model.Categorie;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieDAO {

    public List<Categorie> obtenirToutes() {
        List<Categorie> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE est_active = 1";

        try (Connection conn = GestionnaireBaseDeDonnees.getConnexion();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(new Categorie(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getInt("est_active") == 1));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des catégories : " + e.getMessage());
        }
        return categories;
    }

    public void ajouter(Categorie categorie) {
        String sql = "INSERT INTO categories(nom, est_active) VALUES(?, ?)";

        try (Connection conn = GestionnaireBaseDeDonnees.getConnexion();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, categorie.getNom());
            pstmt.setInt(2, categorie.isEstActive() ? 1 : 0);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de la catégorie : " + e.getMessage());
        }
    }
}
