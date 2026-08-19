package com.corgibalance.components.table;

import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.services.CurrencyFormatter;
import javafx.animation.PauseTransition;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DescriptionTemplateTableCell extends TableCell<Transaction, String> {

    private static final String PLACEHOLDER_STYLE_CLASS = "table__placeholder";
    private static final String SUGGESTION_CSS = Objects.requireNonNull(
            DescriptionTemplateTableCell.class.getResource("/css/table.css")).toExternalForm();

    private final String placeholderText;
    private final Function<String, List<Transaction>> searchFor;
    private final BiConsumer<Transaction, Transaction> applyTemplate;
    private final Function<Long, String> tagColorOf;
    private final Function<Long, Long> currencyIdOf;
    private final CurrencyFormatter formatter = new CurrencyFormatter();
    private final TextField textField = new TextField();
    private final ListView<Transaction> suggestions = new ListView<>();
    private final Popup popup = new Popup();
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(200));

    public DescriptionTemplateTableCell(String placeholderText,
                                        Function<String, List<Transaction>> searchFor,
                                        BiConsumer<Transaction, Transaction> applyTemplate,
                                        Function<Long, String> tagColorOf,
                                        Function<Long, Long> currencyIdOf) {
        this.placeholderText = placeholderText;
        this.searchFor = searchFor;
        this.applyTemplate = applyTemplate;
        this.tagColorOf = tagColorOf;
        this.currencyIdOf = currencyIdOf;

        suggestions.getStyleClass().add("suggestion-list");
        suggestions.setMaxHeight(220);
        suggestions.setCellFactory(_ -> suggestionCell());
        suggestions.setOnMouseClicked(_ -> {
            Transaction selected = suggestions.getSelectionModel().getSelectedItem();
            if (selected != null) {
                apply(selected);
            }
        });

        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.getContent().add(suggestions);
        popup.setOnShown(_ -> popup.getScene().getStylesheets().add(SUGGESTION_CSS));

        textField.setOnAction(_ -> commitFromTextField());
        textField.getStyleClass().add("input");
        textField.focusedProperty().addListener((_, _, isFocused) -> {
            if (!isFocused && !popup.isShowing() && isEditing()) {
                cancelEdit();
            }
        });
        textField.textProperty().addListener((_, _, _) -> {
            searchDelay.setOnFinished(_ -> showSuggestions());
            searchDelay.playFromStart();
        });
    }

    private Transaction currentItem() {
        return getTableRow() == null ? null : getTableRow().getItem();
    }

    private ListCell<Transaction> suggestionCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Transaction transaction, boolean empty) {
                super.updateItem(transaction, empty);
                if (empty || transaction == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                HBox box = new HBox(8);
                box.setAlignment(Pos.CENTER_LEFT);
                Circle dot = dot(transaction.getTagId());
                if (dot != null) {
                    box.getChildren().add(dot);
                }
                box.getChildren().add(new Label(transaction.getDescription()));
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                box.getChildren().add(spacer);
                Label amount = new Label(formatAmount(transaction));
                amount.getStyleClass().add("suggestion__amount");
                box.getChildren().add(amount);
                setGraphic(box);
                setText(null);
            }
        };
    }

    private Circle dot(Long tagId) {
        if (tagId == null) {
            return null;
        }
        String color = tagColorOf.apply(tagId);
        if (color == null) {
            return null;
        }
        try {
            return new Circle(5, Color.web(color));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String formatAmount(Transaction transaction) {
        long display = transaction.getTransactionType() == TransactionType.EXPENSE
                || (transaction.getTransactionType() == TransactionType.TRANSFER && transaction.getDirection() == 0)
                ? -Math.abs(transaction.getAmount())
                : transaction.getAmount();
        return formatter.format(display, currencyIdOf.apply(transaction.getAccountId()));
    }

    private void showSuggestions() {
        String query = textField.getText().trim();
        if (query.isEmpty()) {
            popup.hide();
            return;
        }
        List<Transaction> matches = searchFor.apply(query);
        if (!textField.getText().trim().equals(query) || matches.isEmpty()) {
            popup.hide();
            return;
        }
        suggestions.getItems().setAll(matches);
        suggestions.setPrefWidth(Math.max(textField.getWidth(), 260));
        suggestions.setPrefHeight(Math.min(matches.size() * 32.0 + 4, 220));
        Point2D anchor = textField.localToScreen(0, textField.getHeight());
        popup.show(textField, anchor.getX(), anchor.getY() + 2);
    }

    private void apply(Transaction template) {
        Transaction target = currentItem();
        if (target == null) {
            return;
        }
        popup.hide();
        applyTemplate.accept(template, target);
        commitEdit(template.getDescription());
        if (getTableView() != null) {
            getTableView().refresh();
        }
    }

    private void commitFromTextField() {
        if (!isEditing()) {
            return;
        }
        if (popup.isShowing() && !suggestions.getItems().isEmpty()) {
            apply(suggestions.getItems().getFirst());
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
        popup.hide();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(null);
        popup.hide();
        updateItem(getItem(), isEmpty());
    }
}
