package com.corgibalance.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Currency extends BaseModel {

    private String code;
    private String name;
    private String symbol;
    private int minorUnit;
}
