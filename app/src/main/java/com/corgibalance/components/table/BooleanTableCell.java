package com.corgibalance.components.table;

import com.corgibalance.models.BaseModel;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;

import java.util.function.BiConsumer;

public class BooleanTableCell<T extends BaseModel> extends TableCell<T, Boolean> {

    private final CheckBox checkBox = new CheckBox();
    private final BiConsumer<T, Boolean> onChange;

    public BooleanTableCell(BiConsumer<T, Boolean> onChange) {
        this.onChange = onChange;
        checkBox.setOnAction(_ -> {
            T item = getTableRow() == null ? null : getTableRow().getItem();
            if (item != null) {
                onChange.accept(item, checkBox.isSelected());
            }
        });
    }

    @Override
    protected void updateItem(Boolean value, boolean empty) {
        super.updateItem(value, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            setAlignment(Pos.CENTER);
            setGraphic(checkBox);
            checkBox.setSelected(value != null && value);
        }
    }
}