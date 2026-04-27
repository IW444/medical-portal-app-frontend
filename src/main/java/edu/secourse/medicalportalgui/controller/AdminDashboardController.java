package edu.secourse.medicalportalgui.controller;

import edu.secourse.medicalportalgui.model.User;
import edu.secourse.medicalportalgui.model.Appointment;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Controller class for the Administrator Dashboard.
 * Handles user management (CRUD), appointment management, and real-time filtering.
 */
public class AdminDashboardController {

    /** The list of users retrieved from the database. */
    private final ObservableList<User> masterUserData = FXCollections.observableArrayList();
    /** A wrapper around the list that allows for searching and filtering. */
    private FilteredList<User> filteredData;

    /** The currently authenticated admin using the dashboard. */
    private User loggedInUser;
    /** HTTP client for performing API calls to the  backend. */
    private final HttpClient client = HttpClient.newHttpClient();

    /** Mapper used to convert JSON responses into Java objects */
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    /** TableView component for listing user records. */
    @FXML
    private TableView<User> usersTable;
    /** Column displaying the unique ID of a user. */
    @FXML
    private TableColumn<User, Integer> colUserId;
    /** Column displaying the user's first name. */
    @FXML
    private TableColumn<User, String> colFirstName;
    /** Column displaying the user's last name. */
    @FXML
    private TableColumn<User, String> colLastName;
    /** Column displaying the unique system username. */
    @FXML
    private TableColumn<User, String> colUsername;
    /** Column displaying the user's assigned role (Admin, Doctor, Patient). */
    @FXML
    private TableColumn<User, String> colRole;
    /** Column displaying the timestamp of the user's last login. */
    @FXML
    private TableColumn<User, String> colLastLogin;
    /** Text field for filtering users by name or username. */
    @FXML private TextField searchUserField;

    /** TableView component for listing appointments. */
    @FXML
    private TableView<Appointment> appointmentsTable;
    /** Column displaying the unique appointment ID. */
    @FXML
    private TableColumn<Appointment, Integer> colAppointmentId;
    /** Column displaying the scheduled date of the appointment. */
    @FXML
    private TableColumn<Appointment, String> colDate;
    /** Column displaying the scheduled start time. */
    @FXML
    private TableColumn<Appointment, String> colStartTime;
    /** Column displaying the scheduled end time. */
    @FXML
    private TableColumn<Appointment, String> colEndTime;
    /** Column displaying the patient's full name. */
    @FXML
    private TableColumn<Appointment, String> colPatient;
    /** Column displaying the assigned doctor's full name. */
    @FXML
    private TableColumn<Appointment, String> colDoctor;
    /** Column displaying when the appointment record was last modified. */
    @FXML
    private TableColumn<Appointment, String> colLastUpdated;

    /** Label used to display a personalized welcome message to the admin. */
    @FXML
    private Label welcomeLabel;

    /** Dropdown for filtering users by their specific role. */
    @FXML private ComboBox<String> roleFilterCombo;

    /** Text field for searching appointments by doctor or patient names. */
    @FXML
    private TextField appointmentSearchField;

    /** The full list of appointments. */
    private ObservableList<Appointment> allAppointments = FXCollections.observableArrayList();

    /**
     * Initializes the controller class. Sets up table column mappings,
     * UI listeners for filtering, and dropdown options.
     */
    @FXML
    public void initialize() {

        filteredData = new FilteredList<>(masterUserData, p -> true);

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

        roleFilterCombo.getItems().clear();
        roleFilterCombo.getItems().addAll("ALL", "PATIENT", "DOCTOR", "ADMIN");

        searchUserField.textProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        roleFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());

        usersTable.setItems(filteredData);

        roleFilterCombo.setValue("ALL");

        loadUsers();
    }

    /**
     * Sets the currently logged-in administrator and triggers data loading.
     * @param user The User object representing the logged-in admin.
     */
    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, Admin " + user.getFirstName());
        }
        loadUsers();
        loadAppointments();
    }

    /**
     * Fetches all users from the backend API asynchronously.
     */
    @FXML
    private void loadUsers() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users"))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                User[] users = mapper.readValue(response.body(), User[].class);

                                Platform.runLater(() -> {
                                    masterUserData.setAll(users);
                                    updateFilter();
                                    //System.out.println("Successfully loaded " + users.length + " users.");
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.err.println("Backend returned error: " + response.statusCode());
                        }
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            new Alert(Alert.AlertType.ERROR, "Server Connection Failed").show();
                        });
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetches all appointments from the backend API.
     */
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
                allAppointments = FXCollections.observableArrayList(appointments);
                appointmentsTable.setItems(allAppointments);
                ObservableList<Appointment> appointmentList = FXCollections.observableArrayList(appointments);
                appointmentsTable.setItems(appointmentList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens a form to create a new user.
     */
    @FXML
    private void handleCreateUser() {
        try {
            // Load the FXML we created for the popup
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/secourse/medicalportalgui/createUser.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Register New User");

            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/Register-user-icon.png")));

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

    /**
     * Opens a form to update the currently selected user.
     */
    @FXML
    private void handleUpdateUser() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a user to update.").showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/secourse/medicalportalgui/createUser.fxml"));
            Parent root = loader.load();

            CreateUserController controller = loader.getController();
            controller.setExistingUser(selectedUser);

            Stage stage = new Stage();
            stage.setTitle("Edit User: " + selectedUser.getUsername());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadUsers(); // Refresh after edit
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes the selected user from the backend after confirmation.
     */
    @FXML
    private void handleDeleteUser() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a user to delete.").showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete user: " + selectedUser.getUsername() + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/users/" + selectedUser.getUserId()))
                            .DELETE()
                            .build();

                    client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenAccept(resp -> {
                                if (resp.statusCode() == 200 || resp.statusCode() == 204) {
                                    // Run the refresh on the JavaFX thread
                                    Platform.runLater(this::loadUsers);
                                } else {
                                    Platform.runLater(() ->
                                            new Alert(Alert.AlertType.ERROR, "Delete failed: " + resp.statusCode()).show()
                                    );
                                }
                            })
                            .exceptionally(ex -> {
                                ex.printStackTrace();
                                return null;
                            });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Opens a form to create a new appointment.
     */
    @FXML
    private void handleCreateAppointment() {
        try {
            // Load the FXML we created for the popup
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/secourse/medicalportalgui/createAppointment.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Create New Appointment");

            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Refresh the table so the new appointment appears immediately
            loadAppointments();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not load the creation form.");
            alert.showAndWait();
        }
    }

    /**
     * Opens a form to edit the selected appointment.
     */
    @FXML
    private void handleUpdateAppointment() {
        Appointment selected = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select an appointment to edit.").showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/edu/secourse/medicalportalgui/createAppointment.fxml"));
            Parent root = loader.load();

            // Pass selected appointment data to the form
            AppointmentFormController controller = loader.getController();
            controller.setAppointment(selected);

            Stage stage = new Stage();
            stage.setTitle("Edit Appointment");
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/Patient-Portal-icon.png")));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadAppointments(); // Refresh table after editing

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Could not load appointment form.").showAndWait();
        }
    }

    /**
     * Deletes the selected appointment from the system.
     */
    @FXML
    private void handleDeleteAppointment() {
        Appointment selected = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select an appointment to cancel.").showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to cancel this appointment?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();

        if (confirm.getResult() != ButtonType.YES) return;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/appointments/" + selected.getAppointmentId()))
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                appointmentsTable.getItems().remove(selected);
            } else {
                new Alert(Alert.AlertType.ERROR,
                        "Could not cancel appointment. Status: " + response.statusCode()).showAndWait();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Connection error.").showAndWait();
        }
    }

    /**
     * Switches the application view back to the login screen
     * after the current admin logs out.
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

        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the Change Password dialog for the logged-in user.
     * Passes the current user to the ChangePasswordController to handle the update.
     */
    @FXML
    private void handleChangePassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/edu/secourse/medicalportalgui/changePassword.fxml"));
            Parent root = loader.load();

            ChangePasswordController controller = loader.getController();

            // Pass the Admin's user object to the popup
            if (loggedInUser != null) {
                controller.setUser(loggedInUser);

                Stage stage = new Stage();
                stage.setTitle("Change Admin Password");
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/change-password-icon.png")));

                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.setScene(new Scene(root));

                // Ensure proper window sizing
                stage.sizeToScene();
                stage.showAndWait();
            }
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Could not open password update form.").showAndWait();
        }
    }

    /**
     * Logic for filtering the User TableView based on role and search text.
     */
    private void updateFilter() {
        String searchText = (searchUserField.getText() == null) ? "" : searchUserField.getText().toLowerCase().trim();
        String roleFilter = roleFilterCombo.getValue();

        filteredData.setPredicate(user -> {
            boolean matchesRole = (roleFilter == null || roleFilter.equals("ALL")) ||
                    user.getRole().equalsIgnoreCase(roleFilter);

            boolean matchesSearch = searchText.isEmpty() ||
                    user.getFirstName().toLowerCase().contains(searchText) ||
                    user.getLastName().toLowerCase().contains(searchText) ||
                    user.getUsername().toLowerCase().contains(searchText);

            return matchesRole && matchesSearch;
        });
    }

    /**
     * Filters the Appointment TableView based on doctor or patient username keywords.
     */
    @FXML
    private void handleAppointmentSearch() {
        String keyword = appointmentSearchField.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            appointmentsTable.setItems(allAppointments);
            return;
        }

        ObservableList<Appointment> filtered = allAppointments.filtered(appointment -> {
            //Search by patient username
            String patientUsername = "";
            if (appointment.getPatient() != null) {
                patientUsername = appointment.getPatient().getUsername().toLowerCase();
            }

            // Search by doctor username
            String doctorUsername = "";
            if (appointment.getDoctor() != null) {
                doctorUsername = appointment.getDoctor().getUsername().toLowerCase();
            }

            return patientUsername.contains(keyword) || doctorUsername.contains(keyword);
        });

        appointmentsTable.setItems(filtered);

        if (filtered.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "No appointments found for \"" + appointmentSearchField.getText() + "\"")
                    .showAndWait();
        }
    }

    /**
     * Resets the appointment search field and restores the full list.
     */
    @FXML
    private void handleClearAppointmentSearch() {
        appointmentSearchField.clear();
        appointmentsTable.setItems(allAppointments);
    }
}