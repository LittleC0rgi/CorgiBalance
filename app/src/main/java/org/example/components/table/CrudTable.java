package org.example.components.table;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Setter;
import org.example.components.inputs.CrudFormDialog;
import org.example.models.BaseModel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CrudTable<T extends BaseModel> extends VBox {

    private final String title;
    private final List<ColumnSpec<T>> columns;
    private final CrudRepository<T> repository;
    private final Supplier<List<T>> dataLoader;
    private final Supplier<T> newInstance;
    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final TableView<T> table = new TableView<>();
    private final HBox toolbar;
    @Setter
    private Consumer<T> afterCreate;
    @Setter
    private Runnable onDataChanged;

    public CrudTable(String title,
                     CrudRepository<T> repository,
                     Supplier<T> newInstance,
                     List<ColumnSpec<T>> columns) {
        this(title, repository, repository::findAll, newInstance, columns);
    }

    public CrudTable(String title,
                     CrudRepository<T> repository,
                     Supplier<List<T>> dataLoader,
                     Supplier<T> newInstance,
                     List<ColumnSpec<T>> columns) {
        this.title = title;
        this.repository = repository;
        this.dataLoader = dataLoader;
        this.newInstance = newInstance;
        this.columns = columns;

        getStyleClass().add("crud-table");
        setSpacing(12);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("crud-table-title");

        Button addButton = new Button("Add");
        addButton.getStyleClass().add("crud-btn");
        addButton.setOnAction(event -> showAddDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(titleLabel, spacer, addButton);
        toolbar.getStyleClass().add("crud-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        this.toolbar = toolbar;

        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setItems(items);
        VBox.setVgrow(table, Priority.ALWAYS);

        columns.forEach(this::addColumn);

        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                T row = table.getSelectionModel().getSelectedItem();
                if (row != null) {
                    confirmDelete(row);
                }
            }
        });

        getChildren().addAll(toolbar, table);
        refresh();
    }

    public void refresh() {
        items.setAll(dataLoader.get());
    }

    public void addToolbarButton(String text, EventHandler<ActionEvent> handler) {
        Button button = new Button(text);
        button.getStyleClass().add("crud-btn");
        button.setOnAction(handler);
        toolbar.getChildren().add(toolbar.getChildren().size() - 1, button);
    }

    public void addToolbarNode(Node node) {
        toolbar.getChildren().add(toolbar.getChildren().size() - 1, node);
    }

    private void addColumn(ColumnSpec<T> spec) {
        TableColumn<T, Object> column = new TableColumn<>(spec.title());
        column.setPrefWidth(spec.width());
        column.setEditable(spec.editable());
        column.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(spec.value().apply(data.getValue())));
        if (spec.cellFactory() != null) {
            column.setCellFactory(spec.cellFactory());
        }
        if (spec.editable()) {
            column.setOnEditCommit(
                    event -> handleCommit(event.getRowValue(), event.getNewValue(), spec));
        }
        table.getColumns().add(column);
    }

    private void handleCommit(T row, Object newValue, ColumnSpec<T> spec) {
        Object oldValue = spec.value().apply(row);
        if (Objects.equals(oldValue, newValue)) {
            table.refresh();
            return;
        }
        if (spec.onCommit() != null) {
            spec.onCommit().accept(row, newValue);
        }
        persist(row);
    }

    private void persist(T row) {
        try {
            if (row.getId() == null) {
                repository.create(row);
            } else {
                repository.update(row);
            }
            refresh();
            notifyDataChanged();
        } catch (RuntimeException e) {
            showError("Failed to save row", e.getMessage());
            refresh();
        }
    }

    private void showAddDialog() {
        CrudFormDialog<T> dialog = new CrudFormDialog<>(title, columns, newInstance);
        Optional<T> result = dialog.showAndWait();
        result.ifPresent(value -> {
            try {
                repository.create(value);
                if (afterCreate != null) {
                    afterCreate.accept(value);
                }
                refresh();
                notifyDataChanged();
            } catch (RuntimeException e) {
                showError("Failed to create " + title.toLowerCase(), e.getMessage());
            }
        });
    }

    private void confirmDelete(T row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this row?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                repository.delete(row);
                refresh();
                notifyDataChanged();
            } catch (RuntimeException e) {
                showError("Failed to delete row", e.getMessage());
            }
        }
    }

    private void notifyDataChanged() {
        if (onDataChanged != null) {
            onDataChanged.run();
        }
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
