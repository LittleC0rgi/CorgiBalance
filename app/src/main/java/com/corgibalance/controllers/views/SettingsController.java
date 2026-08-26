package com.corgibalance.controllers.views;

import com.corgibalance.App;
import com.corgibalance.repositories.SettingsRepository;
import com.corgibalance.services.Database;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class SettingsController {

    private static final String SHOW_EXPENSES_BY_TAG_KEY = "overview.showExpensesByTag";

    @FXML
    private VBox root;
    @FXML
    private CheckBox showExpensesByTagCheckBox;

    private SettingsRepository settingsRepository;

    @FXML
    private void initialize() {
        settingsRepository = new SettingsRepository();
        showExpensesByTagCheckBox.setSelected(isShowExpensesByTag());
    }

    private boolean isShowExpensesByTag() {
        return settingsRepository.get(SHOW_EXPENSES_BY_TAG_KEY)
                .map(Boolean::parseBoolean)
                .orElse(true);
    }

    public void toggleShowExpensesByTag() {
        settingsRepository.set(SHOW_EXPENSES_BY_TAG_KEY,
                String.valueOf(showExpensesByTagCheckBox.isSelected()));
    }

    public void exportDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save database");
        chooser.setInitialFileName("corgibalance.db");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite database", "*.db"));
        File file = chooser.showSaveDialog(root.getScene().getWindow());
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
        File file = chooser.showOpenDialog(root.getScene().getWindow());
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
            showInfo("Database imported", "Database imported from " + file.getAbsolutePath()
                    + ". The application will restart now.");
            App.restart();
        } catch (SQLException e) {
            showError("Failed to import database", e.getMessage());
        }
    }

    public void connectDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Connect to database");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite database", "*.db"));
        File file = chooser.showOpenDialog(root.getScene().getWindow());
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
            showInfo("Database connected", "Connected to " + file.getAbsolutePath()
                    + ". The application will restart now.");
            App.restart();
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
