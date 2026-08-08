package org.example.components.views;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.example.components.table.Cells;
import org.example.components.table.ColumnSpec;
import org.example.components.table.CrudTable;
import org.example.components.table.FormSpec;
import org.example.models.Currency;
import org.example.models.ExchangeRate;
import org.example.models.Tag;
import org.example.repositories.CurrencyRepository;
import org.example.repositories.ExchangeRateRepository;
import org.example.repositories.TagRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsView extends View {

    @FXML
    private VBox sections;

    public SettingsView() {
        super("Settings", "/fxml/views/settings.fxml");
    }

    @FXML
    private void initialize() {
        sections.getChildren().add(createTagsTable());
        sections.getChildren().add(createCurrenciesTable());
        sections.getChildren().add(createExchangeRatesTable());
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
