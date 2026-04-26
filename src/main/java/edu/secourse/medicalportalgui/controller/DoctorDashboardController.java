package edu.secourse.medicalportalgui.controller;

import edu.secourse.medicalportalgui.model.Appointment;
import edu.secourse.medicalportalgui.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Controller for the Doctor Dashboard.
 * Manages the doctor-specific functionality available after login, including
 * viewing appointments filtered by current date, week, or month OR by past or future
 * and refreshing the appointment list.
 */
public class DoctorDashboardController {

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
    private TableColumn<Appointment, String> colPatient;
    @FXML
    private TableColumn<Appointment, String> colLastUpdated;

    @FXML
    private ToggleButton todayButton;
    @FXML
    private ToggleButton weekButton;
    @FXML
    private ToggleButton monthButton;
    @FXML
    private ToggleButton pastButton;
    @FXML
    private ToggleButton futureButton;


    /**
     * Initializes the Doctor Dashboard by configuring the appointment table columns
     * using the defaults.
     * Sets up cell value factories for date, time, and patient name display.
     * Called automatically by JavaFX when the FXML is loaded.
     * Note:  Copied directly from the AdminDashboardController
     */
    @FXML
    public void initialize()
    {
        // Appointments Table Column Mapping
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStartTime.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colLastUpdated.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Extracting names from User objects within the Appointment
        colPatient.setCellValueFactory(cellData -> {
            User p = cellData.getValue().getPatient();
            return new SimpleStringProperty(p != null ? p.getFirstName() + " " + p.getLastName() : "N/A");
        });
    }

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    /**
     * Sets the logged-in user and initializes the dashboard for that doctor.
     * Displays a personalized welcome message using the doctor's title and first name
     * and loads their appointments.
     * @param user the currently logged-in doctor
     */
    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        welcomeLabel.setText("Welcome, Dr. " + user.getFirstName());
        loadAppointments();
    }

    /**
     * Refreshes the appointment list by reloading all appointments from the backend.
     * Returns the table to its default unfiltered view.
     */
    @FXML
    private void handleRefreshAppointments() {
        loadAppointments();
    }

    /**
     * Loads all appointments from the backend and filters them to show
     * only appointments belonging to the currently logged-in doctor.
     */
    private void loadAppointments() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/appointments"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Appointment[] appointments = mapper.readValue(response.body(), Appointment[].class);

                // Filter appointments for this doctor only
                ObservableList<Appointment> doctorAppointments = FXCollections.observableArrayList();
                for (Appointment a : appointments) {
                    if (a.getDoctor() != null && a.getDoctor().getUserId().equals(loggedInUser.getUserId())) {
                        doctorAppointments.add(a);
                    }
                }

                appointmentsTable.setItems(doctorAppointments);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs out the current doctor and returns to the login screen.
     */
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

    /**
     * Opens the Change Password dialog for the logged-in doctor.
     * Passes the current user to the ChangePasswordController to handle the update.
     */
    @FXML
    private void handleChangePassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/secourse/medicalportalgui/changePassword.fxml"));
            Parent root = loader.load();

            // Pass the current logged-in user to the popup controller
            ChangePasswordController controller = loader.getController();
            controller.setUser(loggedInUser);

            Stage stage = new Stage();
            stage.setTitle("Change Password");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Loads appointments for the logged-in doctor filtered by date range.
     * Calls the backend API with the specified filter type.
     * @param filter the date filter to apply ("today", "current week", "current month", "past", or "future")
     */
    private void loadAppointmentsByDateFilter(String filter) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/appointments/doctor/" + loggedInUser.getUserId() + "/" + filter))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Appointment[] appointments = mapper.readValue(response.body(), Appointment[].class);
                ObservableList<Appointment> doctorAppointments = FXCollections.observableArrayList(appointments);
                appointmentsTable.setItems(doctorAppointments);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the toggle button selection for filtering appointments by date.
     * Determines which filter to apply based on which toggle button is selected
     * and calls loadAppointmentsByDateFilter() with the appropriate parameter.
     */
    @FXML
    private void handleViewAppointments() {
        if (todayButton.isSelected()) {
            loadAppointmentsByDateFilter("today");
        }
        else if (weekButton.isSelected()) {
            loadAppointmentsByDateFilter("week");
        }
        else if (monthButton.isSelected()) {
            loadAppointmentsByDateFilter("month");
        }
        else if (pastButton.isSelected()) {
            loadAppointmentsByDateFilter("past");
        }
        else if (futureButton.isSelected()) {
            loadAppointmentsByDateFilter("future");
        }
    }
}
