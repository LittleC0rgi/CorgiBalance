package com.corgibalance.components.table;

import com.corgibalance.components.HeroCheckBox;
import com.corgibalance.models.BaseModel;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;

import java.util.function.BiConsumer;

public class BooleanTableCell<T extends BaseModel> extends TableCell<T, Boolean> {

    private final HeroCheckBox checkBox = new HeroCheckBox();
    private boolean updating;

    public BooleanTableCell(BiConsumer<T, Boolean> onChange) {
        checkBox.selectedProperty().addListener((_, _, on) -> {
            if (updating) {
                return;
            }
            T item = getTableRow() == null ? null : getTableRow().getItem();
            if (item != null) {
                onChange.accept(item, on);
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
            updating = true;
            checkBox.setSelected(value != null && value);
            updating = false;
        }
    }
}