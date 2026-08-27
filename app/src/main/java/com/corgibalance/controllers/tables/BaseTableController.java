package com.corgibalance.controllers.tables;

import com.corgibalance.models.BaseModel;
import com.corgibalance.repositories.CrudRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseTableController<T extends BaseModel, R extends CrudRepository<T>> {

    protected final R repository;
    @FXML
    protected TableView<T> table;

    @FXML
    public void initialize() {
        configureTable();
        configureColumns();
        loadData();
    }

    private void configureTable() {
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                deleteSelected();
            }
        });
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(_ -> {
                T item = row.getItem();
                if (item != null && !isPlaceholder(item)) {
                    deleteWithConfirmation(item);
                }
            });
            ContextMenu menu = new ContextMenu(deleteItem);
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(menu));
            return row;
        });
    }

    protected abstract void configureColumns();

    protected void loadData() {
        ObservableList<T> items = FXCollections.observableArrayList(repository.findAll());
        items.add(newPlaceholder());
        setItems(items);
    }

    protected abstract T newPlaceholder();

    protected abstract String deleteConfirmationText(T item);

    protected boolean isPlaceholder(T item) {
        return item.getId() == null;
    }

    protected void commit(T item, Consumer<T> apply, boolean createOnPlaceholder) {
        apply.accept(item);
        if (isPlaceholder(item)) {
            if (createOnPlaceholder) {
                try {
                    repository.create(item);
                } catch (RuntimeException e) {
                    showError(e);
                    return;
                }
                table.getItems().add(newPlaceholder());
            }
            table.refresh();
        } else {
            try {
                repository.update(item);
            } catch (RuntimeException e) {
                showError(e);
            }
            table.refresh();
        }
    }

    protected void showError(RuntimeException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(e.getMessage());
        alert.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm());
        alert.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().addAll("btn", "btn--primary");
        alert.showAndWait();
    }

    protected void refresh() {
        table.refresh();
    }

    private void deleteSelected() {
        T selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || isPlaceholder(selected)) {
            return;
        }
        deleteWithConfirmation(selected);
    }

    private void deleteWithConfirmation(T item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText(deleteConfirmationText(item));
        confirm.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm());
        confirm.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().addAll("btn", "btn--danger");
        confirm.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass().add("btn");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            repository.delete(item);
            table.getItems().remove(item);
            onItemDeleted(item);
        }
    }

    protected void onItemDeleted(T item) {
    }

    public void setItems(ObservableList<T> items) {
        table.setItems(items);
    }
}
