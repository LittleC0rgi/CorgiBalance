package org.example.components.table;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.example.models.BaseModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class CrudTable<T extends BaseModel> extends VBox {

    private final String title;
    private final List<ColumnSpec<T>> columns;
    private final CrudRepository<T> repository;
    private final Supplier<T> newInstance;
    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final TableView<T> table = new TableView<>();

    public CrudTable(String title, CrudRepository<T> repository, Supplier<T> newInstance,
                     List<ColumnSpec<T>> columns) {
        this.title = title;
        this.repository = repository;
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

        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setItems(items);
        VBox.setVgrow(table, Priority.ALWAYS);

        columns.forEach(this::addColumn);
        table.getColumns().add(createDeleteColumn());

        getChildren().addAll(toolbar, table);
        refresh();
    }

    public void refresh() {
        items.setAll(repository.findAll());
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
        } catch (RuntimeException e) {
            showError("Failed to save row", e.getMessage());
            refresh();
        }
    }

    private void showAddDialog() {
        T entity = newInstance.get();
        List<FieldEntry<T>> fields = new ArrayList<>();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setPrefWidth(360);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setPrefWidth(120);
        ColumnConstraints controlColumn = new ColumnConstraints();
        controlColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, controlColumn);

        int row = 0;
        for (ColumnSpec<T> spec : columns) {
            FormSpec form = spec.formSpec();
            if (form == null) {
                continue;
            }
            Control control = createControl(form);
            grid.add(buildLabel(spec), 0, row);
            grid.add(control, 1, row);
            fields.add(new FieldEntry<>(spec, control));
            row++;
        }

        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle("Add " + title);
        dialog.setHeaderText("Add " + title);

        dialog.getDialogPane().getStylesheets()
                .add(Objects.requireNonNull(
                        CrudTable.class.getResource("/css/table.css")).toExternalForm());
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("crud-btn");
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            List<String> errors = validate(fields);
            if (!errors.isEmpty()) {
                event.consume();
                showError("Invalid input", String.join("\n", errors));
                return;
            }
            try {
                applyValues(entity, fields);
            } catch (RuntimeException e) {
                event.consume();
                showError("Invalid input", e.getMessage());
            }
        });

        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? entity : null);

        Optional<T> result = dialog.showAndWait();
        result.ifPresent(value -> {
            try {
                repository.create(value);
                refresh();
            } catch (RuntimeException e) {
                showError("Failed to create " + title.toLowerCase(), e.getMessage());
            }
        });
    }

    private Node buildLabel(ColumnSpec<T> spec) {
        Label label = new Label(spec.title());
        label.getStyleClass().add("crud-form-label");
        if (!spec.required()) {
            return label;
        }
        Label star = new Label("*");
        star.getStyleClass().add("crud-form-required");
        HBox box = new HBox(3, label, star);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Control createControl(FormSpec form) {
        switch (form.kind()) {
            case TEXT:
            case COLOR:
            case NUMBER: {
                TextField textField = new TextField();
                textField.setMaxWidth(Double.MAX_VALUE);
                return textField;
            }
            case DATE: {
                DatePicker datePicker = new DatePicker();
                datePicker.setMaxWidth(Double.MAX_VALUE);
                return datePicker;
            }
            case ENUM:
            case COMBO: {
                ComboBox<Object> comboBox = new ComboBox<>();
                comboBox.getItems().setAll(form.options());
                comboBox.setMaxWidth(Double.MAX_VALUE);
                Callback<ListView<Object>, ListCell<Object>> cellFactory =
                        list -> new ListCell<>() {
                            @Override
                            protected void updateItem(Object item, boolean empty) {
                                super.updateItem(item, empty);
                                setText(empty || item == null ? "" : form.labeler().apply(item));
                            }
                        };
                comboBox.setCellFactory(cellFactory);
                comboBox.setButtonCell(cellFactory.call(null));
                return comboBox;
            }
            case BOOLEAN: {
                CheckBox checkBox = new CheckBox();
                return checkBox;
            }
            default:
                throw new IllegalStateException("Unsupported form kind: " + form.kind());
        }
    }

    private List<String> validate(List<FieldEntry<T>> fields) {
        List<String> errors = new ArrayList<>();
        for (FieldEntry<T> field : fields) {
            ColumnSpec<T> spec = field.spec();
            if (!spec.required()) {
                continue;
            }
            Object value = read(field.control(), spec.formSpec().kind());
            if (value == null) {
                if (spec.formSpec().kind() == FormSpec.Kind.NUMBER) {
                    errors.add(spec.title() + " must be a valid number");
                } else {
                    errors.add(spec.title() + " is required");
                }
            }
        }
        return errors;
    }

    private void applyValues(T entity, List<FieldEntry<T>> fields) {
        for (FieldEntry<T> field : fields) {
            Object value = read(field.control(), field.spec().formSpec().kind());
            if (value == null || field.spec().onCommit() == null) {
                continue;
            }
            field.spec().onCommit().accept(entity, value);
        }
    }

    private Object read(Control control, FormSpec.Kind kind) {
        switch (kind) {
            case TEXT:
            case COLOR: {
                String text = ((TextField) control).getText();
                text = text == null ? "" : text.trim();
                return text.isEmpty() ? null : text;
            }
            case NUMBER: {
                String text = ((TextField) control).getText();
                text = text == null ? "" : text.trim();
                if (text.isEmpty()) {
                    return null;
                }
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            case DATE:
                return ((DatePicker) control).getValue();
            case ENUM:
            case COMBO:
                return ((ComboBox<?>) control).getValue();
            case BOOLEAN:
                return ((CheckBox) control).isSelected();
            default:
                throw new IllegalStateException("Unsupported form kind: " + kind);
        }
    }

    private TableColumn<T, Object> createDeleteColumn() {
        TableColumn<T, Object> column = new TableColumn<>("");
        column.setPrefWidth(90);
        column.setSortable(false);
        column.setEditable(false);
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        column.setCellFactory(col -> new DeleteCell<T>(this));
        return column;
    }

    private void confirmDelete(T row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this row?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                repository.delete(row);
                items.remove(row);
            } catch (RuntimeException e) {
                showError("Failed to delete row", e.getMessage());
            }
        }
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record FieldEntry<S>(ColumnSpec<S> spec, Control control) {
    }

    private static final class DeleteCell<T extends BaseModel> extends TableCell<T, Object> {

        private final Button button = new Button("Delete");

        @SuppressWarnings("unchecked")
        DeleteCell(CrudTable<T> table) {
            button.getStyleClass().add("crud-delete-btn");
            button.setOnAction(event -> {
                T row = (T) getItem();
                if (row != null) {
                    table.confirmDelete(row);
                }
            });
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                setGraphic(button);
                setText(null);
            }
        }
    }
}
