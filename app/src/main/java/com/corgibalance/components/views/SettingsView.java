package com.corgibalance.components.views;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import com.corgibalance.services.Database;

import java.io.File;
import java.sql.SQLException;
import java.util.Optional;

public class SettingsView extends View {

    public SettingsView() {
        super("Settings", "/fxml/views/settings.fxml");
    }

    public void exportDatabase() {
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

    public void importDatabase() {
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
