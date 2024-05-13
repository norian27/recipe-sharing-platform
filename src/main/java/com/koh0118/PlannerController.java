package com.koh0118;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlannerController {
    private static final String USERNAME_NOT_AVAILABLE = "Username is not available";
    private static final Logger logger = LoggerFactory.getLogger(PlannerController.class);
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML private ListView<String> plannerRecipesInPlannerListview;
    @FXML private ChoiceBox<String> plannerRecipesInPlannerMonChoices;
    @FXML private ChoiceBox<String> plannerRecipesInPlannerTueChoices;
    @FXML private ChoiceBox<String> plannerRecipesInPlannerWedChoices;
    @FXML private ChoiceBox<String> plannerRecipesInPlannerThuChoices;
    @FXML private ChoiceBox<String> plannerRecipesInPlannerFriChoices;

    private final Map<String, String> dayToRecipeMap = new HashMap<>();
    private static final String MONDAY = "monday";
    private static final String TUESDAY = "tuesday";
    private static final String WEDNESDAY = "wednesday";
    private static final String THURSDAY = "thursday";
    private static final String FRIDAY = "friday";

    @FXML
    public void initialize() {
        fetchPlannerRecipes();
        setupChoiceBoxListeners();
    }

    private void setupChoiceBoxListeners() {
        plannerRecipesInPlannerMonChoices.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> setRecipeForDay(MONDAY, newVal, plannerRecipesInPlannerMonChoices));
        plannerRecipesInPlannerTueChoices.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> setRecipeForDay(TUESDAY, newVal, plannerRecipesInPlannerTueChoices));
        plannerRecipesInPlannerWedChoices.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> setRecipeForDay(WEDNESDAY, newVal, plannerRecipesInPlannerWedChoices));
        plannerRecipesInPlannerThuChoices.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> setRecipeForDay(THURSDAY, newVal, plannerRecipesInPlannerThuChoices));
        plannerRecipesInPlannerFriChoices.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> setRecipeForDay(FRIDAY, newVal, plannerRecipesInPlannerFriChoices));
    }

    private void setRecipeForDay(String day, String recipeDetails, ChoiceBox<String> choiceBox) {
        if (recipeDetails == null || recipeDetails.isEmpty()) return;
        Long recipeId = Long.parseLong(recipeDetails.split(" - ")[0]);

        String username = AppController.getCurrentUsername();
        if (username == null) {
            logger.info("Username is not available");
            return;
        }

        String requestUri = String.format("http://localhost:8080/planners/%s/setRecipeForDay/%d/%s", username, recipeId, day);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUri))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    if (statusCode == 200) {
                        Platform.runLater(() -> {
                            logger.info("Recipe set for {} successfully", day);
                            choiceBox.getSelectionModel().select(recipeDetails);  // Re-select the item to ensure it stays visible
                        });
                    } else {
                        Platform.runLater(() -> logger.warn("Failed to set recipe for {}, status code: {}", day, statusCode));
                    }
                }).exceptionally(ex -> {
                    logger.error("Exception occurred while setting recipe for {}: {}", day, ex.getMessage(), ex);
                    return null;
                });
    }

    private void fetchPlannerRecipes() {
        String username = AppController.getCurrentUsername();
        if (username == null) {
            logger.warn("Username is not set. Please ensure the user is logged in.");
            return;
        }

        String uri = String.format("http://localhost:8080/planners/getPlannerRecipes/%s", username);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::updatePlannerRecipesListView)
                .exceptionally(ex -> {
                    logger.error("Failed to fetch planner recipes", ex);
                    return null;
                });
    }

    private void updatePlannerRecipesListView(String json) {
        List<String> plannerRecipes = parseRecipesFromJson(json).stream()
                .map(recipe -> recipe.getId() + " - " + recipe.getTitle())
                .toList();

        Platform.runLater(() -> {
            plannerRecipesInPlannerListview.setItems(FXCollections.observableArrayList(plannerRecipes));
            updateChoiceBoxes(plannerRecipes);
        });
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

    private void updateChoiceBoxes(List<String> recipes) {
        ObservableList<String> recipeOptions = FXCollections.observableArrayList(recipes);
        updateChoiceBox(plannerRecipesInPlannerMonChoices, recipeOptions, MONDAY);
        updateChoiceBox(plannerRecipesInPlannerTueChoices, recipeOptions, TUESDAY);
        updateChoiceBox(plannerRecipesInPlannerWedChoices, recipeOptions, WEDNESDAY);
        updateChoiceBox(plannerRecipesInPlannerThuChoices, recipeOptions, THURSDAY);
        updateChoiceBox(plannerRecipesInPlannerFriChoices, recipeOptions, FRIDAY);
    }

    private void updateChoiceBox(ChoiceBox<String> choiceBox, ObservableList<String> options, String day) {
        choiceBox.setItems(options);
        String selected = dayToRecipeMap.get(day);
        if (selected != null && options.contains(selected)) {
            choiceBox.setValue(selected);
        }
        choiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                dayToRecipeMap.put(day, newVal);  // Save the selection to the map
            }
        });
    }

    @FXML
    @SuppressWarnings("unused")
    private void removeFromPlanner(ActionEvent actionEvent) {
        String selectedEntry = plannerRecipesInPlannerListview.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            logger.info("No recipe selected in planner");
            return;
        }

        long recipeId;
        try {
            recipeId = Long.parseLong(selectedEntry.split(" - ")[0]);
        } catch (NumberFormatException e) {
            logger.error("Failed to extract recipe ID from the planner selection: {}", selectedEntry, e);
            return;
        }

        String username = AppController.getCurrentUsername();
        if (username == null) {
            logger.info(USERNAME_NOT_AVAILABLE);
            return;
        }

        String requestUri = String.format("http://localhost:8080/planners/%s/deleteRecipe/%d", username, recipeId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUri))
                .DELETE()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    logger.info("Received status code: {}", statusCode);
                    if (statusCode == 200) {
                        Platform.runLater(() -> {
                            logger.info("Recipe removed from planner successfully");
                            clearRecipeFromDays(recipeId);
                            fetchPlannerRecipes();
                            fetchWeeklyPlan();
                        });
                    } else {
                        Platform.runLater(() -> logger.warn("Failed to remove recipe from planner, status code: {}", statusCode));
                    }
                }).exceptionally(ex -> {
                    logger.error("Exception occurred while removing recipe from planner", ex);
                    return null;
                });
    }

    private void fetchWeeklyPlan() {
        String username = AppController.getCurrentUsername();
        if (username == null) {
            logger.info("Username is not set. Please ensure the user is logged in.");
            return;
        }

        String uri = String.format("http://localhost:8080/planners/getRecipesForWeek/%s", username);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::updatePlanListView)
                .exceptionally(ex -> {
                    logger.error("Failed to fetch weekly plan: {}", ex.getMessage());
                    return null;
                });
    }

    private void updatePlanListView(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, RecipeDTO> weeklyPlan = mapper.readValue(json, new TypeReference<>() {
            });
            ObservableList<String> items = FXCollections.observableArrayList();
            weeklyPlan.forEach((day, recipe) -> {
                if (recipe != null) {
                    items.add(day + ": " + recipe.getTitle());
                }
            });
            Platform.runLater(() -> plannerRecipesInPlannerListview.setItems(items));
        } catch (JsonProcessingException e) {
            logger.error("Error parsing weekly plan", e);
        }
    }


    private void clearRecipeFromDays(long recipeId) {
        String recipeIdString = String.valueOf(recipeId);
        clearRecipeFromDay(plannerRecipesInPlannerMonChoices, MONDAY, recipeIdString);
        clearRecipeFromDay(plannerRecipesInPlannerTueChoices, TUESDAY, recipeIdString);
        clearRecipeFromDay(plannerRecipesInPlannerWedChoices, WEDNESDAY, recipeIdString);
        clearRecipeFromDay(plannerRecipesInPlannerThuChoices, THURSDAY, recipeIdString);
        clearRecipeFromDay(plannerRecipesInPlannerFriChoices, FRIDAY, recipeIdString);
    }

    private void clearRecipeFromDay(ChoiceBox<String> choiceBox, String day, String recipeIdString) {
        String selected = dayToRecipeMap.get(day);
        if (selected != null && selected.startsWith(recipeIdString)) {
            choiceBox.getSelectionModel().clearSelection();
            dayToRecipeMap.remove(day);
        }
    }
}
