package edu.secourse.medicalportalgui.controller;

import edu.secourse.medicalportalgui.model.Appointment;
import edu.secourse.medicalportalgui.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PatientDashboardController {

    private User loggedInUser;

    @FXML
    private Label welcomeLabel;

    @FXML
    private TableView<Appointment> appointmentsTable;

    @FXML
    private TableColumn<Appointment, String> colDate;
    @FXML
    private TableColumn<Appointment, String> colStartTime;
    @FXML
    private TableColumn<Appointment, String> colEndTime;
    @FXML
    private TableColumn<Appointment, String> colDoctor;
    @FXML
    private TableColumn<Appointment, String> colLastUpdated;

    //copied directly from the AdminDashboardController
    @FXML
    public void initialize()
    {
        // Appointments Table Column Mapping
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStartTime.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colLastUpdated.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Extracting names from User objects within the Appointment
        colDoctor.setCellValueFactory(cellData -> {
            User d = cellData.getValue().getDoctor();
            return new SimpleStringProperty(d != null ? d.getFirstName() + " " + d.getLastName() : "N/A");
        });
    }

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        welcomeLabel.setText("Welcome, " + user.getFirstName());
        loadAppointments();
    }

    @FXML
    private void handleRefreshAppointments() {
        loadAppointments();
    }

    @FXML
    private void handleCancelAppointment() {
        Appointment selected = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/appointments/" + selected.getAppointmentId()))
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                appointmentsTable.getItems().remove(selected);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAppointments() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/appointments"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Appointment[] appointments = mapper.readValue(response.body(), Appointment[].class);

                // Filter appointments for current patient
                ObservableList<Appointment> patientAppointments = FXCollections.observableArrayList();
                for (Appointment a : appointments) {
                    if (a.getPatient() != null && a.getPatient().getUserId().equals(loggedInUser.getUserId())) {
                        patientAppointments.add(a);
                    }
                }

                appointmentsTable.setItems(patientAppointments);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try{
            //Find the login screen from our files
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/secourse/medicalportalgui/login.fxml"));
            //Define the root as the login screen (Parent elements can contain other UI elements)
            Parent root = loader.load();
            //The stage is the window.  The welcomeLabel is the welcome text we get when logging in and from this (which we
            //currently have since we are logged in, trying to logout, we can get to the scene and then the window that we
            //are in and thus change it to contain the login screen instead.
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            //Change the title back to Medical Portal instead of "Welcome [role] username"
            stage.setTitle("Medical Portal");
            //Change the scene back to the login screen
            stage.setScene(new Scene(root));

            stage.sizeToScene();
            stage.centerOnScreen();
        }
        //Need to handle exceptions because the loader might throw one.  This takes a general exception and prints the
        //trail of methods that led to the error.
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChangePassword() {
        try {
            // 1. Load the shared Change Password FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/secourse/medicalportalgui/changePassword.fxml"));
            Parent root = loader.load();

            // 2. Get the controller and pass the current logged-in patient
            ChangePasswordController controller = loader.getController();

            // This 'loggedInUser' must be the one you set during the login transition
            if (loggedInUser != null) {
                controller.setUser(loggedInUser);

                // 3. Setup and show the modal window
                Stage stage = new Stage();
                stage.setTitle("Update Your Security Settings");
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.setScene(new Scene(root));

                // Optional: Match the window size to the content
                stage.sizeToScene();

                stage.showAndWait();
            } else {
                System.err.println("Error: No logged-in user found to update.");
            }

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to load the password update form.");
            alert.showAndWait();
        }
    }
}
