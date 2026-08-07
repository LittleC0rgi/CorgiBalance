package org.example.components.views;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.example.components.table.Cells;
import org.example.components.table.ColumnSpec;
import org.example.components.table.CrudTable;
import org.example.components.table.FormSpec;
import org.example.models.Currency;
import org.example.models.Tag;
import org.example.repositories.CurrencyRepository;
import org.example.repositories.TagRepository;

import java.util.List;

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
        ColumnSpec<Tag> icon = ColumnSpec.<Tag>builder("Icon")
                .width(180)
                .value(Tag::getIcon)
                .editable(Cells.editableText(), (tag, value) -> tag.setIcon((String) value))
                .form(FormSpec.text())
                .build();
        return new CrudTable<>("Tags", new TagRepository(), Tag::new, List.of(name, color, icon));
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
}
