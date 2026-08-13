package com.corgibalance.components.views;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.corgibalance.services.Database;

import java.io.File;
import java.sql.SQLException;
import java.util.Optional;

public class SettingsView extends View {

    @FXML
    private VBox sections;

    public SettingsView() {
        super("Settings", "/fxml/views/settings.fxml");
    }

    @FXML
    private void initialize() {
        sections.getChildren().add(createDatabaseSection());
    }

    private VBox createDatabaseSection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("settings-section");

        Label title = new Label("Database");
        title.getStyleClass().add("settings-section-title");

        Label hint = new Label("Save a copy of your data or restore it from a backup.");
        hint.getStyleClass().add("crud-balance-label");

        Button saveButton = new Button("Save database");
        saveButton.getStyleClass().add("crud-btn");
        saveButton.setOnAction(event -> exportDatabase());

        Button importButton = new Button("Import database");
        importButton.getStyleClass().add("db-import-btn");
        importButton.setOnAction(event -> importDatabase());

        HBox buttons = new HBox(12, saveButton, importButton);

        section.getChildren().addAll(title, hint, buttons);
        return section;
    }

    private void exportDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save database");
        chooser.setInitialFileName("corgibalance.db");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite database", "*.db"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            Database.getInstance().exportTo(file.toPath());
            showInfo("Database saved", "Database saved to " + file.getAbsolutePath());
        } catch (SQLException e) {
            showError("Failed to save database", e.getMessage());
        }
    }

    private void importDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import database");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite database", "*.db"));
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("Importing will replace all current data with the selected database. Continue?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            Database.getInstance().importFrom(file.toPath());
            showInfo("Database imported",
                    "Database imported from " + file.getAbsolutePath()
                            + ". Restart the application to fully refresh all views.");
        } catch (SQLException e) {
            showError("Failed to import database", e.getMessage());
        }
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
