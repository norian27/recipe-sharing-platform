package com.koh0118;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Recipe extends PanacheEntity {
    private String title;
    private String description;
    private String ingredients;
    private String instructions;
}
