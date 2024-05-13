package com.koh0118;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class PlanController {

    private static final Logger logger = LoggerFactory.getLogger(PlanController.class);
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML private ListView<String> planListViewRecipes;

    private final Map<String, RecipeDTO> dayToRecipeDetails = new HashMap<>();

    @FXML
    public void initialize() {
        fetchWeeklyPlan();
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
                    dayToRecipeDetails.put(day, recipe);
                }
            });
            Platform.runLater(() -> planListViewRecipes.setItems(items));
        } catch (JsonProcessingException e) {
            logger.error("Error parsing weekly plan", e);
        }
    }

    @FXML
    private void showRecipeDetail() {
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
            ResourceBundle resourceBundle = LocaleManager.getInstance().getResourceBundle();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/recipe_card.fxml"), resourceBundle);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(resourceBundle.getString("recipeDetail.title"));
            stage.setScene(new Scene(root));

            RecipeCardAppController controller = loader.getController();
            controller.setRecipeDetails(details);

            stage.show();
        } catch (IOException e) {
            logger.error("Error loading the recipe card view: {}", e.getMessage(), e);
        }
    }
}
