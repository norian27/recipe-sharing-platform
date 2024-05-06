package com.koh0118;

import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
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
    private final ListView<Recipe> recipesListView = new ListView<>();
    private final ObservableList<Recipe> recipes = FXCollections.observableArrayList();
    private final TextArea recipeDetails = new TextArea();
    private Stage mainStage;
    private User currentUser; // Add a User class field to manage the session

    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;
        primaryStage.setTitle("Recipe Sharing Platform");
        Scene loginScene = createLoginScene();
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }
    private Scene createLoginScene() {
        VBox loginForm = createLoginForm();
        return new Scene(loginForm, 300, 200);
    }

    private Scene createMainScene() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(createRecipesTab(), createPlannerTab());
        tabPane.getTabs().forEach(tab -> tab.setClosable(false));
        return new Scene(tabPane, 800, 600);
    }
    private Tab createRecipesTab() {
        VBox recipeForm = createRecipeForm();
        SplitPane recipeViewer = createRecipeViewer();
        VBox layout = new VBox(recipeForm, recipeViewer);
        layout.setSpacing(10);
        Tab recipeTab = new Tab("Recipes", layout);
        return recipeTab;
    }

    private Tab createPlannerTab() {
        VBox plannerView = new VBox(new Label("Your Planner"));
        Tab plannerTab = new Tab("Planner", new ScrollPane(plannerView));
        return plannerTab;
    }
    private VBox createRecipeForm() {
        TextField titleField = new TextField();
        TextArea descriptionArea = new TextArea();
        TextArea ingredientsArea = new TextArea();
        TextArea instructionsArea = new TextArea();
        Button submitButton = new Button("Submit Recipe");
        submitButton.setOnAction(e -> submitRecipe(titleField.getText(), descriptionArea.getText(), ingredientsArea.getText(), instructionsArea.getText()));
        return new VBox(10,
                new Label("Title"), titleField,
                new Label("Description"), descriptionArea,
                new Label("Ingredients"), ingredientsArea,
                new Label("Instructions"), instructionsArea,
                submitButton);
    }

    private SplitPane createRecipeViewer() {
        ListView<Recipe> recipesListView = new ListView<>(recipes);
        TextArea recipeDetails = new TextArea();
        recipesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showRecipeDetails(newSelection);
            }
        });
        SplitPane splitPane = new SplitPane(new VBox(new Label("Recipes List"), recipesListView), new VBox(new Label("Recipe Details"), recipeDetails));
        splitPane.setDividerPositions(0.3);
        return splitPane;
    }

    private VBox createListView() {
        VBox listBox = new VBox(new Label("Recipes"), new ScrollPane(recipesListView));
        listBox.setSpacing(10);
        recipesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showRecipeDetails(newSelection);  // Assuming `recipeDetails` is the TextArea you want to use
            }
        });
        return listBox;
    }

    private VBox createDetailView() {
        VBox detailBox = new VBox(new Label("Recipe Details"), recipeDetails);
        recipeDetails.setEditable(false);
        detailBox.setSpacing(10);
        return detailBox;
    }
    private VBox createLoginForm() {
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Button loginButton = new Button("Login");
        Button registerButton = new Button("Register");
        Label messageLabel = new Label();

        loginButton.setOnAction(e -> handleLogin(usernameField.getText(), passwordField.getText(), messageLabel));
        registerButton.setOnAction(e -> handleRegistration(usernameField.getText(), passwordField.getText(), messageLabel));

        return new VBox(10,
                new Label("Username"), usernameField,
                new Label("Password"), passwordField,
                loginButton, registerButton, messageLabel);
    }

    private void handleLogin(String username, String password, Label messageLabel) {
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both username and password.");
            System.out.println("Login attempt failed: Username or password is empty.");
            return;
        }

        String json = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);
        System.out.println("Attempting login for username: " + username + " with payload: " + json);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/users/login")) // Adjust the port if needed
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("Login response: " + response.statusCode() + " " + response.body());
                    if (response.statusCode() == 200) {
                        Platform.runLater(() -> {
                            currentUser = new User(username); // assuming User constructor with username
                            mainStage.setScene(createMainScene());
                            messageLabel.setText("Login successful!");
                        });
                    } else {
                        Platform.runLater(() -> messageLabel.setText("Invalid credentials. Status: " + response.statusCode()));
                    }
                }).exceptionally(ex -> {
                    System.out.println("Login failed with exception: " + ex.getMessage());
                    Platform.runLater(() -> messageLabel.setText("Login failed. Error: " + ex.getMessage()));
                    return null;
                });
    }

    private void handleRegistration(String username, String password, Label messageLabel) {
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both username and password to register.");
            System.out.println("Registration failed: Username or password is empty.");
            return;
        }

        User newUser = new User(username);
        newUser.setPassword(password); // Ensure this is aligned with how the server expects to receive the password

        try {
            String json = new ObjectMapper().writeValueAsString(newUser);
            System.out.println("Attempting registration with JSON: " + json);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("Server response: " + response.statusCode() + " " + response.body());
                        Platform.runLater(() -> {
                            if (response.statusCode() == 201) {
                                messageLabel.setText("Registration successful!");
                                mainStage.setScene(createMainScene());
                            } else {
                                messageLabel.setText("Registration failed. Status: " + response.statusCode() + " - " + response.body());
                            }
                        });
                    }).exceptionally(ex -> {
                        System.out.println("Registration failed with exception: " + ex.getMessage());
                        Platform.runLater(() -> messageLabel.setText("Registration failed. Error: " + ex.getMessage()));
                        return null;
                    });
        } catch (JsonProcessingException e) {
            System.out.println("JsonProcessingException during registration: " + e.getMessage());
            messageLabel.setText("Failed to serialize user data: " + e.getMessage());
        }
    }


    private void submitRecipe(String title, String description, String ingredients, String instructions) {
        String json = String.format("{\"title\":\"%s\", \"description\":\"%s\", \"ingredients\":\"%s\", \"instructions\":\"%s\"}",
                title, description, ingredients, instructions);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/recipes"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 201) {
                        clearForm();
                        // Introduce a delay or a more robust update mechanism
                        try {
                            Thread.sleep(1000); // not recommended for real applications
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
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

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        updateRecipesListView(response.body());
                    } else {
                        System.err.println("Failed to fetch recipes. Status code: " + response.statusCode());
                        System.err.println("Response: " + response.body());
                    }
                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    System.err.println("Exception during fetch: " + ex.getMessage());
                    return null;
                });
    }
    private void updateRecipesListView(String json) {
        try {
            List<Recipe> recipeList = new ObjectMapper().readValue(json, new TypeReference<List<Recipe>>() {});
            Platform.runLater(() -> {
                recipes.setAll(recipeList);
                System.out.println("Recipes updated: " + recipes.size());
            });
        } catch (JsonProcessingException e) {
            System.err.println("Error parsing recipes JSON");
            e.printStackTrace();
        }
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

    private void setupListView() {
        recipesListView.setItems(recipes);
        recipesListView.setCellFactory(listView -> new ListCell<Recipe>() {
            @Override
            protected void updateItem(Recipe recipe, boolean empty) {
                super.updateItem(recipe, empty);
                if (empty || recipe == null) {
                    setText(null);
                } else {
                    setText(recipe.getTitle() + " - " + recipe.getIngredients());
                }
            }
        });
    }



    private void showRecipeDetails(Recipe recipe) {
        StringBuilder details = new StringBuilder();
        details.append("Title: ").append(recipe.getTitle()).append("\n");
        details.append("Description: ").append(recipe.getDescription()).append("\n\n");
        details.append("Ingredients:\n").append(recipe.getIngredients()).append("\n\n");
        details.append("Instructions:\n");
        String[] instructionSteps = recipe.getInstructions().split(";");
        int stepNumber = 1;
        for (String step : instructionSteps) {
            details.append(stepNumber++).append(". ").append(step.trim()).append("\n");
        }
        recipeDetails.setText(details.toString());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
