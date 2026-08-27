package com.corgibalance.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountFolder extends BaseModel {

    private String name;
    private boolean expanded = true;
    private Long parentId;
}
