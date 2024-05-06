package com.koh0118;

import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class App extends Application {

    private final TextArea descriptionArea = new TextArea();
    private final TextField titleField = new TextField();
    private final TextArea ingredientsArea = new TextArea();
    private final TextArea instructionsArea = new TextArea();
    private final ListView<String> recipesListView = new ListView<>();
    private final ObservableList<String> recipes = FXCollections.observableArrayList();
    private final Label recipeDetails = new Label();

    @Override
    public void start(Stage primaryStage) {
        TabPane tabPane = new TabPane();

        Platform.runLater(this::fetchRecipes);

        // Setup UI components
        setupTabs(tabPane, primaryStage);

        Scene scene = new Scene(tabPane, 800, 400);
        primaryStage.setTitle("Recipe Sharing Platform");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupTabs(TabPane tabPane, Stage primaryStage) {
        Tab createTab = new Tab("Create Recipe", createRecipeForm());
        createTab.setClosable(false);

        Tab viewTab = new Tab("View Recipes", new ScrollPane(recipesListView));
        viewTab.setClosable(false);
        viewTab.setOnSelectionChanged(e -> {
            if (viewTab.isSelected()) {
                System.out.println("Fetching recipes on tab selection...");
                fetchRecipes();
            }
        });

        Tab detailsTab = new Tab("Recipe Details", new ScrollPane(recipeDetails));
        detailsTab.setClosable(false);
        detailsTab.setDisable(true);

        recipesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showRecipeDetails(newSelection);
                detailsTab.setDisable(false);
                tabPane.getSelectionModel().select(detailsTab);
            }
        });

        tabPane.getTabs().addAll(createTab, viewTab, detailsTab);
    }
    private VBox createRecipeForm() {
        Button submitButton = new Button("Submit Recipe");
        submitButton.setOnAction(e -> submitRecipe(titleField.getText(), descriptionArea.getText(), ingredientsArea.getText(), instructionsArea.getText()));

        return new VBox(10,
                new Label("Title"), titleField,
                new Label("Description"), descriptionArea,
                new Label("Ingredients"), ingredientsArea,
                new Label("Instructions"), instructionsArea,
                submitButton);
    }

    private void submitRecipe(String title, String description, String ingredients, String instructions) {
        String json = String.format("{\"title\":\"%s\", \"description\":\"%s\", \"ingredients\":\"%s\", \"instructions\":\"%s\"}",
                title, description, ingredients, instructions);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/recipes"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("Submitted. Status code: " + response.statusCode());
                    System.out.println("Response body: " + response.body());
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
        titleField.clear();
        descriptionArea.clear();
        ingredientsArea.clear();
        instructionsArea.clear();
    }

    private void fetchRecipes() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/recipes"))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("Fetch response status: " + response.statusCode());
                    System.out.println("Fetch response body: " + response.body());
                    if (response.statusCode() == 200) {
                        updateRecipesListView(response.body());
                    }
                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    private void updateRecipesListView(String json) {
        List<String> fetchedRecipes = parseRecipesFromJson(json);
        Platform.runLater(() -> {
            recipes.setAll(fetchedRecipes);
            recipesListView.setItems(recipes);
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
            System.err.println("Error parsing JSON: " + json);
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private void showRecipeDetails(String recipeTitle) {
        recipeDetails.setText("Details for: " + recipeTitle);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
