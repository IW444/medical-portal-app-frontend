package edu.secourse.medicalportalgui.controller;

import edu.secourse.medicalportalgui.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Controller class for the Change Password window.
 * This class handles the logic for validating and updating a user's password
 * by communicating with the backend REST API.
 */
public class ChangePasswordController {

    /** Field for the user to input their desired new password. */
    @FXML private PasswordField newPasswordField;
    /** Field for the user to re-enter the new password for verification. */
    @FXML private PasswordField confirmPasswordField;

    /** The User object representing the account currently being updated. */
    private User userToUpdate;
    /** HTTP client used to send password update requests to the server. */
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * Selects the user whose password needs to be changed.
     * This is called by the parent controller before displaying the stage.
     * * @param user The user object to be updated.
     */
    public void setUser(User user) {
        this.userToUpdate = user;
    }

    /**
     * Handles the password update action.
     * Validates that the fields are not empty and that the passwords match.
     * If valid, sends a PATCH request to the backend with the new password.
     */
    @FXML
    private void handleUpdate() {
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (newPass.isEmpty() || !newPass.equals(confirmPass)) {
            showError("Mismatch", "Passwords do not match or are empty.");
            return;
        }

        try {
            String json = String.format("{\"password\":\"%s\"}", newPass);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users/" + userToUpdate.getUserId()))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Password updated successfully!");
                alert.showAndWait();
                ((Stage) newPasswordField.getScene().getWindow()).close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the change password window without making any changes.
     */
    @FXML private void handleCancel() {
        ((Stage) newPasswordField.getScene().getWindow()).close();
    }

    /**
     * Method to display an error alert to the user.
     * * @param title   The title of the alert window.
     * @param content The error message to display.
     */
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR, content);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
