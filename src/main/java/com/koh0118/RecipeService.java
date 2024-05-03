package com.koh0118;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class RecipeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeService.class);

    @Inject
    RecipeRepository recipeRepository;

    public void addRecipe(Recipe recipe) {
        LOGGER.debug("Adding a new recipe: {}", recipe.getTitle());
        recipeRepository.persist(recipe);
    }

    public List<Recipe> getAllRecipes() {
        return recipeRepository.listAll();
    }
}
