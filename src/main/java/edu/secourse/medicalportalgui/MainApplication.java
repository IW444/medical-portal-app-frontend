package edu.secourse.medicalportalgui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // Load the login page
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/edu/secourse/medicalportalgui/login.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 400, 300);

        stage.setTitle("Medical Portal");
        stage.setScene(scene);

        stage.sizeToScene();

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
