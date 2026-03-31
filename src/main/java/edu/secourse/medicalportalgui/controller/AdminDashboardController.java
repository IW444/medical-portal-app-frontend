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

public class AdminDashboardController {

    private final ObservableList<User> masterUserData = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;

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

    @FXML private ComboBox<String> roleFilterCombo;


    @FXML
    public void initialize() {

        filteredData = new FilteredList<>(masterUserData, p -> true);

        // 2. TABLE SETUP: Link columns to User model properties
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colLastLogin.setCellValueFactory(new PropertyValueFactory<>("lastLogin"));

        // 3. APPOINTMENT TABLE SETUP: Column Mappings
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

        // 4. UI COMPONENT SETUP: Fill the dropdown options
        roleFilterCombo.getItems().clear();
        roleFilterCombo.getItems().addAll("ALL", "PATIENT", "DOCTOR", "ADMIN");

        // 5. ATTACH LISTENERS: Now that filteredData is initialized, it's safe to listen
        searchUserField.textProperty().addListener((obs, oldVal, newVal) -> updateFilter());
        roleFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter());

        // 6. FINALIZING: Set table items and trigger initial load
        usersTable.setItems(filteredData);

        // Setting the value "ALL" will trigger updateFilter() once,
        // which is fine now because filteredData exists.
        roleFilterCombo.setValue("ALL");

        loadUsers();
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
    @FXML private TextField searchUserField; // Add this variable

    @FXML
    private void loadUsers() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users"))
                    .GET()
                    .build();

            // We use sendAsync to keep the UI from freezing during the network call
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                User[] users = mapper.readValue(response.body(), User[].class);

                                // CRITICAL: UI updates must happen on the JavaFX Application Thread
                                Platform.runLater(() -> {
                                    masterUserData.setAll(users);
                                    updateFilter();
                                    System.out.println("Successfully loaded " + users.length + " users.");
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

            // Get the controller and "Load" the selected user data into it
            CreateUserController controller = loader.getController();
            controller.setExistingUser(selectedUser); // You'll need to add this method to CreateUserController

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

                    // USE ASYNC: Don't freeze the UI while waiting for the server
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
}