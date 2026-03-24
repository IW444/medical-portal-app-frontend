package edu.secourse.medicalportalgui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Appointment {

    private Integer appointmentId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    private User patient;  // for Patient Dashboard
    private User doctor;   // for Doctor Dashboard
    private LocalDateTime timestamp;

    public Appointment() {}

    // Getters and Setters
    public Integer getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Integer appointmentId) { this.appointmentId = appointmentId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}