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
    @FXML private TextArea recipe_card_descrption;
    @FXML private TextArea recipe_card_insctructions;
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

    public void setRecipeDetails(Object recipeDetailsObject) {
        if (recipeDetailsObject instanceof RecipeDTO) {
            RecipeDTO recipe = (RecipeDTO) recipeDetailsObject;
            recipe_card_name.setText(recipe.getTitle());
            recipe_card_descrption.setText(recipe.getDescription());
            recipe_card_ingredients.setText(recipe.getIngredients());
            recipe_card_insctructions.setText(recipe.getInstructions());
        }
    }

}
