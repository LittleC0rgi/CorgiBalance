package org.example.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class BaseModel {

    private Long id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
