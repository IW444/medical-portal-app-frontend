package edu.secourse.medicalportalgui.controller;

import edu.secourse.medicalportalgui.model.User;
import edu.secourse.medicalportalgui.model.Appointment;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AdminDashboardController {

    private User loggedInUser;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    // ------------------ Users Table ------------------
    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, Integer> colUserId;
    @FXML
    private TableColumn<User, String> colFirstName;
    @FXML
    private TableColumn<User, String> colLastName;
    @FXML
    private TableColumn<User, String> colUsername;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, String> colLastLogin;

    // ------------------ Appointments Table ------------------
    @FXML
    private TableView<Appointment> appointmentsTable;
    @FXML
    private TableColumn<Appointment, Integer> colAppointmentId;
    @FXML
    private TableColumn<Appointment, String> colDate;
    @FXML
    private TableColumn<Appointment, String> colStartTime;
    @FXML
    private TableColumn<Appointment, String> colEndTime;
    @FXML
    private TableColumn<Appointment, String> colPatient;
    @FXML
    private TableColumn<Appointment, String> colDoctor;
    @FXML
    private TableColumn<Appointment, String> colLastUpdated;

    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        // Users Table Column Mapping
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        // Standard PropertyValueFactory works for basic types
        colLastLogin.setCellValueFactory(new PropertyValueFactory<>("lastLogin"));

        // Appointments Table Column Mapping
        colAppointmentId.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStartTime.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colLastUpdated.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Extracting names from User objects within the Appointment
        colPatient.setCellValueFactory(cellData -> {
            User p = cellData.getValue().getPatient();
            return new SimpleStringProperty(p != null ? p.getFirstName() + " " + p.getLastName() : "N/A");
        });

        colDoctor.setCellValueFactory(cellData -> {
            User d = cellData.getValue().getDoctor();
            return new SimpleStringProperty(d != null ? d.getFirstName() + " " + d.getLastName() : "N/A");
        });
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, Admin " + user.getFirstName());
        }
        loadUsers();
        loadAppointments();
    }

    // Loading Users
    @FXML
    private void loadUsers() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                User[] users = mapper.readValue(response.body(), User[].class);
                ObservableList<User> userList = FXCollections.observableArrayList(users);
                usersTable.setItems(userList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load Appointments
    @FXML
    private void loadAppointments() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/appointments"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Appointment[] appointments = mapper.readValue(response.body(), Appointment[].class);
                ObservableList<Appointment> appointmentList = FXCollections.observableArrayList(appointments);
                appointmentsTable.setItems(appointmentList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // CRUD Buttons for Users
    @FXML
    private void handleCreateUser() {
        try {
            // Load the FXML we created for the popup
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/secourse/medicalportalgui/createUser.fxml"));
            Parent root = loader.load();

            // Setup the new window (Stage)
            Stage stage = new Stage();
            stage.setTitle("Register New User");

            // Wait until the form is closed
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));

            // Wait for the admin to finish before continuing
            stage.showAndWait();

            // Refresh the table so the new user appears immediately
            loadUsers();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not load the creation form.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleUpdateUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        // TODO: Open modal to edit selected user
    }

    @FXML
    private void handleDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users/" + selected.getUserId()))
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                usersTable.getItems().remove(selected);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // CRUD Buttons for Appointments
    @FXML
    private void handleCreateAppointment() {
        // TODO: Open modal or FXML for new appointment
    }

    @FXML
    private void handleUpdateAppointment() {
        Appointment selected = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        // TODO: Open modal to edit selected appointment
    }

    @FXML
    private void handleDeleteAppointment() {
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

    @FXML
    private void handleLogout() {
        // TODO: Go back to login screen
    }
}