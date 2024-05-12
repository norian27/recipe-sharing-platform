package com.koh0118;

import javafx.event.ActionEvent;
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

public class LoginAppController {
    @FXML private TextField login_username_text;
    @FXML private TextField login_password_text;
    @FXML private Text login_status;

    private Stage primaryStage;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        System.out.println("Primary Stage set");
    }

    @FXML
    private void handleLogin() {
        String username = login_username_text.getText();
        String password = login_password_text.getText();
        if (username.isEmpty() || password.isEmpty()) {
            login_status.setText("Username and password are required");
            return;
        }

        String json = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("Response status: " + response.statusCode());
                    System.out.println("Response body: " + response.body());
                    return response;
                })
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Platform.runLater(() -> {
                            login_status.setText("Login successful");
                            loadMainApplicationView();
                        });
                    } else {
                        Platform.runLater(() -> login_status.setText("Login failed: " + response.body()));
                    }
                }).exceptionally(ex -> {
                    Platform.runLater(() -> login_status.setText("Error: " + ex.getMessage()));
                    ex.printStackTrace();
                    return null;
                });
    }

    @FXML
    private void handleRegister() {
        System.out.println("Attempting to register");
        String username = login_username_text.getText();
        String password = login_password_text.getText();
        if (username.isEmpty() || password.isEmpty()) {
            login_status.setText("Username and password are required");
            return;
        }

        String json = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .thenAccept(statusCode -> {
                    if (statusCode == 201) {
                        Platform.runLater(() -> {
                            login_status.setText("Registration successful");
                            if (primaryStage != null) {
                                loadMainApplicationView();
                            } else {
                                System.err.println("Primary stage is null");
                            }
                        });
                    } else {
                        Platform.runLater(() -> login_status.setText("Registration failed: " + statusCode));
                    }
                }).exceptionally(ex -> {
                    Platform.runLater(() -> login_status.setText("Error: " + ex.getMessage()));
                    ex.printStackTrace();
                    return null;
                });
    }

    private void loadMainApplicationView() {
        if (primaryStage == null) {
            System.err.println("Primary stage is not set. Cannot load the main view.");
            return;
        }
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/application_layout.fxml"));
                Parent root = loader.load();
                AppController mainAppController = loader.getController();
                mainAppController.setCurrentUsername(login_username_text.getText());
                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.setTitle("Main Application");
                primaryStage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
