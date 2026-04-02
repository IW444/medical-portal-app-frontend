package edu.secourse.medicalportalgui.controller;

import edu.secourse.medicalportalgui.model.User;
import edu.secourse.medicalportalgui.model.Role; // Ensure you have this Enum
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.scene.image.Image;

public class CreateUserController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        // Add roles to the dropdown
        roleComboBox.getItems().addAll("ADMIN", "DOCTOR", "PATIENT");
        roleComboBox.setValue("PATIENT"); // Default value

    }


    @FXML
    private void handleSave() {
        if (isInputValid()) {
            try {
                // Create a User object to hold the form data
                User newUser = new User();
                newUser.setFirstName(firstNameField.getText());
                newUser.setLastName(lastNameField.getText());
                newUser.setUsername(usernameField.getText());
                newUser.setPassword(passwordField.getText());
                newUser.setRole(roleComboBox.getValue());

                String json = mapper.writeValueAsString(newUser);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/users"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    closeWindow();
                } else if (response.statusCode() == 409) {
                    // Highlight the username field in red
                    usernameField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    showError("Username Taken",
                            "The username \"" + usernameField.getText() + "\" is already in use.\nPlease choose a different username.");
                } else {
                    showError("Server Error", "Could not create user. Status: " + response.statusCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Connection Error", "Could not reach the server.");
            }
        }
    }

    private boolean isInputValid() {
        if (firstNameField.getText().isEmpty() || usernameField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            showError("Validation Error", "Please fill in all required fields.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        stage.close();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}