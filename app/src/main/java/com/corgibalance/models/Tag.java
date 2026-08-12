package com.corgibalance.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tag extends BaseModel {

    private String name;
    private String color;
    private String icon;
}
