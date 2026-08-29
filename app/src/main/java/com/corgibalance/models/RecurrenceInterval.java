package com.corgibalance.models;

import java.time.LocalDate;

public enum RecurrenceInterval {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    public LocalDate next(LocalDate date) {
        return switch (this) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY -> date.plusYears(1);
        };
    }
}
