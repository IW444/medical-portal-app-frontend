module edu.secourse.medicalportalgui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens edu.secourse.medicalportalgui.model to com.fasterxml.jackson.databind, javafx.base;
    opens edu.secourse.medicalportalgui to javafx.fxml;
    opens edu.secourse.medicalportalgui.controller to javafx.fxml;
    exports edu.secourse.medicalportalgui;
}