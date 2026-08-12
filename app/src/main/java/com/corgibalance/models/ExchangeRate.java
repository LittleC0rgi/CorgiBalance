package com.corgibalance.models;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ExchangeRate extends BaseModel {

    private Long fromCurrencyId;
    private Long toCurrencyId;
    private BigDecimal rate;
    private LocalDate rateDate;
}
