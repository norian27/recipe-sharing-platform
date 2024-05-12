package com.koh0118;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;

public class RecipeCardAppController {

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/recipe_card.fxml"));  // Ensure the path is correct
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void setRecipeDetails(RecipeDTO details) {
        try {
            recipe_card_name.setText(details.getTitle());
            recipe_card_description.setText(details.getDescription());
            recipe_card_instructions.setText(details.getInstructions());
            recipe_card_ingredients.setText(details.getIngredients());
        } catch (NullPointerException e) {
            System.err.println("Component not initialized.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error setting recipe details.");
            e.printStackTrace();
        }
    }

}
