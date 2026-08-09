package org.example.components.table;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ColumnSpec<T> {

    private final String title;
    private final double width;
    private final boolean editable;
    private final Function<T, Object> value;
    private final Callback<TableColumn<T, Object>, TableCell<T, Object>> cellFactory;
    private final BiConsumer<T, Object> onCommit;
    private final FormSpec formSpec;
    private final boolean required;
    private final Supplier<Object> defaultValue;
    private final Function<Object, String> hint;

    private ColumnSpec(Builder<T> builder) {
        this.title = builder.title;
        this.width = builder.width;
        this.editable = builder.editable;
        this.value = Objects.requireNonNull(builder.value, "value");
        this.cellFactory = builder.cellFactory;
        this.onCommit = builder.onCommit;
        this.formSpec = builder.formSpec;
        this.required = builder.required;
        this.defaultValue = builder.defaultValue;
        this.hint = builder.hint;
    }

    public static <T> Builder<T> builder(String title) {
        return new Builder<>(title);
    }

    public String title() {
        return title;
    }

    public double width() {
        return width;
    }

    public boolean editable() {
        return editable;
    }

    public Function<T, Object> value() {
        return value;
    }

    public Callback<TableColumn<T, Object>, TableCell<T, Object>> cellFactory() {
        return cellFactory;
    }

    public BiConsumer<T, Object> onCommit() {
        return onCommit;
    }

    public FormSpec formSpec() {
        return formSpec;
    }

    public boolean required() {
        return required;
    }

    public Supplier<Object> defaultValue() {
        return defaultValue;
    }

    public Function<Object, String> hint() {
        return hint;
    }

    public static final class Builder<T> {

        private final String title;
        private double width = 150;
        private boolean editable;
        private Function<T, Object> value;
        private Callback<TableColumn<T, Object>, TableCell<T, Object>> cellFactory;
        private BiConsumer<T, Object> onCommit;
        private FormSpec formSpec;
        private boolean required;
        private Supplier<Object> defaultValue;
        private Function<Object, String> hint;

        private Builder(String title) {
            this.title = title;
        }

        public Builder<T> width(double width) {
            this.width = width;
            return this;
        }

        public Builder<T> value(Function<T, Object> value) {
            this.value = value;
            return this;
        }

        public Builder<T> cellFactory(
                Callback<TableColumn<T, Object>, TableCell<T, Object>> cellFactory) {
            this.cellFactory = cellFactory;
            return this;
        }

        public Builder<T> editable(
                Callback<TableColumn<T, Object>, TableCell<T, Object>> cellFactory,
                BiConsumer<T, Object> onCommit) {
            this.editable = true;
            this.cellFactory = cellFactory;
            this.onCommit = onCommit;
            return this;
        }

        public Builder<T> form(FormSpec formSpec) {
            this.formSpec = formSpec;
            return this;
        }

        public Builder<T> required() {
            this.required = true;
            return this;
        }

        public Builder<T> defaultValue(Supplier<Object> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder<T> hint(Function<Object, String> hint) {
            this.hint = hint;
            return this;
        }

        public ColumnSpec<T> build() {
            return new ColumnSpec<>(this);
        }
    }
}
