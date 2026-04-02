package edu.secourse.medicalportalgui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.secourse.medicalportalgui.model.Appointment;
import edu.secourse.medicalportalgui.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AppointmentFormController {

    @FXML private DatePicker datePicker;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private ComboBox<String> patientComboBox;
    @FXML private ComboBox<String> doctorComboBox;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private List<User> allUsers;   // full user list from server
    private Integer appointmentId = null;

    @FXML
    public void initialize() {
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);       // can't click it
                    setStyle("-fx-background-color: #e0e0e0;"); // grayed out
                }
            }
        });

        loadUsersIntoDropdowns();
    }

    private void loadUsersIntoDropdowns() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                allUsers = Arrays.asList(mapper.readValue(response.body(), User[].class));

                // Patients: users with role PATIENT
                List<String> patients = allUsers.stream()
                        .filter(u -> "PATIENT".equalsIgnoreCase(u.getRole()))
                        .map(u -> u.getUsername())
                        .collect(Collectors.toList());

                // Doctors: users with role DOCTOR
                List<String> doctors = allUsers.stream()
                        .filter(u -> "DOCTOR".equalsIgnoreCase(u.getRole()))
                        .map(u -> u.getUsername())
                        .collect(Collectors.toList());

                patientComboBox.setItems(FXCollections.observableArrayList(patients));
                doctorComboBox.setItems(FXCollections.observableArrayList(doctors));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Connection Error", "Could not load users from server.");
        }
    }

    // Helper: find User object by username
    private User getUserByUsername(String username) {
        return allUsers.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    // Called when editing an existing appointment
    public void setAppointment(Appointment appointment) {
        this.appointmentId = appointment.getAppointmentId();
        datePicker.setValue(appointment.getDate());
        startTimeField.setText(appointment.getStartTime() != null ? appointment.getStartTime().toString() : "");
        endTimeField.setText(appointment.getEndTime() != null ? appointment.getEndTime().toString() : "");

        // Pre-select the username in dropdowns
        if (appointment.getPatient() != null)
            patientComboBox.setValue(appointment.getPatient().getUsername());
        if (appointment.getDoctor() != null)
            doctorComboBox.setValue(appointment.getDoctor().getUsername());
    }

    @FXML
    private void handleSave() {
        if (!isInputValid()) return;

        try {
            // Get User objects from selected usernames
            User patient = getUserByUsername(patientComboBox.getValue());
            User doctor  = getUserByUsername(doctorComboBox.getValue());
            String dateStr = datePicker.getValue().toString();

            String json = String.format(
                    "{\"date\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"," +
                            "\"patient\":{\"userId\":%d},\"doctor\":{\"userId\":%d}}",
                    dateStr,
                    startTimeField.getText() + ":00",
                    endTimeField.getText() + ":00",
                    patient.getUserId(),
                    doctor.getUserId()
            );

            HttpRequest request;

            if (appointmentId == null) {
                request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/appointments"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
            } else {
                request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/appointments/" + appointmentId))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(json))
                        .build();
            }

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                closeWindow();
            } else {
                showError("Server Error", "Status: " + response.statusCode());
            }

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                closeWindow();
            } else if (response.statusCode() == 409) {
                showError("Time Conflict", response.body()); // shows "Doctor already has an appointment..." etc.
            } else {
                showError("Server Error", "Status: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Connection Error", "Could not reach the server.");
        }
    }

    private boolean isInputValid() {
        // Clear previous error styles
        datePicker.setStyle("");
        startTimeField.setStyle("");
        endTimeField.setStyle("");
        patientComboBox.setStyle("");
        doctorComboBox.setStyle("");

        boolean valid = true;
        StringBuilder errorMsg = new StringBuilder("Please fix the following:\n");

        // DatePicker validation — no format checking needed
        if (datePicker.getValue() == null) {
            datePicker.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            errorMsg.append("• Please select an appointment date\n");
            valid = false;
        } else if (datePicker.getValue().isBefore(LocalDate.now())) {
            datePicker.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            errorMsg.append("• Appointment date cannot be in the past\n");
            valid = false;
        }

        if (startTimeField.getText().isEmpty() || !startTimeField.getText().matches("\\d{2}:\\d{2}")) {
            startTimeField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            errorMsg.append("• Start Time format must be HH:MM (e.g. 09:00)\n");
            valid = false;
        }
        if (endTimeField.getText().isEmpty() || !endTimeField.getText().matches("\\d{2}:\\d{2}")) {
            endTimeField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            errorMsg.append("• End Time format must be HH:MM (e.g. 10:00)\n");
            valid = false;
        }
        if (patientComboBox.getValue() == null) {
            patientComboBox.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            errorMsg.append("• Please select a Patient\n");
            valid = false;
        }
        if (doctorComboBox.getValue() == null) {
            doctorComboBox.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            errorMsg.append("• Please select a Doctor\n");
            valid = false;
        }

        if (!valid) showError("Validation Error", errorMsg.toString());
        return valid;
    }

    @FXML
    private void handleCancel() { closeWindow(); }

    private void closeWindow() {
        Stage stage = (Stage) datePicker.getScene().getWindow();
        stage.close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}