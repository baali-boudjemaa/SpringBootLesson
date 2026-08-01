package com.example.mef.demo.Services;

import com.example.mef.demo.Model.User;
import com.example.mef.demo.Repository.UserRepository;
import com.example.mef.demo.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Typed service backing the "users" module screen (account management). */
@Service
public class UserServices {

    @Autowired
    private UserRepository userRepository;


    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByEmail(username);
        if (user.isEmpty())
            throw new UsernameNotFoundException("User not found");
        return new org.springframework.security.core.userdetails.User(user.get().getFullName(), user.get().getPassword(), new ArrayList<>());

    }



    //Get All Users
    public List<User> getAllUser()
    {
        return this.userRepository.findAll();
    }

    //Get Single User
    public User getUser(int id)
    {
        Optional<User> optional = this.userRepository.findById(id);
        return optional.orElse(null);
    }

    //Get Single User By Email
    public Optional<User> getUserByEmail(String email)
    {
        Optional<User> user =	this.userRepository.findByEmail(email);
        return user;
    }

    /**
     * Updates an existing user. If rawPassword is null/blank, the existing
     * password hash is kept unchanged; otherwise it is re-hashed with BCrypt.
     */
    public void updateUser(User user, int id, String rawPassword)
    {
        user.setId(id);
        if (rawPassword == null || rawPassword.isBlank()) {
            userRepository.findById(id).ifPresent(existing -> user.setPassword(existing.getPassword()));
        } else {
            user.setPassword(PasswordUtil.hash(rawPassword));
        }
        this.userRepository.save(user);
    }

    //delete single User
    public void deleteUser(int id)
    {
        this.userRepository.deleteById(id);
    }

    /** Creates a new user, hashing rawPassword with BCrypt before storing it. */
    public User addUser(User user, String rawPassword)
    {
        user.setPassword(PasswordUtil.hash(rawPassword));
        return this.userRepository.save(user);
    }

    public boolean validateLoginCredentials(String email, String password)
    {
        return userRepository.findByEmail(email)
                .map(u -> PasswordUtil.matches(password, u.getPassword()))
                .orElse(false);
    }

}
