package org.example.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RecurringTransaction extends BaseModel {

    private Long accountId;
    private Long tagId;
    private long amount;
    private String description;
    private TransactionType transactionType;
    private LocalDate startDate;
    private LocalDate nextDate;
    private LocalDate endDate;
    private RecurrenceInterval interval;
    private boolean active;
}
