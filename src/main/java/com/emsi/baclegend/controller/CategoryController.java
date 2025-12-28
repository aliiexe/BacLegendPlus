package com.emsi.baclegend.controller;

import com.emsi.baclegend.App;
import com.emsi.baclegend.dao.CategorieDAO;
import com.emsi.baclegend.model.Categorie;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import java.io.IOException;

public class CategoryController {

    @FXML
    private ListView<Categorie> listCategories;
    @FXML
    private TextField txtNouvelleCategorie;

    private CategorieDAO categorieDAO;
    private ObservableList<Categorie> categoriesList;

    @FXML
    public void initialize() {
        categorieDAO = new CategorieDAO();
        loadCategories();
    }

    private void loadCategories() {
        categoriesList = FXCollections.observableArrayList(categorieDAO.obtenirToutes());
        listCategories.setItems(categoriesList);
    }

    @FXML
    private void handleAjouter() {
        String nom = txtNouvelleCategorie.getText();
        if (nom != null && !nom.trim().isEmpty()) {
            Categorie nouvelle = new Categorie(nom.trim());
            categorieDAO.ajouter(nouvelle);
            txtNouvelleCategorie.clear();
            loadCategories(); // Recharger la liste
        }
    }

    @FXML
    private void handleRetour() throws IOException {
        App.setRoot("view/main");
    }
}
