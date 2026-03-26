package edu.secourse.medicalportalgui.controller;

import edu.secourse.medicalportalgui.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChangePasswordController {

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    private User userToUpdate;
    private final HttpClient client = HttpClient.newHttpClient();

    public void setUser(User user) {
        this.userToUpdate = user;
    }

    @FXML
    private void handleUpdate() {
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (newPass.isEmpty() || !newPass.equals(confirmPass)) {
            showError("Mismatch", "Passwords do not match or are empty.");
            return;
        }

        try {
            // We send a partial update (PATCH) with just the new password
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

    @FXML private void handleCancel() {
        ((Stage) newPasswordField.getScene().getWindow()).close();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR, content);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
