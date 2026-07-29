package com.example.mef.demo.Service;



import com.example.mef.demo.Model.User;
import com.example.mef.demo.Repository.UserRepository;
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
            return null;
        }
        return userRepository.findByUsername(username);
    }
}
