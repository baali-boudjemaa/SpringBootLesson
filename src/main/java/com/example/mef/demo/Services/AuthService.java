package com.example.mef.demo.service;

import com.example.mef.demo.Model.User;
import com.example.mef.demo.Repository.UserRepository;
import com.example.mef.demo.enums.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Verifies credentials against the stored bcrypt hash.
     * Returns the authenticated user, or empty if the username doesn't
     * exist or the password doesn't match.
     */
    public Optional<User> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(username)
                .filter(user -> BCrypt.checkpw(password, user.getPassword()));
    }

    /**
     * Creates a new user account.
     * Returns true if created successfully, false if the email already exists.
     */
    public boolean register(String name, String email, String password, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            return false;
        }
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = User.builder()
                .name(name)
                .email(email)
                .password(hashed)
                .role(role)
                .build();
        userRepository.save(user);
        return true;
    }
}
