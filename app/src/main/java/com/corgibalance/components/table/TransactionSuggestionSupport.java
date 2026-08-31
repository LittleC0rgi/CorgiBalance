package com.corgibalance.components.table;

import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.services.CurrencyFormatter;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public abstract class TransactionSuggestionSupport {

    private static final String SUGGESTION_CSS = Objects.requireNonNull(
            TransactionSuggestionSupport.class.getResource("/css/table.css")).toExternalForm();

    private static final ExecutorService SEARCH_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "transaction-suggestion-search");
        thread.setDaemon(true);
        return thread;
    });

    protected final ListView<Transaction> suggestions = new ListView<>();
    protected final Popup popup = new Popup();

    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(200));
    private final CurrencyFormatter formatter;
    private final Function<String, List<Transaction>> searchFor;
    private final Function<Long, String> tagColorOf;
    private final Function<Long, Long> currencyIdOf;
    private final Function<Long, String> accountNameOf;
    private TextField field;

    protected TransactionSuggestionSupport(CurrencyFormatter formatter,
                                           Function<String, List<Transaction>> searchFor,
                                           Function<Long, String> tagColorOf,
                                           Function<Long, Long> currencyIdOf,
                                           Function<Long, String> accountNameOf) {
        this.formatter = formatter;
        this.searchFor = searchFor;
        this.tagColorOf = tagColorOf;
        this.currencyIdOf = currencyIdOf;
        this.accountNameOf = accountNameOf;

        suggestions.getStyleClass().add("suggestion-list");
        suggestions.getStylesheets().add(SUGGESTION_CSS);
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
    }

    private static void setVisibleManaged(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    public void bind(TextField textField) {
        this.field = textField;
        searchDelay.setOnFinished(_ -> showSuggestions());
        textField.textProperty().addListener((_, _, _) -> searchDelay.playFromStart());
        textField.sceneProperty().addListener((_, _, _) -> onSceneChanged());
        popup.sceneProperty().addListener((_, _, _) -> onSceneChanged());
        onSceneChanged();
    }

    private void onSceneChanged() {
        Scene fieldScene = field.getScene();
        if (fieldScene != null) {
            fieldScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);
        }
        Scene popupScene = popup.getScene();
        if (popupScene != null) {
            popupScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);
        }
    }

    private void handleKey(KeyEvent event) {
        if (!popup.isShowing() || suggestions.getItems().isEmpty()) {
            return;
        }
        switch (event.getCode()) {
            case ENTER -> {
                Transaction selected = selectedSuggestion();
                if (selected != null) {
                    popup.hide();
                    apply(selected);
                    event.consume();
                }
            }
            case ESCAPE -> {
                popup.hide();
                event.consume();
            }
        }
    }

    protected abstract void apply(Transaction template);

    protected boolean isPopupShowing() {
        return popup.isShowing();
    }

    protected Transaction selectedSuggestion() {
        Transaction selected = suggestions.getSelectionModel().getSelectedItem();
        return selected != null ? selected
                : suggestions.getItems().isEmpty() ? null : suggestions.getItems().getFirst();
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
        SEARCH_EXECUTOR.submit(() -> {
            List<Transaction> matches;
            try {
                matches = searchFor.apply(query);
            } catch (RuntimeException e) {
                return;
            }
            Platform.runLater(() -> showMatches(query, matches));
        });
    }

    private void showMatches(String query, List<Transaction> matches) {
        if (!field.getText().trim().equals(query)) {
            return;
        }
        if (matches.isEmpty()) {
            popup.hide();
            return;
        }
        suggestions.getItems().setAll(matches);
        suggestions.getSelectionModel().selectFirst();
        suggestions.setPrefWidth(Math.max(field.getWidth(), 260));
        suggestions.setPrefHeight(Math.min(matches.size() * 32.0 + 4, 220));
        Point2D anchor = field.localToScreen(0, field.getHeight());
        popup.show(field, anchor.getX(), anchor.getY() + 2);
    }

    private ListCell<Transaction> suggestionCell() {
        return new ListCell<>() {
            private final HBox box = new HBox(8);
            private final Circle dot = new Circle(5);
            private final Label description = new Label();
            private final Region spacer = new Region();
            private final Label amount = new Label();
            private final Label account = new Label();

            {
                box.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                amount.getStyleClass().add("suggestion__amount");
                account.getStyleClass().add("suggestion__account");
                box.getChildren().addAll(dot, description, spacer, amount, account);
            }

            @Override
            protected void updateItem(Transaction transaction, boolean empty) {
                super.updateItem(transaction, empty);
                if (empty || transaction == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Color color = tagColor(transaction.getTagId());
                dot.setFill(color == null ? Color.TRANSPARENT : color);
                setVisibleManaged(dot, color != null);
                description.setText(transaction.getDescription());
                amount.setText(formatAmount(transaction));
                String accountName = accountNameOf.apply(transaction.getAccountId());
                boolean hasAccount = accountName != null && !accountName.isEmpty();
                account.setText(hasAccount ? "(" + accountName + ")" : "");
                setVisibleManaged(account, hasAccount);
                setGraphic(box);
                setText(null);
            }
        };
    }

    private Color tagColor(Long tagId) {
        if (tagId == null) {
            return null;
        }
        String color = tagColorOf.apply(tagId);
        if (color == null) {
            return null;
        }
        try {
            return Color.web(color);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String formatAmount(Transaction transaction) {
        long display = transaction.getTransactionType() == TransactionType.EXPENSE
                || (transaction.getTransactionType() == TransactionType.TRANSFER
                && transaction.getDirection() == Transaction.DIRECTION_OUTGOING)
                ? -Math.abs(transaction.getAmount())
                : transaction.getAmount();
        return formatter.format(display, currencyIdOf.apply(transaction.getAccountId()));
    }
}