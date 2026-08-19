package com.corgibalance.components.table;

import com.corgibalance.models.Transaction;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DescriptionTemplateTableCell extends TableCell<Transaction, String> {

    private static final String PLACEHOLDER_STYLE_CLASS = "table__placeholder";

    private final String placeholderText;
    private final BiConsumer<Transaction, Transaction> applyTemplate;
    private final TextField textField = new TextField();
    private final TransactionSuggestionSupport suggestions;

    public DescriptionTemplateTableCell(String placeholderText,
                                        Function<String, List<Transaction>> searchFor,
                                        BiConsumer<Transaction, Transaction> applyTemplate,
                                        Function<Long, String> tagColorOf,
                                        Function<Long, Long> currencyIdOf) {
        this.placeholderText = placeholderText;
        this.applyTemplate = applyTemplate;
        this.suggestions = new TransactionSuggestionSupport(searchFor, tagColorOf, currencyIdOf) {
            @Override
            protected void apply(Transaction template) {
                Transaction target = currentItem();
                if (target == null) {
                    return;
                }
                applyTemplate.accept(template, target);
                commitEdit(template.getDescription());
                if (getTableView() != null) {
                    getTableView().refresh();
                }
            }
        };

        textField.setOnAction(_ -> commitFromTextField());
        textField.getStyleClass().add("input");
        textField.focusedProperty().addListener((_, _, isFocused) -> {
            if (!isFocused && !suggestions.isPopupShowing() && isEditing()) {
                cancelEdit();
            }
        });
        suggestions.bind(textField);
    }

    private Transaction currentItem() {
        return getTableRow() == null ? null : getTableRow().getItem();
    }

    private void commitFromTextField() {
        if (!isEditing()) {
            return;
        }
        Transaction first = suggestions.firstSuggestion();
        if (first != null && suggestions.isPopupShowing()) {
            suggestions.apply(first);
            return;
        }
        commitEdit(textField.getText());
    }

    @Override
    protected void updateItem(String value, boolean empty) {
        super.updateItem(value, empty);
        Transaction item = currentItem();
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().remove(PLACEHOLDER_STYLE_CLASS);
        } else if (item.getId() == null) {
            setText(placeholderText);
            setGraphic(null);
            if (!getStyleClass().contains(PLACEHOLDER_STYLE_CLASS)) {
                getStyleClass().add(PLACEHOLDER_STYLE_CLASS);
            }
        } else {
            setText(value);
            setGraphic(null);
            getStyleClass().remove(PLACEHOLDER_STYLE_CLASS);
        }
    }

    @Override
    public void startEdit() {
        if (!isEditable()) {
            return;
        }
        super.startEdit();
        Transaction item = currentItem();
        textField.setText(item == null || item.getDescription() == null ? "" : item.getDescription());
        setText(null);
        setGraphic(textField);
        textField.selectAll();
        textField.requestFocus();
    }

    @Override
    public void commitEdit(String newValue) {
        super.commitEdit(newValue);
        setGraphic(null);
        suggestions.hidePopup();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(null);
        suggestions.hidePopup();
        updateItem(getItem(), isEmpty());
    }
}