package edu.secourse.medicalportalgui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Model class for Users.
 * This class contains the User constructor
 * as well as the necessary getters and setters.
 */
public class User {
    private Integer userId;
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String role; // THIS MUST MATCH BACKEND JSON EXACTLY
    private LocalDateTime lastLogin;

    public User() {}

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

/**Getter and Setter methods for a specific User ID *
 * @return userId
 */
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

/**Getter and Setter methods for a specific User's First Name *
* @return firstName
*/
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

/**Getter and Setter methods for a specific User's Last Name *
* @return lastName
*/
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

/**Getter and Setter methods for a specific User's Username *
* @return username
*/
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

/**Getter and Setter methods for a specific User's role assignment  *
* @return role
*/
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

/**Getter and Setter methods for a specific User's time and date of last login *
* @return lastLogin
*/
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
}
