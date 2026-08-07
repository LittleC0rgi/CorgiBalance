package org.example.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Budget extends BaseModel {

    private String name;
    private Long tagId;
    private long plannedAmount;
    private LocalDate startDate;
    private LocalDate endDate;
}
