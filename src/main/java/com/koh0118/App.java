package com.koh0118;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {

    private static final Logger logger = Logger.getLogger(App.class.getName());

    @Override
    public void start(Stage primaryStage) {
        try {
            ResourceBundle resourceBundle = LocaleManager.getInstance().getResourceBundle();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login_screen.fxml"), resourceBundle);
            Parent root = loader.load();
            LoginAppController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);

            Scene scene = new Scene(root);
            primaryStage.setTitle(resourceBundle.getString("loginScreen.title"));
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load the login screen.", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
