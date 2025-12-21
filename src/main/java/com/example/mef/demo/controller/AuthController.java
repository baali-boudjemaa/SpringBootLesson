package com.example.mef.demo.controller;

import com.example.mef.demo.Model.AuthenticationRequest;
import com.example.mef.demo.Model.User;
import com.example.mef.demo.Repository.UserRepository;
import com.example.mef.demo.Service.UserServices;
import com.example.mef.demo.Service.JwtUtil;
import io.lettuce.core.output.ScanOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserServices userDetailsService;
    @Autowired
    private UserRepository repository;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    UserServices userServices;
    @PostMapping("/signup")
    public String registerUser( @RequestBody User user) {
        System.out.println(user.getEmail());
        System.out.println(user.getPassword());
        user.setPassword(encoder.encode(user.getPassword()));
        userServices.addUser(user);
        return "User registered successfully";
    }

    @PostMapping("/login")
    public String loginUser(@RequestBody AuthenticationRequest request) {
        System.out.println(request.getUsername()+request.getPassword());
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new RuntimeException(e);
        }

        UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());

        return jwtUtil.generateToken(user);
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Secure World!";
    }
}
