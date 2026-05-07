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
/**
 * Controller class for the Create User window.
 * This class handles the logic for creating and updating a user
 * by communicating with the backend REST API.
 */
public class CreateUserController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private User existingUser;

/** This method handles a dropdown selection for a user role to be added */
    @FXML
    public void initialize() {
        // Add roles to the dropdown
        roleComboBox.getItems().addAll("ADMIN", "DOCTOR", "PATIENT");
        roleComboBox.setValue("PATIENT"); // Default value

    }
/**This method allows for existing user information to be edited
 * @param user
 */
    public void setExistingUser(User user) {
        this.existingUser = user;
        // Pre-fill the form
        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        usernameField.setText(user.getUsername());
        roleComboBox.setValue(user.getRole());
    }

/**This method handles the saving system that connects the
 * information input and syncs it with the REST API database.
 * There is also error handling in the event that
 * a username already exists or the server can't be connected to.
 */
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
                } else if (response.statusCode() == 409) {
                    // Highlight the username field in red
                    usernameField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    showError("Username Taken",
                            "The username \"" + usernameField.getText() + "\" is already in use.\nPlease choose a different username.");
                } else {
                    showError("Server Error", "Operation failed. Status: " + response.statusCode());
                }

            } catch (Exception e) {
                e.printStackTrace();
                showError("Connection Error", "Could not reach the server.");
            }
        }
    }

/** This method verifies that information for users is entered correctly
 * @return error messages associated with invalid data entry
 */
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

/** This method handles a close window functionality */
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        stage.close();
    }

/** This method handles a visual aid for error handling where it will
 * prompt the user with what is wrong on screen
 * @param title
 * @param content
 */
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}