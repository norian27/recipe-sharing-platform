package com.koh0118;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecipeCardAppController {
    private static final Logger logger = Logger.getLogger(RecipeCardAppController.class.getName());


    @FXML private Text recipe_card_name;
    @FXML private TextArea recipe_card_description;
    @FXML private TextArea recipe_card_instructions;
    @FXML private TextArea recipe_card_ingredients;

    private Stage primaryStage;

    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Recipe Card App");
        showRecipeCardAppView();
    }

    private void showRecipeCardAppView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/recipe_card.fxml"));
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
            primaryStage.show();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load the Recipe card.", e);
        }
    }
    public void setRecipeDetails(RecipeDTO details) {
        try {
            recipe_card_name.setText(details.getTitle());
            recipe_card_description.setText(details.getDescription());
            recipe_card_instructions.setText(details.getInstructions());
            recipe_card_ingredients.setText(details.getIngredients());
        } catch (NullPointerException e) {
            logger.log(Level.SEVERE, "Component not initialized, or null values passed.", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting recipe details.", e);
        }
    }

}
