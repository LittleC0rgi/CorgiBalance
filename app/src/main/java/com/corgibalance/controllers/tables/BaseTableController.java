package com.corgibalance.controllers.tables;

import com.corgibalance.models.BaseModel;
import com.corgibalance.repositories.CrudRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
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
                repository.create(item);
                table.getItems().add(newPlaceholder());
            }
            table.refresh();
        } else {
            repository.update(item);
        }
    }

    protected void refresh() {
        table.refresh();
    }

    private void deleteSelected() {
        T selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || isPlaceholder(selected)) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText(deleteConfirmationText(selected));
        confirm.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm());
        ((Button) confirm.getDialogPane().lookupButton(ButtonType.OK)).getStyleClass().addAll("btn", "btn--primary");
        ((Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL)).getStyleClass().add("btn");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            repository.delete(selected);
            table.getItems().remove(selected);
        }
    }

    public void setItems(ObservableList<T> items) {
        table.setItems(items);
    }
}
