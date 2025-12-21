package com.example.mef.demo.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.util.UUID;


@Setter
@Getter
@Entity
@Table(name="User")
@NoArgsConstructor
public class User {
    // Getters and setters
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @NotEmpty
    @Column(name="password", nullable = false, length = 250)
    @Size(min=3,max=250 , message = "Password must be 3 - 15 characters long")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotEmpty
    @Column(name="username", nullable = false, length = 100)
    @Size(min=4,message = "Username must be minimum of 4 characters")
    private String username;

    @Email(message = "Email address is not valid")
    @Column(name="email", nullable = false, length = 100)
    private String email;

    // Constructor with parameters
    public User(String username,String password, String email) {
        this.username = username;
        this.email = email;
        this.password=password;
    }


}