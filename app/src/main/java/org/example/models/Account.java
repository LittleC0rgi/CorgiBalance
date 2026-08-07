package org.example.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Account extends BaseModel {

    private String name;
    private long initialBalance;
    private Long currencyId;
}
