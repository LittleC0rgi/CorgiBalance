package com.corgibalance.components.views;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import com.corgibalance.services.Database;

import java.io.File;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class SettingsView extends View {

    public SettingsView() {
        super("Settings", "/fxml/views/Settings.fxml");
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
        style(confirm);
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

    public void connectDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Connect to database");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite database", "*.db"));
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("The application will use this database as its primary one: "
                + file.getAbsolutePath() + ". Continue?");
        style(confirm);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            Database.getInstance().connectTo(file.toPath());
            showInfo("Database connected",
                    "Connected to " + file.getAbsolutePath()
                            + ". Restart the application to fully refresh all views.");
        } catch (SQLException e) {
            showError("Failed to connect database", e.getMessage());
        }
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(header);
        alert.setContentText(message);
        style(alert);
        alert.showAndWait();
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(message);
        style(alert);
        alert.showAndWait();
    }

    private void style(Alert alert) {
        alert.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm());
        for (ButtonType type : alert.getButtonTypes()) {
            Button button = (Button) alert.getDialogPane().lookupButton(type);
            button.getStyleClass().add("btn");
            if (type == ButtonType.OK || type == ButtonType.YES) {
                button.getStyleClass().add("btn--primary");
            }
        }
    }
}
