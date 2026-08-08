package org.example.components.inputs;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import org.example.components.table.ColumnSpec;
import org.example.components.table.CrudTable;
import org.example.components.table.FormSpec;
import org.example.models.BaseModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class CrudFormDialog<T extends BaseModel> extends Dialog<T> {

    private final String title;
    private final T entity;
    private final List<FieldEntry<T>> fields = new ArrayList<>();

    public CrudFormDialog(String title, List<ColumnSpec<T>> columns, Supplier<T> newInstance) {
        this.title = title;
        this.entity = newInstance.get();

        setTitle("Add " + title);

        getDialogPane().getStylesheets()
                .add(Objects.requireNonNull(
                        CrudTable.class.getResource("/css/table.css")).toExternalForm());
        getDialogPane().setContent(buildForm(columns));
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("crud-btn");
        Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("crud-cancel-btn");
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            List<String> errors = validate();
            if (!errors.isEmpty()) {
                event.consume();
                showError("Invalid input", String.join("\n", errors));
                return;
            }
            try {
                applyValues();
            } catch (RuntimeException e) {
                event.consume();
                showError("Invalid input", e.getMessage());
            }
        });

        setResultConverter(buttonType -> buttonType == ButtonType.OK ? entity : null);
    }

    private GridPane buildForm(List<ColumnSpec<T>> columns) {
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
            applyDefault(control, spec, form.kind());
            grid.add(buildLabel(spec), 0, row);
            grid.add(control, 1, row);
            fields.add(new FieldEntry<>(spec, control));
            row++;
        }
        return grid;
    }

    private void applyDefault(Control control, ColumnSpec<T> spec, FormSpec.Kind kind) {
        if (spec.defaultValue() == null) {
            return;
        }
        Object value = spec.defaultValue().get();
        if (value == null) {
            return;
        }
        switch (kind) {
            case TEXT:
            case COLOR:
            case NUMBER:
            case DECIMAL:
                ((TextField) control).setText(String.valueOf(value));
                break;
            case DATE:
                ((DatePicker) control).setValue((LocalDate) value);
                break;
            case ENUM:
            case COMBO:
                ((ComboBox<Object>) control).setValue(value);
                break;
            case BOOLEAN:
                ((CheckBox) control).setSelected(Boolean.TRUE.equals(value));
                break;
            default:
                throw new IllegalStateException("Unsupported form kind: " + kind);
        }
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
            case NUMBER:
            case DECIMAL: {
                return new CrudTextField();
            }
            case DATE: {
                return new CrudDatePicker();
            }
            case ENUM:
            case COMBO: {
                CrudComboBox<Object> comboBox = new CrudComboBox<>();
                comboBox.getItems().setAll(form.options());
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
                return new CrudCheckBox();
            }
            default:
                throw new IllegalStateException("Unsupported form kind: " + form.kind());
        }
    }

    private List<String> validate() {
        List<String> errors = new ArrayList<>();
        for (FieldEntry<T> field : fields) {
            ColumnSpec<T> spec = field.spec();
            if (!spec.required()) {
                continue;
            }
            Object value = read(field.control(), spec.formSpec().kind());
            if (value == null) {
                if (spec.formSpec().kind() == FormSpec.Kind.NUMBER
                        || spec.formSpec().kind() == FormSpec.Kind.DECIMAL) {
                    errors.add(spec.title() + " must be a valid number");
                } else {
                    errors.add(spec.title() + " is required");
                }
            }
        }
        return errors;
    }

    private void applyValues() {
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
            case DECIMAL: {
                String text = ((TextField) control).getText();
                text = text == null ? "" : text.trim();
                if (text.isEmpty()) {
                    return null;
                }
                try {
                    return new BigDecimal(text);
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

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record FieldEntry<S>(ColumnSpec<S> spec, Control control) {
    }
}
