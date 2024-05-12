package com.koh0118;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Planner extends PanacheEntity {
    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne
    private Recipe mondayRecipe;

    @ManyToOne
    private Recipe tuesdayRecipe;

    @ManyToOne
    private Recipe wednesdayRecipe;

    @ManyToOne
    private Recipe thursdayRecipe;

    @ManyToOne
    private Recipe fridayRecipe;

    @ManyToMany
    private List<Recipe> recipes = new ArrayList<>();


}