package com.koh0118;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RecipesController {
    private static final String USERNAME_NOT_AVAILABLE = "Username is not available";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private static final Logger logger = LoggerFactory.getLogger(RecipesController.class);
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    private ListView<String> recipesAllRecipesListview;
    @FXML
    private TextArea recipesRecipeDetails;
    @FXML
    private TextField recipesRecipeName;

    private CreateRecipeController createRecipeController;

    @FXML
    public void initialize() {
        fetchRecipes();
        recipesAllRecipesListview.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> showRecipeDetails());
    }

    public void setCreateRecipeController(CreateRecipeController createRecipeController) {
        this.createRecipeController = createRecipeController;
    }
    void fetchRecipes() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/recipes"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        updateRecipesListView(response.body());
                    }
                }).exceptionally(ex -> {
                    logger.error("Failed to fetch recipes", ex);
                    return null;
                });
    }

    private void updateRecipesListView(String json) {
        List<String> displayedRecipes = parseRecipesFromJson(json).stream()
                .map(recipe -> recipe.getId() + " - " + recipe.getTitle() + " - " + recipe.getIngredients())
                .collect(Collectors.toList());

        Platform.runLater(() -> recipesAllRecipesListview.setItems(FXCollections.observableArrayList(displayedRecipes)));
    }

    private List<Recipe> parseRecipesFromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse recipes from JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @FXML
    private void showRecipeDetails() {
        String selectedRecipe = recipesAllRecipesListview.getSelectionModel().getSelectedItem();
        if (selectedRecipe != null && !selectedRecipe.isEmpty()) {
            String[] parts = selectedRecipe.split(" - ", 4);
            if (parts.length == 4) {
                recipesRecipeName.setText(parts[1]);
                recipesRecipeDetails.setText(parts[2]);
            } else {
                recipesRecipeName.setText(selectedRecipe);
                recipesRecipeDetails.clear();
            }
        } else {
            recipesRecipeName.clear();
            recipesRecipeDetails.clear();
        }
    }

    @FXML
    public void deleteRecipe() {
        String selected = recipesAllRecipesListview.getSelectionModel().getSelectedItem();
        if (selected == null) {
            logger.info("No recipe selected to delete.");
            return;
        }

        long recipeId;
        try {
            recipeId = Long.parseLong(selected.split(" - ")[0]);
        } catch (NumberFormatException e) {
            logger.info("Invalid format for recipe ID.");
            return;
        }

        deleteRecipeFromServer(recipeId);
        fetchRecipes();
    }

    private void deleteRecipeFromServer(Long recipeId) {
        String requestUri = "http://localhost:8080/recipes/delete/" + recipeId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUri))
                .DELETE()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    if (statusCode == 200) {
                        Platform.runLater(() -> {
                            recipesAllRecipesListview.getItems().removeIf(item -> item.startsWith(recipeId + " -"));
                            logger.info("Recipe deleted successfully.");
                        });
                    } else {
                        Platform.runLater(() -> logger.warn("Failed to delete recipe, status code: {}", statusCode));
                    }
                }).exceptionally(ex -> {
                    logger.error("Error deleting recipe: {}", ex.getMessage());
                    return null;
                });
    }

    @FXML
    @SuppressWarnings("unused")
    private void addRecipeToPlan(ActionEvent actionEvent) {
        String selectedEntry = recipesAllRecipesListview.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            logger.info("No recipe selected");
            return;
        }

        long recipeId;
        try {
            recipeId = Long.parseLong(selectedEntry.split(" - ")[0]);
        } catch (NumberFormatException e) {
            logger.error("Failed to extract recipe ID from the selection: {}", selectedEntry, e);
            return;
        }
        logger.debug("Sending recipe ID: {}", recipeId);

        String username = AppController.getCurrentUsername();
        if (username == null) {
            logger.info(USERNAME_NOT_AVAILABLE);
            return;
        }

        String requestUri = String.format("http://localhost:8080/planners/%s/addRecipe/%d", username, recipeId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUri))
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    logger.info("Received status code: {}", statusCode);
                    if (statusCode == 200) {
                        Platform.runLater(() -> logger.info("Recipe added to planner successfully"));
                    } else {
                        Platform.runLater(() -> logger.warn("Failed to add recipe to planner, status code: {}", statusCode));
                    }
                }).exceptionally(ex -> {
                    logger.error("Exception occurred while adding recipe to planner", ex);
                    return null;
                });
    }

    @FXML
    private void onEditRecipe() {
        String selected = recipesAllRecipesListview.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isEmpty()) {
            Long recipeId = Long.parseLong(selected.split(" - ")[0]);
            createRecipeController.loadRecipeForEditing(recipeId);
        }
    }
}
