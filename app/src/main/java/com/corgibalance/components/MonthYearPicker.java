package com.corgibalance.components;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class MonthYearPicker {

    private final ComboBox<Integer> monthCombo;
    private final ComboBox<Integer> yearCombo;
    private final boolean includeAll;

    public MonthYearPicker(ComboBox<Integer> monthCombo, ComboBox<Integer> yearCombo, boolean includeAll) {
        this.monthCombo = monthCombo;
        this.yearCombo = yearCombo;
        this.includeAll = includeAll;
    }

    public void initialize() {
        monthCombo.setCellFactory(monthCellFactory());
        monthCombo.setButtonCell(monthCellFactory().call(null));
        monthCombo.getItems().setAll(includeAll ? List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12) : List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        yearCombo.setCellFactory(yearCellFactory());
        yearCombo.setButtonCell(yearCellFactory().call(null));
    }

    public void load(List<Integer> years, String latest, boolean applyDefaults) {
        int defaultYear = latest == null ? LocalDate.now().getYear() : Integer.parseInt(latest.substring(0, 4));
        int defaultMonth = latest == null ? LocalDate.now().getMonthValue() : Integer.parseInt(latest.substring(5, 7));

        boolean multiYear = years.size() > 1;
        yearCombo.setVisible(multiYear);
        yearCombo.setManaged(multiYear);
        if (multiYear) {
            Integer selected = yearCombo.getValue();
            yearCombo.getItems().setAll(years);
            yearCombo.setValue(selected != null && years.contains(selected) ? selected : defaultYear);
        } else {
            yearCombo.getItems().clear();
            yearCombo.setValue(years.isEmpty() ? LocalDate.now().getYear() : years.getFirst());
        }
        if (applyDefaults || monthCombo.getValue() == null) {
            monthCombo.setValue(defaultMonth);
        }
    }

    public void setOnChange(Runnable handler) {
        monthCombo.valueProperty().addListener((obs, old, val) -> handler.run());
        yearCombo.valueProperty().addListener((obs, old, val) -> handler.run());
    }

    public int year() {
        return yearCombo.getValue() == null ? LocalDate.now().getYear() : yearCombo.getValue();
    }

    public int month() {
        return monthCombo.getValue() == null ? LocalDate.now().getMonthValue() : monthCombo.getValue();
    }

    private Callback<ListView<Integer>, ListCell<Integer>> monthCellFactory() {
        return list -> new ListCell<>() {
            @Override
            protected void updateItem(Integer month, boolean empty) {
                super.updateItem(month, empty);
                setText(empty || month == null ? "" : month == 0 ? "All" : Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            }
        };
    }

    private Callback<ListView<Integer>, ListCell<Integer>> yearCellFactory() {
        return list -> new ListCell<>() {
            @Override
            protected void updateItem(Integer year, boolean empty) {
                super.updateItem(year, empty);
                setText(empty || year == null ? "" : String.valueOf(year));
            }
        };
    }
}
