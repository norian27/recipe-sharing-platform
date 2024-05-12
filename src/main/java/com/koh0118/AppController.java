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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class AppController {
    Logger logger = Logger.getLogger(getClass().getName());

    @FXML private TextField create_recipe_name_textfield;
    @FXML private TextArea create_recipe_description_textfield;
    @FXML private TextField create_recipe_ingredients_textfield;
    @FXML private TextArea create_recipe_steps_textfield;
    @FXML private ListView<String> recipes_all_recipes_listview;
    @FXML private TextArea recipes_recipe_details;
    @FXML private TextField recipes_recipe_name;
    @FXML private ListView<String> planner_recipes_in_planner_listview;
    @FXML private TabPane tabPane;
    @FXML
    private ChoiceBox<String> planner_recipes_in_planner_mon_choices;
    @FXML
    private ChoiceBox<String> planner_recipes_in_planner_tue_choices;
    @FXML
    private ChoiceBox<String> planner_recipes_in_planner_wed_choices;
    @FXML
    private ChoiceBox<String> planner_recipes_in_planner_thu_choices;
    @FXML
    private ChoiceBox<String> planner_recipes_in_planner_fri_choices;

    @FXML
    private Button btnShowRecipeDetail;
    @FXML
    private ListView<String> planListViewRecipes;

    private Map<String, String> dayToRecipeMap = new HashMap<>();
    private Map<String, RecipeDTO> dayToRecipeDetails = new HashMap<>();


    private String currentUsername;
    private String getCurrentUsername() {
        return currentUsername; // Placeholder for now
    }
    void setCurrentUsername(String username) {
        currentUsername = username;
    }
    @FXML
    public void initialize() {
        recipes_all_recipes_listview.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            showRecipeDetails();
        });
        fetchRecipes();
        logger.info(("Username:"+getCurrentUsername()));
        setupTabChangeListener();
        setupChoiceBoxListeners();

    }
    @FXML
    private void showRecipeDetail(ActionEvent event) {
        String selected = planListViewRecipes.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No recipe selected");
            return;
        }
        String day = selected.split(":")[0].trim();
        RecipeDTO details = dayToRecipeDetails.get(day); // Directly retrieve as RecipeDTO
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
            System.out.println("Error loading the recipe card view: " + e.getMessage());
            e.printStackTrace();
        }
    }




    private void setupChoiceBoxListeners() {
        planner_recipes_in_planner_mon_choices.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> setRecipeForDay("monday", newVal, planner_recipes_in_planner_mon_choices));
        planner_recipes_in_planner_tue_choices.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> setRecipeForDay("tuesday", newVal, planner_recipes_in_planner_tue_choices));
        planner_recipes_in_planner_wed_choices.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> setRecipeForDay("wednesday", newVal, planner_recipes_in_planner_wed_choices));
        planner_recipes_in_planner_thu_choices.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> setRecipeForDay("thursday", newVal, planner_recipes_in_planner_thu_choices));
        planner_recipes_in_planner_fri_choices.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> setRecipeForDay("friday", newVal, planner_recipes_in_planner_fri_choices));
    }
    private void setRecipeForDay(String day, String recipeDetails, ChoiceBox<String> choiceBox) {
        if (recipeDetails == null || recipeDetails.isEmpty()) return;
        Long recipeId = Long.parseLong(recipeDetails.split(" - ")[0]);

        String username = getCurrentUsername();
        if (username == null) {
            System.out.println("Username is not available");
            return;
        }

        String requestUri = String.format("http://localhost:8080/planners/%s/setRecipeForDay/%d/%s", username, recipeId, day);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUri))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    if (statusCode == 200) {
                        Platform.runLater(() -> {
                            System.out.println("Recipe set for " + day + " successfully");
                            choiceBox.getSelectionModel().select(recipeDetails);  // Re-select the item to ensure it stays visible
                        });
                    } else {
                        Platform.runLater(() -> System.out.println("Failed to set recipe for " + day + ", status code: " + statusCode));
                    }
                }).exceptionally(ex -> {
                    System.out.println("Exception occurred while setting recipe for " + day + ": " + ex.getMessage());
                    ex.printStackTrace();
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
                fetchWeeklyPlan();  // This should fetch the weekly plan and update listViewRecipes
            }
        });
    }
    private void fetchWeeklyPlan() {
        String username = getCurrentUsername();
        if (username == null) {
            System.out.println("Username is not set. Please ensure the user is logged in.");
            return;
        }

        String uri = String.format("http://localhost:8080/planners/getRecipesForWeek/%s", username);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::updatePlanListView)
                .exceptionally(ex -> {
                    System.out.println("Failed to fetch weekly plan: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }
    private void updatePlanListView(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, RecipeDTO> weeklyPlan = mapper.readValue(json, new TypeReference<Map<String, RecipeDTO>>() {});
            ObservableList<String> items = FXCollections.observableArrayList();
            weeklyPlan.forEach((day, recipe) -> {
                if (recipe != null) {
                    items.add(day + ": " + recipe.getTitle());
                    dayToRecipeDetails.put(day, recipe);
                }
            });
            Platform.runLater(() -> planListViewRecipes.setItems(items));
        } catch (JsonProcessingException e) {
            System.out.println("Error parsing weekly plan: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void restoreChoiceBoxSelections() {
        restoreSelection(planner_recipes_in_planner_mon_choices, dayToRecipeMap.get("monday"));
        restoreSelection(planner_recipes_in_planner_tue_choices, dayToRecipeMap.get("tuesday"));
        restoreSelection(planner_recipes_in_planner_wed_choices, dayToRecipeMap.get("wednesday"));
        restoreSelection(planner_recipes_in_planner_thu_choices, dayToRecipeMap.get("thursday"));
        restoreSelection(planner_recipes_in_planner_fri_choices, dayToRecipeMap.get("friday"));
    }

    private void restoreSelection(ChoiceBox<String> choiceBox, String selection) {
        if (selection != null && choiceBox.getItems().contains(selection)) {
            choiceBox.setValue(selection);
        }
    }
    @FXML
    private void submitRecipe() {
        logger.info("Submitting recipe");
        String json = String.format("{\"title\":\"%s\", \"description\":\"%s\", \"ingredients\":\"%s\", \"instructions\":\"%s\"}",
                create_recipe_name_textfield.getText(),
                create_recipe_description_textfield.getText(),
                create_recipe_ingredients_textfield.getText(),
                create_recipe_steps_textfield.getText());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/recipes"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 201) {
                        clearForm();
                        fetchRecipes();
                        logger.info("Recipe submitted successfully");
                    }
                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    private void clearForm() {
        create_recipe_name_textfield.clear();
        create_recipe_description_textfield.clear();
        create_recipe_ingredients_textfield.clear();
        create_recipe_steps_textfield.clear();
    }

    private void fetchRecipes() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/recipes"))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        updateRecipesListView(response.body());
                    }
                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    private void updateRecipesListView(String json) {
        List<String> displayedRecipes = parseRecipesFromJson(json).stream()
                .map(recipe -> recipe.getId() + " - " +recipe.getTitle() + " - " + recipe.getIngredients())
                .collect(Collectors.toList());

        Platform.runLater(() -> {
            recipes_all_recipes_listview.setItems(FXCollections.observableArrayList(displayedRecipes));
        });
    }

    private List<Recipe> parseRecipesFromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, new TypeReference<List<Recipe>>() {});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @FXML
    private void showRecipeDetails() {
        String selectedRecipe = recipes_all_recipes_listview.getSelectionModel().getSelectedItem();
        if (selectedRecipe != null && !selectedRecipe.isEmpty()) {
            String[] parts = selectedRecipe.split(" - ", 4); // Splits into two parts at the first occurrence of " - "
            if (parts.length == 4) {
                recipes_recipe_name.setText(parts[1]); // Set the title in the TextField
                recipes_recipe_details.setText(parts[2]); // Set the ingredients in the TextArea
            } else {
                recipes_recipe_name.setText(selectedRecipe); // Fallback in case there is no " - " in the string
                recipes_recipe_details.clear();
            }
        } else {
            recipes_recipe_name.clear();
            recipes_recipe_details.clear();
        }
    }


    @FXML
    private void addRecipeToPlan(ActionEvent actionEvent) {
        String selectedEntry = recipes_all_recipes_listview.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            System.out.println("No recipe selected");
            return;
        }

        Long recipeId;
        try {
            recipeId = Long.parseLong(selectedEntry.split(" - ")[0]);
        } catch (NumberFormatException e) {
            System.out.println("Failed to extract recipe ID from the selection: " + selectedEntry);
            return;
        }
        System.out.println("Sending recipe ID: " + recipeId);

        String username = getCurrentUsername(); // This method should return the currently logged-in user's username
        if (username == null) {
            System.out.println("Username is not available");
            return;
        }

        // Update the URI with both username and recipe ID as path parameters
        String requestUri = String.format("http://localhost:8080/planners/%s/addRecipe/%d", username, recipeId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUri))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody()) // No body needed since ID is in the URL
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    System.out.println("Received status code: " + statusCode);
                    if (statusCode == 200) {
                        Platform.runLater(() -> System.out.println("Recipe added to planner successfully"));
                    } else {
                        Platform.runLater(() -> System.out.println("Failed to add recipe to planner, status code: " + statusCode));
                    }
                }).exceptionally(ex -> {
                    System.out.println("Exception occurred while adding recipe to planner: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    private void fetchPlannerRecipes() {
        String username = getCurrentUsername();
        if (username == null) {
            System.out.println("Username is not set. Please ensure the user is logged in.");
            return;
        }

        String uri = String.format("http://localhost:8080/planners/getPlannerRecipes/%s", username);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::updatePlannerRecipesListView)
                .exceptionally(ex -> {
                    System.out.println("Failed to fetch planner recipes: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    private void updatePlannerRecipesListView(String json) {
        List<String> plannerRecipes = parseRecipesFromJson(json).stream()
                .map(recipe -> recipe.getId() + " - " + recipe.getTitle())
                .collect(Collectors.toList());

        Platform.runLater(() -> {
            planner_recipes_in_planner_listview.setItems(FXCollections.observableArrayList(plannerRecipes));
            updateChoiceBoxes(plannerRecipes);
        });
    }

    private void updateChoiceBoxes(List<String> recipes) {
        ObservableList<String> recipeOptions = FXCollections.observableArrayList(recipes);
        updateChoiceBox(planner_recipes_in_planner_mon_choices, recipeOptions, "monday");
        updateChoiceBox(planner_recipes_in_planner_tue_choices, recipeOptions, "tuesday");
        updateChoiceBox(planner_recipes_in_planner_wed_choices, recipeOptions, "wednesday");
        updateChoiceBox(planner_recipes_in_planner_thu_choices, recipeOptions, "thursday");
        updateChoiceBox(planner_recipes_in_planner_fri_choices, recipeOptions, "friday");
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

    public void editRecipe(ActionEvent actionEvent) {
    }

    @FXML
    public void deleteRecipe(ActionEvent actionEvent) {
        String selected = recipes_all_recipes_listview.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No recipe selected to delete.");
            return;
        }

        Long recipeId;
        try {
            recipeId = Long.parseLong(selected.split(" - ")[0]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid format for recipe ID.");
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

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    if (statusCode == 200) {
                        Platform.runLater(() -> {
                            recipes_all_recipes_listview.getItems().removeIf(item -> item.startsWith(recipeId + " -"));
                            System.out.println("Recipe deleted successfully.");
                        });
                    } else {
                        Platform.runLater(() -> System.out.println("Failed to delete recipe, status code: " + statusCode));
                    }
                }).exceptionally(ex -> {
                    System.out.println("Error deleting recipe: " + ex.getMessage());
                    return null;
                });
    }


    @FXML
    private void removeFromPlanner(ActionEvent actionEvent) {
        String selectedEntry = planner_recipes_in_planner_listview.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            System.out.println("No recipe selected in planner");
            return;
        }

        Long recipeId;
        try {
            recipeId = Long.parseLong(selectedEntry.split(" - ")[0]);
        } catch (NumberFormatException e) {
            System.out.println("Failed to extract recipe ID from the planner selection: " + selectedEntry);
            return;
        }

        String username = getCurrentUsername();
        if (username == null) {
            System.out.println("Username is not available");
            return;
        }

        String requestUri = String.format("http://localhost:8080/planners/%s/deleteRecipe/%d", username, recipeId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUri))
                .DELETE() // Use DELETE method
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    System.out.println("Received status code: " + statusCode);
                    if (statusCode == 200) {
                        Platform.runLater(() -> {
                            System.out.println("Recipe removed from planner successfully");
                            fetchPlannerRecipes(); // Refresh the list
                        });
                    } else {
                        Platform.runLater(() -> System.out.println("Failed to remove recipe from planner, status code: " + statusCode));
                    }
                }).exceptionally(ex -> {
                    System.out.println("Exception occurred while removing recipe from planner: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

}
