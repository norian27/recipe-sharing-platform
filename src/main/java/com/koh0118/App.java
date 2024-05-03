package com.koh0118;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        TabPane tabPane = new TabPane();

        // Tab for creating recipes
        Tab createTab = new Tab("Create Recipe");
        createTab.setClosable(false);
        createTab.setContent(createRecipeForm());

        // Tab for viewing all recipes
        Tab viewTab = new Tab("View Recipes");
        viewTab.setClosable(false);
        viewTab.setContent(new Label("List of recipes...")); // Placeholder

        // Tab for planner
        Tab plannerTab = new Tab("Planner");
        plannerTab.setClosable(false);
        plannerTab.setContent(new Label("Planner details...")); // Placeholder

        tabPane.getTabs().addAll(createTab, viewTab, plannerTab);

        Scene scene = new Scene(tabPane, 400, 300);
        primaryStage.setTitle("Recipe Sharing Platform");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createRecipeForm() {
        TextField titleField = new TextField();
        TextArea descriptionArea = new TextArea();
        Button submitButton = new Button("Submit Recipe");
        submitButton.setOnAction(e -> submitRecipe(titleField.getText(), descriptionArea.getText()));

        return new VBox(10, new Label("Title"), titleField, new Label("Description"), descriptionArea, submitButton);
    }

    private void submitRecipe(String title, String description) {
        String json = String.format("{\"title\":\"%s\",\"description\":\"%s\"}", title, description);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/recipes"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> System.out.println("Response status code: " + response.statusCode()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
