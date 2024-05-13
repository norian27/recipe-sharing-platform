package com.koh0118;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppController {

    private static final String USERNAME_NOT_AVAILABLE = "Username is not available";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String MONDAY = "monday";
    private static final String TUESDAY = "tuesday";
    private static final String WEDNESDAY = "wednesday";
    private static final String THURSDAY = "thursday";
    private static final String FRIDAY = "friday";



    private static final Logger logger = LoggerFactory.getLogger(AppController.class);

    @FXML private TextField createRecipeNameTextfield;
    @FXML private TextArea createRecipeDescriptionTextfield;
    @FXML private TextField createRecipeIngredientsTextfield;
    @FXML private TextArea createRecipeStepsTextfield;
    @FXML private ListView<String> recipesAllRecipesListview;
    @FXML private TextArea recipesRecipeDetails;
    @FXML private TextField recipesRecipeName;
    @FXML private ListView<String> plannerRecipesInPlannerListview;
    @FXML private TabPane tabPane;
    @FXML
    private ChoiceBox<String> plannerRecipesInPlannerMonChoices;
    @FXML
    private ChoiceBox<String> plannerRecipesInPlannerTueChoices;
    @FXML
    private ChoiceBox<String> plannerRecipesInPlannerWedChoices;
    @FXML
    private ChoiceBox<String> plannerRecipesInPlannerThuChoices;
    @FXML
    private ChoiceBox<String> plannerRecipesInPlannerFriChoices;

    @FXML
    private ListView<String> planListViewRecipes;

    private final Map<String, String> dayToRecipeMap = new HashMap<>();
    private final Map<String, RecipeDTO> dayToRecipeDetails = new HashMap<>();

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private boolean isEditMode = false;
    private Long editingRecipeId = null;


    private static String currentUsername;
    String getCurrentUsername() {
        return currentUsername;
    }
    void setCurrentUsername(String username) {
        currentUsername = username;
    }
    @FXML
    public void initialize() {
        recipesAllRecipesListview.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
                showRecipeDetails());
        fetchRecipes();
        logger.info(("Username:"+getCurrentUsername()));
        setupTabChangeListener();
        setupChoiceBoxListeners();

    }
    @FXML
    @SuppressWarnings("unused")
    private void showRecipeDetail(ActionEvent event) {
        String selected = planListViewRecipes.getSelectionModel().getSelectedItem();
        if (selected == null) {
            logger.info("No recipe selected");
            return;
        }
        String day = selected.split(":")[0].trim();
        RecipeDTO details = dayToRecipeDetails.get(day);
        if (details != null) {
            openRecipeCard(details);
        }
    }



    private void openRecipeCard(RecipeDTO details) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/recipe_card.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Recipe Details");
            stage.setScene(new Scene(root));

            RecipeCardAppController controller = loader.getController();
            controller.setRecipeDetails(details);


            stage.show();
        } catch (IOException e) {
            logger.error("Error loading the recipe card view: {}", e.getMessage(), e);
        }
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

        String username = getCurrentUsername();
        if (username == null) {
            logger.info(USERNAME_NOT_AVAILABLE);
            return;
        }

        String requestUri = String.format("http://localhost:8080/planners/%s/setRecipeForDay/%d/%s", username, recipeId, day);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUri))
                .header(CONTENT_TYPE, APPLICATION_JSON)
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

    private void setupTabChangeListener() {
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && "Planner".equals(newTab.getText())) {
                fetchPlannerRecipes();
                restoreChoiceBoxSelections();
            }
            if (newTab != null && "Plan".equals(newTab.getText())) {
                fetchWeeklyPlan();
            }
        });
    }
    private void fetchWeeklyPlan() {
        String username = getCurrentUsername();
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
                    dayToRecipeDetails.put(day, recipe);
                }
            });
            Platform.runLater(() -> planListViewRecipes.setItems(items));
        } catch (JsonProcessingException e) {
            logger.error("Error parsing weekly plan", e);
        }
    }


    private void restoreChoiceBoxSelections() {
        restoreSelection(plannerRecipesInPlannerMonChoices, dayToRecipeMap.get(MONDAY));
        restoreSelection(plannerRecipesInPlannerTueChoices, dayToRecipeMap.get(TUESDAY));
        restoreSelection(plannerRecipesInPlannerWedChoices, dayToRecipeMap.get(WEDNESDAY));
        restoreSelection(plannerRecipesInPlannerThuChoices, dayToRecipeMap.get(THURSDAY));
        restoreSelection(plannerRecipesInPlannerFriChoices, dayToRecipeMap.get(FRIDAY));
    }

    private void restoreSelection(ChoiceBox<String> choiceBox, String selection) {
        if (selection != null && choiceBox.getItems().contains(selection)) {
            choiceBox.setValue(selection);
        }
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
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .method(method, HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        clearForm();
                        fetchRecipes();
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


    @FXML
    private void onEditRecipe() {
        String selected = recipesAllRecipesListview.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.isEmpty()) {
            Long recipeId = Long.parseLong(selected.split(" - ")[0]);
            loadRecipeForEditing(recipeId);
        }
    }
    private void loadRecipeForEditing(Long recipeId) {
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
                            tabPane.getSelectionModel().select(0);  // Assuming the editor tab index is 0
                        });
                    } catch (JsonProcessingException e) {
                        logger.error("Failed to parse recipe data", e);
                    }
                }).exceptionally(ex -> {
                    logger.error("Error retrieving recipe data", ex);
                    return null;
                });
    }

    private void clearForm() {
        createRecipeNameTextfield.clear();
        createRecipeDescriptionTextfield.clear();
        createRecipeIngredientsTextfield.clear();
        createRecipeStepsTextfield.clear();
    }

    private void fetchRecipes() {
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
                .map(recipe -> recipe.getId() + " - " +recipe.getTitle() + " - " + recipe.getIngredients())
                .toList();

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

        String username = getCurrentUsername();
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

    private void fetchPlannerRecipes() {
        String username = getCurrentUsername();
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
    public void deleteRecipe(ActionEvent actionEvent) {
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
                        Platform.runLater(() ->logger.warn("Failed to delete recipe, status code: {}",statusCode));
                    }
                }).exceptionally(ex -> {
                    logger.error("Error deleting recipe: {}",ex.getMessage());
                    return null;
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

        String username = getCurrentUsername();
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
                            fetchPlannerRecipes(); // Refresh the list
                        });
                    } else {
                        Platform.runLater(() -> logger.warn("Failed to remove recipe from planner, status code: {}", statusCode));
                    }
                }).exceptionally(ex -> {
                    logger.error("Exception occurred while removing recipe from planner", ex);
                    return null;
                });
    }

}