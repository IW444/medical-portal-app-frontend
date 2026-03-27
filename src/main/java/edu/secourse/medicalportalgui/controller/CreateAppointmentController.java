package edu.secourse.medicalportalgui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.secourse.medicalportalgui.model.Appointment;
import edu.secourse.medicalportalgui.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.time.LocalDateTime;
import javafx.scene.control.DatePicker;

public class CreateAppointmentController {

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> startTimeComboBox;
    @FXML private ComboBox<String> endTimeComboBox;
    @FXML private ComboBox<User> patientComboBox;
    @FXML private ComboBox<User> doctorComboBox;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public void initialize() {
        try {
            //GET request to obtain users from the database
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users"))
                    .GET()
                    .build();

            //What we receive (JSON format)
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            //Is it good?  That is, did we successfully receive the JSON file from our request?
            if (response.statusCode() == 200) {

                //Make it Java instead of JSON
                User[] users = mapper.readValue(response.body(), User[].class);

                //Dropboxes for times
                for (int hour = 8; hour <= 16; hour++) {
                    startTimeComboBox.getItems().add(String.format("%02d:00", hour));
                    startTimeComboBox.getItems().add(String.format("%02d:30", hour));
                    endTimeComboBox.getItems().add(String.format("%02d:00", hour));
                    endTimeComboBox.getItems().add(String.format("%02d:30", hour));
                }

                //filter users for patients:
                ObservableList<User> patients = FXCollections.observableArrayList();
                for (User u : users) {
                    if (u.getRole().equals("PATIENT")) {
                        patients.add(u);
                    }
                }
                patientComboBox.getItems().addAll(patients);

                //Make items in the dropdown list look pretty ("First Name" "Last Name")
                patientComboBox.setCellFactory(lv -> new javafx.scene.control.ListCell<User>() {
                    @Override
                    protected void updateItem(User user, boolean empty) {
                        super.updateItem(user, empty);
                        setText(empty || user == null ? null : user.getFirstName() + " " + user.getLastName());
                    }
                });
                //Make items selected from the dropdown list look pretty ("First Name" "Last Name")
                patientComboBox.setButtonCell(new javafx.scene.control.ListCell<User>() {
                    @Override
                    protected void updateItem(User user, boolean empty) {
                        super.updateItem(user, empty);
                        setText(empty || user == null ? null : user.getFirstName() + " " + user.getLastName());
                    }
                });


                //filter users for doctors:
                ObservableList<User> doctors = FXCollections.observableArrayList();
                for (User u : users) {
                    if (u.getRole().equals("DOCTOR")) {
                        doctors.add(u);
                    }
                }
                doctorComboBox.getItems().addAll(doctors);

                //Make items in the dropdown list look pretty ("First Name" "Last Name")
                doctorComboBox.setCellFactory(lv -> new javafx.scene.control.ListCell<User>() {
                    @Override
                    protected void updateItem(User user, boolean empty) {
                        super.updateItem(user, empty);
                        setText(empty || user == null ? null : user.getFirstName() + " " + user.getLastName());
                    }
                });
                //Make items selected from the dropdown list look pretty ("First Name" "Last Name")
                doctorComboBox.setButtonCell(new javafx.scene.control.ListCell<User>() {
                    @Override
                    protected void updateItem(User user, boolean empty) {
                        super.updateItem(user, empty);
                        setText(empty || user == null ? null : user.getFirstName() + " " + user.getLastName());
                    }
                });
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            try {
                // Create an Appointment object to hold the form data
                Appointment newAppointment = new Appointment();
                newAppointment.setDate(datePicker.getValue());
                newAppointment.setStartTime(LocalTime.parse(startTimeComboBox.getSelectionModel().getSelectedItem()));
                newAppointment.setEndTime(LocalTime.parse(endTimeComboBox.getSelectionModel().getSelectedItem()));
                newAppointment.setPatient(patientComboBox.getSelectionModel().getSelectedItem());
                newAppointment.setDoctor(doctorComboBox.getSelectionModel().getSelectedItem());
                newAppointment.setTimestamp(LocalDateTime.now());

                String json = mapper.writeValueAsString(newAppointment);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/appointments"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    closeWindow();
                } else {
                    showError("Server Error", "Could not create appointment. Status: " + response.statusCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Connection Error", "Could not reach the server.");
            }
        }
    }

    private boolean isInputValid() {
        if (datePicker.getValue() == null || startTimeComboBox.getSelectionModel().isEmpty()
                || endTimeComboBox.getSelectionModel().isEmpty()
                || patientComboBox.getValue() == null || doctorComboBox.getValue() == null)
        {
            showError("Validation Error", "Please fill in all required fields.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) datePicker.getScene().getWindow();
        stage.close();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

}

