package com.koh0118;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ResourceBundle;

public class AppController {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);

    @FXML private TabPane tabPane;
    @FXML private Tab createRecipeTab;
    @FXML private Tab recipesTab;
    @FXML private Tab plannerTab;
    @FXML private Tab planTab;

    private RecipesController recipesController;
    private CreateRecipeController createRecipeController;
    private PlannerController plannerController;
    private PlanController planController;


    @Getter
    private static String currentUsername;

    void setCurrentUsername(String username) {
        currentUsername = username;
    }

    @FXML
    public void initialize() {
        loadTabContent();
        setupTabChangeListener();
    }

    private void loadTabContent() {
        ResourceBundle resourceBundle = LocaleManager.getInstance().getResourceBundle();

        try {
            // Load CreateRecipe tab
            createRecipeTab.setContent(loadFXML("/create_recipe_tab.fxml", resourceBundle));

            // Load Recipes tab
            recipesTab.setContent(loadFXML("/recipes_tab.fxml", resourceBundle));

            // Load Planner tab
            plannerTab.setContent(loadFXML("/planner_tab.fxml", resourceBundle));

            // Load Plan tab
            planTab.setContent(loadFXML("/plan_tab.fxml", resourceBundle));

        } catch (IOException e) {
            logger.error("Error loading tab content: {}", e.getMessage(), e);
        }
    }
    private Node loadFXML(String fxmlPath, ResourceBundle resourceBundle) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath), resourceBundle);
        Node node = loader.load();
        Object controller = loader.getController();

        if (controller instanceof RecipesController) {
            recipesController = (RecipesController) controller;
            recipesController.setCreateRecipeController(createRecipeController);
        } else if (controller instanceof CreateRecipeController) {
            createRecipeController = (CreateRecipeController) controller;
        } else if (controller instanceof PlannerController) {
            plannerController = (PlannerController) controller;
        } else if (controller instanceof PlanController) {
            planController = (PlanController) controller;
        }
        return node;
    }

    private void setupTabChangeListener() {
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == recipesTab && recipesController != null) {
                recipesController.initialize();
            }
            if (newTab == plannerTab && plannerController != null)
            {
                plannerController.initialize();
            }
            if (newTab == planTab && planController != null)
            {
                planController.initialize();
            }
        });
    }
}
