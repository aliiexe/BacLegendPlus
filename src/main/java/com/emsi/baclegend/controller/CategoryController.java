package com.emsi.baclegend.controller;

import com.emsi.baclegend.App;
import com.emsi.baclegend.dao.CategorieDAO;
import com.emsi.baclegend.model.Categorie;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import java.io.IOException;
import java.util.Optional;

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
        setupListCellFactory();
    }

    private void loadCategories() {
        categoriesList = FXCollections.observableArrayList(categorieDAO.obtenirToutes());
        listCategories.setItems(categoriesList);
    }

    private void setupListCellFactory() {
        listCategories.setCellFactory(lv -> new ListCell<Categorie>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("✕");
            private final Label lblName = new Label();
            private final HBox hbox = new HBox(12);

            {
                // Style buttons
                btnEdit.getStyleClass().addAll("button", "button-secondary");
                btnEdit.setStyle("-fx-padding: 4 10; -fx-font-size: 12px;");
                btnDelete.getStyleClass().addAll("button", "button-danger");
                btnDelete.setStyle("-fx-padding: 4 10; -fx-font-size: 12px;");

                lblName.getStyleClass().add("label");
                lblName.setStyle("-fx-font-size: 14px;");
                HBox.setHgrow(lblName, Priority.ALWAYS);

                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.getChildren().addAll(lblName, btnEdit, btnDelete);

                btnEdit.setOnAction(e -> {
                    Categorie cat = getItem();
                    if (cat != null)
                        handleModifier(cat);
                });

                btnDelete.setOnAction(e -> {
                    Categorie cat = getItem();
                    if (cat != null)
                        handleSupprimer(cat);
                });
            }

            @Override
            protected void updateItem(Categorie item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    lblName.setText(item.getNom());
                    setGraphic(hbox);
                }
            }
        });
    }

    @FXML
    private void handleAjouter() {
        String nom = txtNouvelleCategorie.getText();
        if (nom != null && !nom.trim().isEmpty()) {
            Categorie nouvelle = new Categorie(nom.trim());
            categorieDAO.ajouter(nouvelle);
            txtNouvelleCategorie.clear();
            loadCategories();
        }
    }

    private void handleModifier(Categorie categorie) {
        TextInputDialog dialog = new TextInputDialog(categorie.getNom());
        dialog.setTitle("Modifier la catégorie");
        dialog.setHeaderText(null);
        dialog.setContentText("Nouveau nom:");

        // Style the dialog
        dialog.getDialogPane().getStylesheets().add(App.class.getResource("style.css").toExternalForm());

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                categorie.setNom(newName.trim());
                categorieDAO.modifier(categorie);
                loadCategories();
            }
        });
    }

    private void handleSupprimer(Categorie categorie) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la catégorie");
        alert.setHeaderText(null);
        alert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + categorie.getNom() + "\" ?");

        // Style the dialog
        alert.getDialogPane().getStylesheets().add(App.class.getResource("style.css").toExternalForm());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            categorieDAO.supprimer(categorie.getId());
            loadCategories();
        }
    }

    @FXML
    private void handleRetour() throws IOException {
        App.setRoot("view/main");
    }
}
