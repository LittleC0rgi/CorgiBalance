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
import java.util.function.Function;

public abstract class TransactionSuggestionSupport {

    private static final String SUGGESTION_CSS = Objects.requireNonNull(
            TransactionSuggestionSupport.class.getResource("/css/table.css")).toExternalForm();

    protected final ListView<Transaction> suggestions = new ListView<>();
    protected final Popup popup = new Popup();

    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(200));
    private final CurrencyFormatter formatter = new CurrencyFormatter();
    private final Function<String, List<Transaction>> searchFor;
    private final Function<Long, String> tagColorOf;
    private final Function<Long, Long> currencyIdOf;
    private final Function<Long, String> accountNameOf;
    private TextField field;

    protected TransactionSuggestionSupport(Function<String, List<Transaction>> searchFor,
                                           Function<Long, String> tagColorOf,
                                           Function<Long, Long> currencyIdOf,
                                           Function<Long, String> accountNameOf) {
        this.searchFor = searchFor;
        this.tagColorOf = tagColorOf;
        this.currencyIdOf = currencyIdOf;
        this.accountNameOf = accountNameOf;

        suggestions.getStyleClass().add("suggestion-list");
        suggestions.setMaxHeight(220);
        suggestions.setCellFactory(_ -> suggestionCell());
        suggestions.setOnMouseClicked(_ -> {
            Transaction selected = suggestions.getSelectionModel().getSelectedItem();
            if (selected != null) {
                popup.hide();
                apply(selected);
            }
        });

        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.getContent().add(suggestions);
        popup.setOnShown(_ -> popup.getScene().getStylesheets().add(SUGGESTION_CSS));
    }

    public void bind(TextField textField) {
        this.field = textField;
        textField.textProperty().addListener((_, _, _) -> {
            searchDelay.setOnFinished(_ -> showSuggestions());
            searchDelay.playFromStart();
        });
    }

    protected abstract void apply(Transaction template);

    protected boolean isPopupShowing() {
        return popup.isShowing();
    }

    protected Transaction firstSuggestion() {
        return suggestions.getItems().isEmpty() ? null : suggestions.getItems().getFirst();
    }

    protected void hidePopup() {
        popup.hide();
    }

    private void showSuggestions() {
        String query = field.getText().trim();
        if (query.isEmpty()) {
            popup.hide();
            return;
        }
        List<Transaction> matches = searchFor.apply(query);
        if (!field.getText().trim().equals(query) || matches.isEmpty()) {
            popup.hide();
            return;
        }
        suggestions.getItems().setAll(matches);
        suggestions.setPrefWidth(Math.max(field.getWidth(), 260));
        suggestions.setPrefHeight(Math.min(matches.size() * 32.0 + 4, 220));
        Point2D anchor = field.localToScreen(0, field.getHeight());
        popup.show(field, anchor.getX(), anchor.getY() + 2);
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
                String accountName = accountNameOf.apply(transaction.getAccountId());
                if (accountName != null && !accountName.isEmpty()) {
                    Label account = new Label("(" + accountName + ")");
                    account.getStyleClass().add("suggestion__account");
                    box.getChildren().add(account);
                }
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
}