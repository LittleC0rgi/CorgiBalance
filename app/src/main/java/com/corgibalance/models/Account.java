package com.corgibalance.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Account extends BaseModel {

    private String name;
    private long initialBalance;
    private Long currencyId;
    private Long folderId;
}
