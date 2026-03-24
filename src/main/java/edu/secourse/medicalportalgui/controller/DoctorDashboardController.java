package edu.secourse.medicalportalgui.controller;

import edu.secourse.medicalportalgui.model.Appointment;
import edu.secourse.medicalportalgui.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        welcomeLabel.setText("Welcome, Dr. " + user.getFirstName());
        loadAppointments();
    }

    @FXML
    private void handleRefreshAppointments() {
        loadAppointments();
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

    @FXML
    private void handleLogout() {
        // TODO: Go back to login screen
    }
}
