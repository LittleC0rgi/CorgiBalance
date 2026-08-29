package com.corgibalance.components;

import com.corgibalance.models.Account;
import com.corgibalance.models.Tag;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.services.CurrencyConverter;
import com.corgibalance.services.NearestPaymentService;
import com.corgibalance.services.NearestPaymentService.NearestPayment;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NearestPaymentsComponent {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int NEAREST_LIMIT = 5;

    private final NearestPaymentService service;
    private final CurrencyConverter converter;
    private final AccountRepository accountRepository;
    private final TagRepository tagRepository;
    private final Runnable onChanged;

    public NearestPaymentsComponent(NearestPaymentService service, CurrencyConverter converter, Runnable onChanged) {
        this(service, converter, new AccountRepository(), new TagRepository(), onChanged);
    }

    public NearestPaymentsComponent(NearestPaymentService service, CurrencyConverter converter,
                                    AccountRepository accountRepository, TagRepository tagRepository,
                                    Runnable onChanged) {
        this.service = service;
        this.converter = converter;
        this.accountRepository = accountRepository;
        this.tagRepository = tagRepository;
        this.onChanged = onChanged;
    }

    public void render(VBox nearestList) {
        Map<Long, Long> accountCurrencies = new HashMap<>();
        Map<Long, String> tagColors = new HashMap<>();
        for (Account account : accountRepository.findAll()) {
            accountCurrencies.put(account.getId(), account.getCurrencyId());
        }
        for (Tag tag : tagRepository.findAll()) {
            tagColors.put(tag.getId(), tag.getColor());
        }
        List<NearestPayment> payments = service.upcoming(LocalDate.now(), NEAREST_LIMIT);

        nearestList.getChildren().clear();
        LocalDate today = LocalDate.now();
        for (NearestPayment payment : payments) {
            nearestList.getChildren().add(paymentRow(payment, today, accountCurrencies, tagColors));
        }
    }

    private HBox paymentRow(NearestPayment payment, LocalDate today, Map<Long, Long> accountCurrencies,
                            Map<Long, String> tagColors) {
        boolean overdue = payment.date().isBefore(today);

        Button confirm = new Button();
        confirm.setGraphic(new HeroIcon(HeroIcon.Icon.CHECK));
        confirm.getStyleClass().addAll("btn", "btn--transparent", "btn--mini", "nearest__btn");
        confirm.setTooltip(new Tooltip("Confirm"));
        confirm.setOnAction(_ -> Dialogs.runSafely(() -> service.confirm(payment), onChanged));

        Label description = new Label(paymentText(payment));
        description.getStyleClass().add("nearest__desc");
        description.setMaxWidth(Double.MAX_VALUE);

        Label date = new Label(String.valueOf(payment.date().getDayOfMonth()));
        date.getStyleClass().add("nearest__date");

        StackPane dateCard = new StackPane(date);
        dateCard.getStyleClass().add("nearest__date_card");
        Tooltip.install(dateCard, new Tooltip(payment.date().format(DATE_FORMAT)));

        Label amount = new Label(converter.format(payment.amount(), accountCurrencies.get(payment.accountId())));
        amount.getStyleClass().add("nearest__amount");
        amount.getStyleClass().add(payment.type() == TransactionType.EXPENSE ? "nearest__amount--expense" : "nearest__amount--income");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(dateCard, tagDot(payment.tagId(), tagColors), description, spacer, amount, confirm, deleteButton(payment));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(6);
        row.getStyleClass().add("nearest__row");
        if (overdue) {
            row.getStyleClass().add("nearest__row--overdue");
        }
        return row;
    }

    private Button deleteButton(NearestPayment payment) {
        Button delete = new Button();
        delete.setGraphic(new HeroIcon(HeroIcon.Icon.X_MARK));
        delete.getStyleClass().addAll("btn", "btn--danger-transparent", "btn--mini", "nearest__btn");
        delete.setTooltip(new Tooltip("Delete"));
        delete.setOnAction(_ -> Dialogs.runSafely(() -> service.delete(payment), onChanged));
        return delete;
    }

    private Circle tagDot(Long tagId, Map<Long, String> tagColors) {
        String color = tagId == null ? null : tagColors.get(tagId);
        if (color == null) {
            return new Circle(4, Color.TRANSPARENT);
        }
        try {
            return new Circle(4, Color.web(color));
        } catch (IllegalArgumentException e) {
            return new Circle(4, Color.TRANSPARENT);
        }
    }

    private static String paymentText(NearestPayment payment) {
        if (payment.description() != null && !payment.description().isBlank()) {
            return payment.description();
        }
        return payment.type() == TransactionType.EXPENSE ? "Planned expense" : "Planned income";
    }
}
