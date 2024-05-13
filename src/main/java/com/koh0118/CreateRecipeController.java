package com.koh0118;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CreateRecipeController {

    private static final Logger logger = LoggerFactory.getLogger(CreateRecipeController.class);
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML private TextField createRecipeNameTextfield;
    @FXML private TextArea createRecipeDescriptionTextfield;
    @FXML private TextField createRecipeIngredientsTextfield;
    @FXML private TextArea createRecipeStepsTextfield;

    private boolean isEditMode = false;
    private Long editingRecipeId = null;

    public void loadRecipeForEditing(Long recipeId) {
        httpClient.sendAsync(
                        HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/recipes/" + recipeId)).GET().build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(jsonString -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        RecipeDTO recipe = mapper.readValue(jsonString, RecipeDTO.class);
                        Platform.runLater(() -> {
                            createRecipeNameTextfield.setText(recipe.getTitle());
                            createRecipeDescriptionTextfield.setText(recipe.getDescription());
                            createRecipeIngredientsTextfield.setText(recipe.getIngredients());
                            createRecipeStepsTextfield.setText(recipe.getInstructions());
                            isEditMode = true;
                            editingRecipeId = recipeId;
                        });
                    } catch (JsonProcessingException e) {
                        logger.error("Failed to parse recipe data", e);
                    }
                }).exceptionally(ex -> {
                    logger.error("Error retrieving recipe data", ex);
                    return null;
                });
    }

    @FXML
    private void submitRecipe() {
        String uri = isEditMode ? "http://localhost:8080/recipes/update/" + editingRecipeId : "http://localhost:8080/recipes";
        String method = isEditMode ? "PUT" : "POST";

        String json = String.format("{\"title\":\"%s\", \"description\":\"%s\", \"ingredients\":\"%s\", \"instructions\":\"%s\"}",
                createRecipeNameTextfield.getText(),
                createRecipeDescriptionTextfield.getText(),
                createRecipeIngredientsTextfield.getText(),
                createRecipeStepsTextfield.getText());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        clearForm();
                        // Call a method to update the recipes list in RecipesController
                        // recipesController.fetchRecipes();
                        logger.info(isEditMode ? "Recipe updated successfully" : "Recipe created successfully");
                        isEditMode = false;
                        editingRecipeId = null;
                    } else {
                        logger.error("Failed to submit recipe, status code: {}", response.statusCode());
                    }
                })).exceptionally(ex -> {
                    logger.error("Exception occurred while submitting recipe", ex);
                    return null;
                });
    }

    private void clearForm() {
        createRecipeNameTextfield.clear();
        createRecipeDescriptionTextfield.clear();
        createRecipeIngredientsTextfield.clear();
        createRecipeStepsTextfield.clear();
    }
}
