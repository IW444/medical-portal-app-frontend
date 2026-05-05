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

/**
 * JavaFX controller for the appointment creation and editing form.
 *
 * <p>Handles user input for scheduling appointments, including date and
 * time selection, patient and doctor assignment, and form validation.
 * Communicates with the REST backend over HTTP to create ({@code POST})
 * or update ({@code PUT}) appointment records.</p>
 *
 * <p>When {@link #appointmentId} is {@code null}, the form operates in
 * <em>create</em> mode. When pre-populated via {@link #setAppointment},
 * it operates in <em>edit</em> mode and issues a {@code PUT} request instead.</p>
 */
public class AppointmentFormController {

    /** Date picker for selecting the appointment date. Past dates are disabled. */
    @FXML private DatePicker datePicker;

    /** Text field for the appointment start time in {@code HH:MM} format. */
    @FXML private TextField startTimeField;

    /** Text field for the appointment end time in {@code HH:MM} format. */
    @FXML private TextField endTimeField;

    /** Dropdown listing all users with the {@code PATIENT} role. */
    @FXML private ComboBox<String> patientComboBox;

    /** Dropdown listing all users with the {@code DOCTOR} role. */
    @FXML private ComboBox<String> doctorComboBox;

    /** Shared HTTP client used for all REST calls to the backend. */
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * JSON mapper configured with {@link JavaTimeModule} to correctly
     * serialize and deserialize {@link java.time.LocalDate} and
     * {@link java.time.LocalTime} values.
     */
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /** Full list of users fetched from the server, used for username lookup. */
    private List<User> allUsers;

    /**
     * The ID of the appointment being edited, or {@code null} when
     * creating a new appointment.
     */
    private Integer appointmentId = null;

    /**
     * Initializes the form after FXML injection is complete.
     *
     * <p>Disables past dates in the {@link DatePicker} by applying a custom
     * {@code DayCellFactory}, then fetches all users from the backend and
     * populates the patient and doctor dropdowns accordingly.</p>
     */
    @FXML
    public void initialize() {
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #e0e0e0;");
                }
            }
        });

        loadUsersIntoDropdowns();
    }

    /**
     * Fetches all users from {@code GET /users} and populates the
     * patient and doctor {@link ComboBox} dropdowns.
     *
     * <p>Users are filtered by role: those with role {@code PATIENT} go
     * into {@link #patientComboBox}, and those with role {@code DOCTOR}
     * go into {@link #doctorComboBox}. On a non-200 response or a
     * network failure, an error dialog is shown and the dropdowns
     * remain empty.</p>
     */
    private void loadUsersIntoDropdowns() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                allUsers = Arrays.asList(mapper.readValue(response.body(), User[].class));

                List<String> patients = allUsers.stream()
                        .filter(u -> "PATIENT".equalsIgnoreCase(u.getRole()))
                        .map(u -> u.getUsername())
                        .collect(Collectors.toList());

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

    /**
     * Looks up a {@link User} object from the cached user list by username.
     *
     * @param username the username to search for
     * @return the matching {@link User}, or {@code null} if not found
     */
    private User getUserByUsername(String username) {
        return allUsers.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    /**
     * Pre-populates the form fields with data from an existing appointment.
     *
     * <p>Calling this method switches the form into <em>edit</em> mode.
     * When the user saves, a {@code PUT} request is sent to update the
     * existing record rather than creating a new one.</p>
     *
     * @param appointment the {@link Appointment} whose data should be loaded into the form
     */
    public void setAppointment(Appointment appointment) {
        this.appointmentId = appointment.getAppointmentId();
        datePicker.setValue(appointment.getDate());
        startTimeField.setText(appointment.getStartTime() != null ? appointment.getStartTime().toString() : "");
        endTimeField.setText(appointment.getEndTime() != null ? appointment.getEndTime().toString() : "");

        if (appointment.getPatient() != null)
            patientComboBox.setValue(appointment.getPatient().getUsername());
        if (appointment.getDoctor() != null)
            doctorComboBox.setValue(appointment.getDoctor().getUsername());
    }

    /**
     * Handles the Save button action.
     *
     * <p>Validates all form fields first via {@link #isInputValid()}. If
     * validation passes, builds a JSON payload with the selected date,
     * times, patient ID, and doctor ID, then sends either:</p>
     * <ul>
     *   <li>{@code POST /appointments} — when creating a new appointment</li>
     *   <li>{@code PUT /appointments/{id}} — when editing an existing appointment</li>
     * </ul>
     *
     * <p>Closes the window on {@code 200} or {@code 201}. Shows an error
     * dialog on {@code 409 Conflict} (scheduling clash) or any other
     * non-success status code. Also handles network failures gracefully.</p>
     */
    @FXML
    private void handleSave() {
        if (!isInputValid()) return;

        try {
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
            } else if (response.statusCode() == 409) {
                showError("Time Conflict", response.body());
            } else {
                showError("Server Error", "Status: " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Connection Error", "Could not reach the server.");
        }
    }

    /**
     * Validates all form inputs before a save is attempted.
     *
     * <p>Checks the following rules and highlights any invalid field
     * with a red border:</p>
     * <ul>
     *   <li>Date must be selected and must not be in the past</li>
     *   <li>Start time must match the pattern {@code HH:MM}</li>
     *   <li>End time must match the pattern {@code HH:MM}</li>
     *   <li>A patient must be selected from the dropdown</li>
     *   <li>A doctor must be selected from the dropdown</li>
     * </ul>
     *
     * <p>If any rule fails, an error dialog listing all issues is shown
     * via {@link #showError}.</p>
     *
     * @return {@code true} if all inputs are valid; {@code false} otherwise
     */
    private boolean isInputValid() {
        datePicker.setStyle("");
        startTimeField.setStyle("");
        endTimeField.setStyle("");
        patientComboBox.setStyle("");
        doctorComboBox.setStyle("");

        boolean valid = true;
        StringBuilder errorMsg = new StringBuilder("Please fix the following:\n");

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

    /**
     * Handles the Cancel button action by closing the form window
     * without saving any changes.
     */
    @FXML
    private void handleCancel() { closeWindow(); }

    /**
     * Closes the current stage (window) containing this form.
     *
     * <p>Retrieves the {@link Stage} from any FXML control's scene and
     * calls {@link Stage#close()}.</p>
     */
    private void closeWindow() {
        Stage stage = (Stage) datePicker.getScene().getWindow();
        stage.close();
    }

    /**
     * Displays a modal error dialog to the user.
     *
     * @param title   the title bar text of the alert dialog
     * @param message the body message describing the error
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}