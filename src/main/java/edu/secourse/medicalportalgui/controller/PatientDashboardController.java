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
        // TODO: Go back to login screen
    }
}
