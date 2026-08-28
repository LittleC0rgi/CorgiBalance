package com.corgibalance.components;

import com.corgibalance.models.Tag;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyConverter;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProfitLossReport {

    private ProfitLossReport() {
    }

    public record Row(String name, long income, long expense) {
    }

    public record Data(List<Row> rows, long totalIncome, long totalExpense, long profit) {
    }

    public static Data compute(TransactionRepository transactions, TagRepository tags,
                               CurrencyConverter converter, Long baseCurrencyId, int year, int month) {
        long totalIncome = sumForPeriod(transactions, converter, baseCurrencyId, TransactionType.INCOME, year, month);
        long totalExpense = sumForPeriod(transactions, converter, baseCurrencyId, TransactionType.EXPENSE, year, month);

        Map<Long, long[]> tagTotals = new HashMap<>();
        fillTagTotals(tagTotals, transactions.sumByTag(TransactionType.INCOME, year, month),
                converter, baseCurrencyId, 0);
        fillTagTotals(tagTotals, transactions.sumByTag(TransactionType.EXPENSE, year, month),
                converter, baseCurrencyId, 1);

        long taggedIncome = 0;
        long taggedExpense = 0;
        for (long[] totals : tagTotals.values()) {
            taggedIncome += totals[0];
            taggedExpense += totals[1];
        }

        Map<Long, String> names = new HashMap<>();
        for (Tag tag : tags.findAll()) {
            names.put(tag.getId(), tag.getName());
        }
        List<Long> tagIds = new ArrayList<>(tagTotals.keySet());
        tagIds.sort(Comparator.comparing(id -> names.getOrDefault(id, String.valueOf(id))));

        List<Row> rows = new ArrayList<>();
        for (Long tagId : tagIds) {
            long[] totals = tagTotals.get(tagId);
            rows.add(new Row(names.getOrDefault(tagId, String.valueOf(tagId)), totals[0], totals[1]));
        }
        long uncategorizedIncome = totalIncome - taggedIncome;
        long uncategorizedExpense = totalExpense - taggedExpense;
        if (uncategorizedIncome != 0 || uncategorizedExpense != 0) {
            rows.add(new Row("Uncategorized", uncategorizedIncome, uncategorizedExpense));
        }
        return new Data(rows, totalIncome, totalExpense, totalIncome - totalExpense);
    }

    public static void populate(GridPane grid, Data data, CurrencyConverter converter, Long baseCurrencyId,
                                boolean showTotal) {
        grid.getChildren().clear();
        int row = 0;
        grid.add(headerCell("Category"), 0, 0);
        grid.add(valueHeaderCell("Income"), 1, 0);
        grid.add(valueHeaderCell("Expense"), 2, 0);

        for (Row r : data.rows()) {
            row++;
            grid.add(nameCell(r.name(), false), 0, row);
            grid.add(valueCell(converter.format(r.income(), baseCurrencyId), "report__income", false), 1, row);
            grid.add(valueCell(converter.format(r.expense(), baseCurrencyId), "report__expense", false), 2, row);
        }

        if (showTotal) {
            row++;
            Region separator = new Region();
            separator.getStyleClass().add("report__separator");
            separator.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(separator, Priority.ALWAYS);
            GridPane.setColumnSpan(separator, 3);
            grid.add(separator, 0, row);

            row++;
            grid.add(nameCell("Total", true), 0, row);
            grid.add(valueCell(converter.format(data.totalIncome(), baseCurrencyId), "report__income", true), 1, row);
            grid.add(valueCell(converter.format(data.totalExpense(), baseCurrencyId), "report__expense", true), 2, row);
        }

        row++;
        boolean profit = data.profit() >= 0;
        String profitText = data.profit() > 0
                ? "+" + converter.format(data.profit(), baseCurrencyId)
                : converter.format(data.profit(), baseCurrencyId);
        Label profitName = new Label("Profit / Loss");
        profitName.getStyleClass().addAll("report__cell", "report__name", "report__profit-name");
        grid.add(profitName, 0, row);
        grid.add(valueCell(profitText, profit ? "report__profit" : "report__loss", false), 1, row);
    }

    private static long sumForPeriod(TransactionRepository transactions, CurrencyConverter converter,
                                     Long baseCurrencyId, TransactionType type, int year, int month) {
        long result = 0;
        for (Map.Entry<Long, Long> entry : transactions.sumByCurrency(type, year, month).entrySet()) {
            result += converter.convert(Math.abs(entry.getValue()), entry.getKey(), baseCurrencyId);
        }
        return result;
    }

    private static void fillTagTotals(Map<Long, long[]> totals, Map<Long, Map<Long, Long>> byTag,
                                      CurrencyConverter converter, Long baseCurrencyId, int index) {
        for (Map.Entry<Long, Map<Long, Long>> entry : byTag.entrySet()) {
            long total = 0;
            for (Map.Entry<Long, Long> currency : entry.getValue().entrySet()) {
                total += converter.convert(Math.abs(currency.getValue()), currency.getKey(), baseCurrencyId);
            }
            if (total != 0) {
                totals.computeIfAbsent(entry.getKey(), _ -> new long[2])[index] = total;
            }
        }
    }

    private static Label headerCell(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("report__header");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static Label valueHeaderCell(String text) {
        Label label = headerCell(text);
        label.getStyleClass().add("report__header-value");
        return label;
    }

    private static Label nameCell(String name, boolean bold) {
        Label label = new Label(name);
        label.getStyleClass().add("report__cell");
        label.getStyleClass().add("report__name");
        label.setMaxWidth(Double.MAX_VALUE);
        if (bold) {
            label.getStyleClass().add("report__total");
        }
        return label;
    }

    private static Label valueCell(String text, String toneClass, boolean bold) {
        Label label = new Label(text);
        label.getStyleClass().add("report__cell");
        if (toneClass != null) {
            label.getStyleClass().add(toneClass);
        }
        if (bold) {
            label.getStyleClass().add("report__total");
        }
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_RIGHT);
        return label;
    }
}