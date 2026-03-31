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
    private User existingUser;

    @FXML
    public void initialize() {
        // Add roles to the dropdown
        roleComboBox.getItems().addAll("ADMIN", "DOCTOR", "PATIENT");
        roleComboBox.setValue("PATIENT"); // Default value

    }

    public void setExistingUser(User user) {
        this.existingUser = user;
        // Pre-fill the form
        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        usernameField.setText(user.getUsername());
        roleComboBox.setValue(user.getRole());
    }


    @FXML
    private void handleSave() {
        if (isInputValid()) {
            try {
                // 1. Create the data object
                User userToSave = new User();
                userToSave.setFirstName(firstNameField.getText());
                userToSave.setLastName(lastNameField.getText());
                userToSave.setUsername(usernameField.getText());
                userToSave.setRole(roleComboBox.getValue());

                // 2. Only set password if the Admin actually typed one
                String passInput = passwordField.getText();
                if (passInput != null && !passInput.isEmpty()) {
                    userToSave.setPassword(passInput);
                }

                // 3. Determine if this is a New User (POST) or Update (PUT)
                String url = "http://localhost:8080/users";
                String method = "POST";

                if (existingUser != null) {
                    url += "/" + existingUser.getUserId();
                    method = "PUT";
                }

                // 4. Send the request
                String json = mapper.writeValueAsString(userToSave);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // 5. Handle the result
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    closeWindow();
                } else {
                    showError("Server Error", "Operation failed. Status: " + response.statusCode());
                }

            } catch (Exception e) {
                e.printStackTrace();
                showError("Connection Error", "Could not reach the server.");
            }
        }
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (firstNameField.getText() == null || firstNameField.getText().isEmpty()) {
            errorMessage += "No valid first name!\n";
        }
        if (lastNameField.getText() == null || lastNameField.getText().isEmpty()) {
            errorMessage += "No valid last name!\n";
        }
        if (usernameField.getText() == null || usernameField.getText().isEmpty()) {
            errorMessage += "No valid username!\n";
        }
        if (roleComboBox.getValue() == null) {
            errorMessage += "No valid role selected!\n";
        }

        // THE CONDITIONAL FIX:
        // Only require password if we are NOT editing an existing user
        if (existingUser == null) {
            if (passwordField.getText() == null || passwordField.getText().isEmpty()) {
                errorMessage += "No valid password!\n";
            }
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            showError("Invalid Fields", errorMessage);
            return false;
        }
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