package edu.secourse.medicalportalgui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * Model class for Appointments.
 * This class contains the Appointment constructor
 * as well as the necessary getters and setters.
 */
public class Appointment {

    private Integer appointmentId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    private User patient;  // for Patient Dashboard
    private User doctor;   // for Doctor Dashboard
    private LocalDateTime timestamp;

    public Appointment() {}

/**Getter and Setter methods for a specific Appointment ID
 * @return appointmentId
 */
    public Integer getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Integer appointmentId) { this.appointmentId = appointmentId; }

/**Getter and Setter methods for the date of a specific Appointment
 * @return LocalDate date
 */
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

/**Getter and Setter methods for the start time for an Appointment
 * @return startTime
 */
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

/**Getter and Setter methods for the end time for an Appointment
 * @return endTime
 */
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

/**Getter and Setter methods for the patient that an Appointment has been made for
 * @return patient
 */
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

/**Getter and Setter methods for the doctor that an Appointment has been scheduled with
 * @return doctor
 */
    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }

/**Getter and Setter methods for the day and time that an Appointment was made on
* @return timestamp
*/
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}