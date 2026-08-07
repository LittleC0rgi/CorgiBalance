package org.example.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Transfer extends BaseModel {

    private Long fromAccountId;
    private Long toAccountId;
    private long amount;
    private String description;
    private LocalDate transferDate;
}
