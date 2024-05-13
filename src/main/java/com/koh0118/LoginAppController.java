package com.koh0118;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginAppController {

    @FXML private TextField loginUsernameText;
    @FXML private TextField loginPasswordText;
    @FXML private Text loginStatus;

    private Stage primaryStage;

    private static final Logger logger = LoggerFactory.getLogger(LoginAppController.class);

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        logger.info("Primary Stage set");
    }


    @FXML
    private void handleLogin() {
        String username = loginUsernameText.getText();
        String password = loginPasswordText.getText();
        if (username.isEmpty() || password.isEmpty()) {
            loginStatus.setText("Username and password are required");
            return;
        }

        String json = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    logger.info("Response status: {}",response.statusCode());
                    logger.info("Response body: {}",response.body());
                    return response;
                })
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Platform.runLater(() -> {
                            loginStatus.setText("Login successful");
                            loadMainApplicationView();
                        });
                    } else {
                        Platform.runLater(() -> loginStatus.setText("Login failed: " + response.body()));
                    }
                }).exceptionally(ex -> {
                    Platform.runLater(() -> loginStatus.setText("Error: " + ex.getMessage()));
                    logger.error("Error: {}", ex.getMessage());
                    return null;
                });
    }

    @FXML
    private void handleRegister() {
        logger.info("Attempting to register");
        String username = loginUsernameText.getText();
        String password = loginPasswordText.getText();
        if (username.isEmpty() || password.isEmpty()) {
            loginStatus.setText("Username and password are required");
            return;
        }

        String json = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    if (statusCode == 201) {
                        Platform.runLater(() -> {
                            loginStatus.setText("Registration successful");
                            if (primaryStage != null) {
                                loadMainApplicationView();
                            } else {
                                logger.error("Primary stage is null");
                            }
                        });
                    } else {
                        Platform.runLater(() -> loginStatus.setText("Registration failed: " + statusCode));
                    }
                }).exceptionally(ex -> {
                    Platform.runLater(() -> loginStatus.setText("Error: " + ex.getMessage()));
                    logger.error("Error: {}", ex.getMessage());
                    return null;
                });
    }

    private void loadMainApplicationView() {
        if (primaryStage == null) {
            logger.error("Primary stage is null");
            return;
        }
        Platform.runLater(() -> {
            try {
                ResourceBundle resourceBundle = LocaleManager.getInstance().getResourceBundle();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/application_layout.fxml"), resourceBundle);
                Parent root = loader.load();
                AppController mainAppController = loader.getController();
                mainAppController.setCurrentUsername(loginUsernameText.getText());
                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.setTitle(resourceBundle.getString("app.title"));
                primaryStage.show();
            } catch (IOException e) {
                logger.error("Failed to load main application view: {}", e.getMessage());
            }
        });
    }
}
