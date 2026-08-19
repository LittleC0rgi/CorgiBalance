package com.corgibalance.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PlannedTransaction extends BaseModel {

    private Long accountId;
    private Long tagId;
    private long amount;
    private String description;
    private TransactionType transactionType;
    private LocalDate plannedDate;
}