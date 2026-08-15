package com.corgibalance.controllers;

import com.corgibalance.components.table.TextTableCell;
import com.corgibalance.models.Tag;
import com.corgibalance.repositories.TagRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;

public class TagTableController extends BaseTableController<Tag, TagRepository> {

    @FXML
    private TableColumn<Tag, String> name;
    @FXML
    private TableColumn<Tag, String> color;
    @FXML
    private TableColumn<Tag, String> icon;

    public TagTableController() {
        super(new TagRepository());
    }

    @Override
    protected void configureColumns() {
        name.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        name.setCellFactory(_ -> new TextTableCell<>(Tag::getName, "+ Add tag"));
        name.setOnEditCommit(this::onNameCommitted);

        color.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getColor()));
        color.setCellFactory(_ -> new TextTableCell<>(Tag::getColor, null));
        color.setOnEditCommit(this::onColorCommitted);

        icon.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getIcon()));
        icon.setCellFactory(_ -> new TextTableCell<>(Tag::getIcon, null));
        icon.setOnEditCommit(this::onIconCommitted);
    }

    private void onNameCommitted(TableColumn.CellEditEvent<Tag, String> event) {
        String newName = event.getNewValue() == null ? "" : event.getNewValue().trim();
        if (newName.isEmpty()) {
            refresh();
            return;
        }
        commit(event.getRowValue(), tag -> tag.setName(newName), true);
    }

    private void onColorCommitted(TableColumn.CellEditEvent<Tag, String> event) {
        Tag tag = event.getRowValue();
        if (isPlaceholder(tag)) {
            refresh();
            return;
        }
        commit(tag, t -> t.setColor(event.getNewValue()), false);
    }

    private void onIconCommitted(TableColumn.CellEditEvent<Tag, String> event) {
        Tag tag = event.getRowValue();
        if (isPlaceholder(tag)) {
            refresh();
            return;
        }
        commit(tag, t -> t.setIcon(event.getNewValue()), false);
    }

    @Override
    protected Tag newPlaceholder() {
        Tag tag = new Tag();
        tag.setColor("#000000");
        return tag;
    }

    @Override
    protected String deleteConfirmationText(Tag tag) {
        return "Delete tag \"" + tag.getName() + "\"?";
    }
}