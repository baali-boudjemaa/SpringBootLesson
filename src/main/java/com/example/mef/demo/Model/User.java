package com.example.mef.demo.Model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@Table(name = "\"User\"")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String username;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    @Column(name = "full_name", nullable = false, columnDefinition = "TEXT")
    private String fullName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String role; // ADMIN, TEACHER, STAFF

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public User() {}

    public User(int id, String username, String passwordHash, String fullName, String role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return fullName + " (" + username + ", " + role + ")";
    }
}