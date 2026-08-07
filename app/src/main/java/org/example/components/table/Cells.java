package org.example.components.table;

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class Cells {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private Cells() {
    }

    public static <T> Callback<TableColumn<T, Object>, TableCell<T, Object>> editableText() {
        return column -> new TextEditCell<>();
    }

    public static <T> Callback<TableColumn<T, Object>, TableCell<T, Object>> longEditable() {
        return column -> new NumberEditCell<>();
    }

    public static <T> Callback<TableColumn<T, Object>, TableCell<T, Object>> dateEditable() {
        return column -> new DateEditCell<>();
    }

    public static <T> Callback<TableColumn<T, Object>, TableCell<T, Object>> enumEditable(
            List<? extends Enum<?>> values) {
        return column -> new EnumEditCell<>(values);
    }

    public static <T> Callback<TableColumn<T, Object>, TableCell<T, Object>> comboEditable(
            List<Long> ids, Map<Long, String> labels) {
        return column -> new ComboEditCell<>(ids, labels);
    }

    public static <T> Callback<TableColumn<T, Object>, TableCell<T, Object>> booleanEditable() {
        return column -> new BooleanEditCell<>();
    }

    public static <T> Callback<TableColumn<T, Object>, TableCell<T, Object>> colorEditable() {
        return column -> new ColorEditCell<>();
    }

    private static final class TextEditCell<T> extends TableCell<T, Object> {

        private final TextField textField = new TextField();

        TextEditCell() {
            textField.setOnAction(event -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (wasFocused && !isFocused) {
                    commitEdit(textField.getText());
                }
            });
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                setText(null);
                setGraphic(textField);
            } else {
                setText(item == null ? "" : String.valueOf(item));
                setGraphic(null);
            }
        }

        @Override
        public void startEdit() {
            if (!isEditable()) {
                return;
            }
            super.startEdit();
            textField.setText(getItem() == null ? "" : String.valueOf(getItem()));
            setText(null);
            setGraphic(textField);
            textField.requestFocus();
            textField.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() == null ? "" : String.valueOf(getItem()));
            setGraphic(null);
        }
    }

    private static final class NumberEditCell<T> extends TableCell<T, Object> {

        private final TextField textField = new TextField();

        NumberEditCell() {
            textField.setOnAction(event -> commitNumber());
            textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (wasFocused && !isFocused) {
                    commitNumber();
                }
            });
        }

        private void commitNumber() {
            String text = textField.getText() == null ? "" : textField.getText().trim();
            if (text.isEmpty()) {
                cancelEdit();
                return;
            }
            try {
                commitEdit(Long.parseLong(text));
            } catch (NumberFormatException e) {
                cancelEdit();
            }
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                setText(null);
                setGraphic(textField);
            } else {
                setText(item == null ? "" : String.valueOf(item));
                setGraphic(null);
            }
        }

        @Override
        public void startEdit() {
            if (!isEditable()) {
                return;
            }
            super.startEdit();
            textField.setText(getItem() == null ? "" : String.valueOf(getItem()));
            setText(null);
            setGraphic(textField);
            textField.requestFocus();
            textField.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() == null ? "" : String.valueOf(getItem()));
            setGraphic(null);
        }
    }

    private static final class DateEditCell<T> extends TableCell<T, Object> {

        private final DatePicker datePicker = new DatePicker();

        DateEditCell() {
            datePicker.setMaxWidth(Double.MAX_VALUE);
            datePicker.setConverter(new StringConverter<>() {
                @Override
                public String toString(LocalDate value) {
                    return value == null ? "" : DATE_FORMATTER.format(value);
                }

                @Override
                public LocalDate fromString(String text) {
                    if (text == null || text.isBlank()) {
                        return null;
                    }
                    try {
                        return LocalDate.parse(text);
                    } catch (RuntimeException e) {
                        return null;
                    }
                }
            });
            datePicker.setOnAction(event -> commitEdit(datePicker.getValue()));
            datePicker.getEditor().setOnAction(event -> commitEdit(datePicker.getValue()));
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                setText(null);
                setGraphic(datePicker);
            } else {
                setText(item == null ? "" : DATE_FORMATTER.format((LocalDate) item));
                setGraphic(null);
            }
        }

        @Override
        public void startEdit() {
            if (!isEditable()) {
                return;
            }
            super.startEdit();
            Object item = getItem();
            datePicker.setValue(item instanceof LocalDate date ? date : null);
            setText(null);
            setGraphic(datePicker);
            datePicker.getEditor().requestFocus();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem() == null ? "" : DATE_FORMATTER.format((LocalDate) getItem()));
            setGraphic(null);
        }
    }

    private static final class EnumEditCell<T> extends TableCell<T, Object> {

        private final ComboBox<Object> comboBox = new ComboBox<>();

        EnumEditCell(List<? extends Enum<?>> values) {
            comboBox.getItems().setAll(values);
            comboBox.setMaxWidth(Double.MAX_VALUE);
            comboBox.showingProperty().addListener((obs, wasShowing, isShowing) -> {
                if (wasShowing && !isShowing) {
                    commitEdit(comboBox.getValue());
                }
            });
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                setText(null);
                setGraphic(comboBox);
            } else {
                setText(item == null ? "" : item.toString());
                setGraphic(null);
            }
        }

        @Override
        public void startEdit() {
            if (!isEditable()) {
                return;
            }
            super.startEdit();
            comboBox.setValue(getItem());
            setText(null);
            setGraphic(comboBox);
        }
    }

    private static final class ComboEditCell<T> extends TableCell<T, Object> {

        private final Map<Long, String> labels;
        private final ComboBox<Long> comboBox = new ComboBox<>();

        ComboEditCell(List<Long> ids, Map<Long, String> labels) {
            this.labels = labels;
            comboBox.getItems().setAll(ids);
            comboBox.setMaxWidth(Double.MAX_VALUE);
            comboBox.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(Long id, boolean empty) {
                    super.updateItem(id, empty);
                    setText(empty || id == null ? "" : label(id));
                }
            });
            comboBox.showingProperty().addListener((obs, wasShowing, isShowing) -> {
                if (wasShowing && !isShowing) {
                    commitEdit(comboBox.getValue());
                }
            });
        }

        private String label(Long id) {
            return labels.getOrDefault(id, String.valueOf(id));
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                setText(null);
                setGraphic(comboBox);
            } else {
                Long id = (Long) item;
                setText(id == null ? "" : label(id));
                setGraphic(null);
            }
        }

        @Override
        public void startEdit() {
            if (!isEditable()) {
                return;
            }
            super.startEdit();
            comboBox.setValue((Long) getItem());
            setText(null);
            setGraphic(comboBox);
        }
    }

    private static final class BooleanEditCell<T> extends TableCell<T, Object> {

        private final CheckBox checkBox = new CheckBox();

        BooleanEditCell() {
            checkBox.setOnAction(event -> {
                if (!isEditing()) {
                    startEdit();
                }
                commitEdit(checkBox.isSelected());
            });
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                checkBox.setSelected(Boolean.TRUE.equals(item));
                setGraphic(checkBox);
                setText(null);
            }
        }
    }

    private static final class ColorEditCell<T> extends TableCell<T, Object> {

        private final TextField textField = new TextField();

        ColorEditCell() {
            textField.setOnAction(event -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (wasFocused && !isFocused) {
                    commitEdit(textField.getText());
                }
            });
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
                return;
            }
            if (isEditing()) {
                setText(null);
                setGraphic(textField);
                return;
            }
            String color = item == null ? "" : String.valueOf(item);
            try {
                Circle dot = new Circle(6, Color.web(color));
                Label label = new Label(color);
                HBox box = new HBox(8, dot, label);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            } catch (IllegalArgumentException e) {
                setGraphic(null);
                setText(color);
            }
        }

        @Override
        public void startEdit() {
            if (!isEditable()) {
                return;
            }
            super.startEdit();
            textField.setText(getItem() == null ? "" : String.valueOf(getItem()));
            setText(null);
            setGraphic(textField);
            textField.requestFocus();
            textField.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            updateItem(getItem(), isEmpty());
        }
    }
}
