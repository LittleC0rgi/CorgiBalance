package com.corgibalance.components.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class FormSpec {

    public enum Kind {
        TEXT,
        NUMBER,
        DECIMAL,
        DATE,
        ENUM,
        COMBO,
        BOOLEAN,
        COLOR
    }

    private final Kind kind;
    private final List<?> options;
    private final Function<Object, String> labeler;

    private FormSpec(Kind kind, List<?> options, Function<Object, String> labeler) {
        this.kind = kind;
        this.options = options;
        this.labeler = labeler;
    }

    public static FormSpec text() {
        return new FormSpec(Kind.TEXT, null, null);
    }

    public static FormSpec color() {
        return new FormSpec(Kind.COLOR, null, null);
    }

    public static FormSpec number() {
        return new FormSpec(Kind.NUMBER, null, null);
    }

    public static FormSpec decimal() {
        return new FormSpec(Kind.DECIMAL, null, null);
    }

    public static FormSpec date() {
        return new FormSpec(Kind.DATE, null, null);
    }

    public static FormSpec booleanValue() {
        return new FormSpec(Kind.BOOLEAN, null, null);
    }

    public static FormSpec enumValue(List<? extends Enum<?>> values) {
        return new FormSpec(Kind.ENUM, new ArrayList<>(values), String::valueOf);
    }

    public static FormSpec combo(List<Long> ids, Map<Long, String> labels) {
        Map<Object, String> labelsCopy = new HashMap<>(labels);
        return new FormSpec(Kind.COMBO, new ArrayList<>(ids),
                id -> labelsCopy.getOrDefault(id, String.valueOf(id)));
    }

    public Kind kind() {
        return kind;
    }

    public List<?> options() {
        return options;
    }

    public Function<Object, String> labeler() {
        return labeler;
    }
}
