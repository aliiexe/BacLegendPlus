package com.emsi.baclegend.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ScoreDAO {
    public Integer getBestScore(String pseudo) {
        String sql = "SELECT MAX(score) AS best FROM scores WHERE LOWER(pseudo)=LOWER(?)";
        try (Connection c = GestionnaireBaseDeDonnees.getConnexion();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, pseudo);
            ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getInt("best");
        } catch (SQLException e) {
            System.err.println("Best score error: " + e.getMessage());
        }
        return null;
    }

    public void saveScore(String pseudo, int score) {
        String sql = "INSERT INTO scores(pseudo,score,date_utc) VALUES(?,?,?)";
        try (Connection c = GestionnaireBaseDeDonnees.getConnexion();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, pseudo);
            p.setInt(2, score);
            p.setLong(3, System.currentTimeMillis());
            p.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Save score error: " + e.getMessage());
        }
    }
}