package com.koh0118;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateRecipeController {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final Logger logger = LoggerFactory.getLogger(CreateRecipeController.class);
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML private TextField createRecipeNameTextfield;
    @FXML private TextArea createRecipeDescriptionTextfield;
    @FXML private TextField createRecipeIngredientsTextfield;
    @FXML private TextArea createRecipeStepsTextfield;
    @FXML private Button create_recipe_submit;

    private boolean isEditMode = false;
    private Long editingRecipeId = null;

    @FXML
    public void initialize() {
    }

    @FXML
    private void submitRecipe(ActionEvent event) {
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
