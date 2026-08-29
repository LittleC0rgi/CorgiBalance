package com.corgibalance.components;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.util.Objects;

public final class Dialogs {

    private Dialogs() {
    }

    public static void showError(RuntimeException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(e.getMessage());
        style(alert, "btn--primary");
        alert.showAndWait();
    }

    public static void style(Dialog<?> dialog, String okStyleClass) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(
                Objects.requireNonNull(Dialogs.class.getResource("/css/base.css")).toExternalForm());
        Node ok = pane.lookupButton(ButtonType.OK);
        if (ok != null) {
            ok.getStyleClass().addAll("btn", okStyleClass);
        }
        Node cancel = pane.lookupButton(ButtonType.CANCEL);
        if (cancel != null) {
            cancel.getStyleClass().add("btn");
        }
    }

    public static void runSafely(Runnable action, Runnable onChanged) {
        try {
            action.run();
            onChanged.run();
        } catch (RuntimeException e) {
            showError(e);
        }
    }
}
