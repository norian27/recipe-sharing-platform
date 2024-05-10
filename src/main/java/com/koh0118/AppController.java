package com.koh0118;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.scene.text.Text;

public class AppController {
    @FXML private TextField create_recipe_name_textfield;
    @FXML private TextArea create_recipe_description_textfield;
    @FXML private TextField create_recipe_ingredients_textfield;
    @FXML private TextArea create_recipe_steps_textfield;
    @FXML private ListView<String> recipes_all_recipes_listview;
    @FXML private TextArea recipes_recipe_details;
    @FXML private TextField recipes_recipe_name;
    @FXML private TextField login_username_text;
    @FXML private TextField login_password_text;
    @FXML private Text login_status;

    private ObservableList<String> recipes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        recipes_all_recipes_listview.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            showRecipeDetails();
        });
    }

    @FXML
    private void submitRecipe() {
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

    @FXML
    private void updateRecipesListView(String json) {
        List<String> fetchedRecipes = parseRecipesFromJson(json);
        Platform.runLater(() -> {
            recipes.setAll(fetchedRecipes);
            recipes_all_recipes_listview.setItems(recipes);
        });
    }

    private List<String> parseRecipesFromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Recipe> recipeList = mapper.readValue(json, new TypeReference<List<Recipe>>() {});
            return recipeList.stream()
                    .map(recipe -> recipe.getTitle() + " - " + recipe.getIngredients())
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @FXML
    private void showRecipeDetails() {
        String selectedRecipe = recipes_all_recipes_listview.getSelectionModel().getSelectedItem();
        if (selectedRecipe != null && !selectedRecipe.isEmpty()) {
            String[] parts = selectedRecipe.split(" - ", 2); // Splits into two parts at the first occurrence of " - "
            if (parts.length == 2) {
                recipes_recipe_name.setText(parts[0]); // Set the title in the TextField
                recipes_recipe_details.setText(parts[1]); // Set the ingredients in the TextArea
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
    private void handleLogin() {
        String username = login_username_text.getText();
        String password = login_password_text.getText();
        if (username.isEmpty() || password.isEmpty()) {
            login_status.setText("Username and password are required");
            return;
        }

        String json = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    if (statusCode == 200) {
                        Platform.runLater(() -> {
                            login_status.setText("Login successful");
                            // Additional logic to change UI to the main application view
                        });
                    } else {
                        Platform.runLater(() -> login_status.setText("Login failed"));
                    }
                }).exceptionally(ex -> {
                    Platform.runLater(() -> login_status.setText("Error: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void handleRegister() {
        // Similar structure to handleLogin(), but points to a registration endpoint
        // e.g., http://localhost:8080/register
    }


    public void addRecipeToPlan(ActionEvent actionEvent) {
    }

    public void editRecipe(ActionEvent actionEvent) {
    }

    public void deleteRecipe(ActionEvent actionEvent) {
    }

    public void removeFromPlanner(ActionEvent actionEvent) {
    }
}
