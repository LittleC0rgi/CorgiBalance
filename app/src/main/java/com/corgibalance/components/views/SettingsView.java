package com.corgibalance.components.views;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.corgibalance.components.table.Cells;
import com.corgibalance.components.table.ColumnSpec;
import com.corgibalance.components.table.CrudTable;
import com.corgibalance.components.table.FormSpec;
import com.corgibalance.models.Currency;
import com.corgibalance.models.ExchangeRate;
import com.corgibalance.models.Tag;
import com.corgibalance.repositories.CurrencyRepository;
import com.corgibalance.repositories.ExchangeRateRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.services.Database;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SettingsView extends View {

    @FXML
    private VBox sections;

    public SettingsView() {
        super("Settings", "/fxml/views/settings.fxml");
    }

    @FXML
    private void initialize() {
        buildSections();
    }

    private void buildSections() {
        sections.getChildren().clear();
        sections.getChildren().add(createDatabaseSection());
        sections.getChildren().add(createTagsTable());
        sections.getChildren().add(createCurrenciesTable());
        sections.getChildren().add(createExchangeRatesTable());
    }

    private VBox createDatabaseSection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("settings-section");

        Label title = new Label("Database");
        title.getStyleClass().add("settings-section-title");

        Label hint = new Label("Save a copy of your data or restore it from a backup.");
        hint.getStyleClass().add("crud-balance-label");

        Button saveButton = new Button("Save database");
        saveButton.getStyleClass().add("crud-btn");
        saveButton.setOnAction(event -> exportDatabase());

        Button importButton = new Button("Import database");
        importButton.getStyleClass().add("db-import-btn");
        importButton.setOnAction(event -> importDatabase());

        HBox buttons = new HBox(12, saveButton, importButton);

        section.getChildren().addAll(title, hint, buttons);
        return section;
    }

    private void exportDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save database");
        chooser.setInitialFileName("corgibalance.db");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite database", "*.db"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            Database.getInstance().exportTo(file.toPath());
            showInfo("Database saved", "Database saved to " + file.getAbsolutePath());
        } catch (SQLException e) {
            showError("Failed to save database", e.getMessage());
        }
    }

    private void importDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import database");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite database", "*.db"));
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("Importing will replace all current data with the selected database. Continue?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            Database.getInstance().importFrom(file.toPath());
            buildSections();
            showInfo("Database imported",
                    "Database imported from " + file.getAbsolutePath()
                            + ". Restart the application to fully refresh all views.");
        } catch (SQLException e) {
            showError("Failed to import database", e.getMessage());
        }
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private CrudTable<Tag> createTagsTable() {
        ColumnSpec<Tag> name = ColumnSpec.<Tag>builder("Name")
                .width(220)
                .value(Tag::getName)
                .editable(Cells.editableText(), (tag, value) -> tag.setName((String) value))
                .form(FormSpec.text())
                .required()
                .build();
        ColumnSpec<Tag> color = ColumnSpec.<Tag>builder("Color")
                .width(140)
                .value(Tag::getColor)
                .editable(Cells.colorEditable(), (tag, value) -> tag.setColor((String) value))
                .form(FormSpec.color())
                .build();
//        ColumnSpec<Tag> icon = ColumnSpec.<Tag>builder("Icon")
//                .width(180)
//                .value(Tag::getIcon)
//                .editable(Cells.editableText(), (tag, value) -> tag.setIcon((String) value))
//                .form(FormSpec.text())
//                .build();
        return new CrudTable<>("Tags", new TagRepository(), Tag::new, List.of(name, color));
    }

    private CrudTable<Currency> createCurrenciesTable() {
        ColumnSpec<Currency> code = ColumnSpec.<Currency>builder("Code")
                .width(120)
                .value(Currency::getCode)
                .editable(Cells.editableText(), (currency, value) -> currency.setCode((String) value))
                .form(FormSpec.text())
                .required()
                .build();
        ColumnSpec<Currency> name = ColumnSpec.<Currency>builder("Name")
                .width(260)
                .value(Currency::getName)
                .editable(Cells.editableText(), (currency, value) -> currency.setName((String) value))
                .form(FormSpec.text())
                .required()
                .build();
        ColumnSpec<Currency> symbol = ColumnSpec.<Currency>builder("Symbol")
                .width(120)
                .value(Currency::getSymbol)
                .editable(Cells.editableText(), (currency, value) -> currency.setSymbol((String) value))
                .form(FormSpec.text())
                .required()
                .build();
        ColumnSpec<Currency> minorUnit = ColumnSpec.<Currency>builder("Minor unit")
                .width(120)
                .value(Currency::getMinorUnit)
                .editable(Cells.longEditable(),
                        (currency, value) -> currency.setMinorUnit(((Number) value).intValue()))
                .form(FormSpec.number())
                .build();
        return new CrudTable<>("Currencies", new CurrencyRepository(), Currency::new,
                List.of(code, name, symbol, minorUnit));
    }

    private CrudTable<ExchangeRate> createExchangeRatesTable() {
        List<Long> currencyIds = new ArrayList<>();
        Map<Long, String> currencyLabels = new HashMap<>();
        for (Currency currency : new CurrencyRepository().findAll()) {
            currencyIds.add(currency.getId());
            currencyLabels.put(currency.getId(), currency.getCode());
        }

        ColumnSpec<ExchangeRate> from = ColumnSpec.<ExchangeRate>builder("From")
                .width(120)
                .value(ExchangeRate::getFromCurrencyId)
                .editable(Cells.comboEditable(currencyIds, currencyLabels),
                        (rate, value) -> rate.setFromCurrencyId((Long) value))
                .form(FormSpec.combo(currencyIds, currencyLabels))
                .required()
                .build();
        ColumnSpec<ExchangeRate> to = ColumnSpec.<ExchangeRate>builder("To")
                .width(120)
                .value(ExchangeRate::getToCurrencyId)
                .editable(Cells.comboEditable(currencyIds, currencyLabels),
                        (rate, value) -> rate.setToCurrencyId((Long) value))
                .form(FormSpec.combo(currencyIds, currencyLabels))
                .required()
                .build();
        ColumnSpec<ExchangeRate> rate = ColumnSpec.<ExchangeRate>builder("Rate")
                .width(140)
                .value(ExchangeRate::getRate)
                .editable(Cells.decimalEditable(),
                        (exchangeRate, value) -> exchangeRate.setRate((BigDecimal) value))
                .form(FormSpec.decimal())
                .required()
                .build();
        ColumnSpec<ExchangeRate> rateDate = ColumnSpec.<ExchangeRate>builder("Date")
                .width(130)
                .value(ExchangeRate::getRateDate)
                .editable(Cells.dateEditable(),
                        (exchangeRate, value) -> exchangeRate.setRateDate((LocalDate) value))
                .form(FormSpec.date())
                .required()
                .build();
        return new CrudTable<>("Exchange rates", new ExchangeRateRepository(), ExchangeRate::new,
                List.of(from, to, rate, rateDate));
    }
}
