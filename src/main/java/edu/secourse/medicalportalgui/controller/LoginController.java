package edu.secourse.medicalportalgui.controller;

import edu.secourse.medicalportalgui.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.stage.Stage;
/**
 * Controller class for the Login window.
 * This class handles the logic and other necessary
 * components for connecting a current user to their account
 * by communicating with the backend REST API.
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

/** This method handles the login functionality from the main window
 * by checking the given information against the REST API database
 * and transferring the user to the correct dashboard based on their User Role.
 * There is also error handling in the event that an incorrect username or password is submitted.
 */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        try {
            // Build JSON body
            String json = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Status: " + response.statusCode());
            System.out.println("Body: " + response.body());

            if (response.statusCode() == 200) {
                // Convert response to User object
                User user = mapper.readValue(response.body(), User.class);
                System.out.println("Logged in as: " + user.getRole());

                // Routing Logic:
                if ("ADMIN".equals(user.getRole())) {
                    navigateToDashboard(user, "adminDashboard.fxml");
                } else if ("DOCTOR".equals(user.getRole())) {
                    navigateToDashboard(user, "doctorDashboard.fxml");
                } else if ("PATIENT".equals(user.getRole())) {
                    navigateToDashboard(user, "patientDashboard.fxml");
                }

            } else {
                // Show error alert
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Failed");
                alert.setHeaderText("Invalid username or password");
                alert.showAndWait();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

/** This method handles the passing of user data to the required controller
 * based on the user role of whoever has just logged in and brings the next window into view.
 */
    private void navigateToDashboard(User user, String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/secourse/medicalportalgui/" + fxmlFile));
            Parent root = loader.load();

            // Passing the user data to the next controller
            Object controller = loader.getController();
            if (controller instanceof AdminDashboardController) {
                ((AdminDashboardController) controller).setLoggedInUser(user);
            } else if (controller instanceof DoctorDashboardController) {
                ((DoctorDashboardController) controller).setLoggedInUser(user);
            } else if (controller instanceof PatientDashboardController) {
                ((PatientDashboardController) controller).setLoggedInUser(user);
            }

            // Get the current window (Stage) and swap the Scene
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            // Show alert if the FXML doesn't load
        }
    }
}