package com.corgibalance.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Transaction extends BaseModel {

    public static final int DIRECTION_OUTGOING = 0;
    public static final int DIRECTION_INCOMING = 1;

    private Long accountId;
    private Long tagId;
    private Long toAccountId;
    private Long transferId;
    private String rate;
    private int direction = 1;
    private long amount;
    private String description;
    private TransactionType transactionType;
    private LocalDate transactionDate;
}
