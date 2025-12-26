package com.example.mef.demo.controller;

import com.example.mef.demo.Model.AuthenticationRequest;
import com.example.mef.demo.Model.JwtAuthResponse;
import com.example.mef.demo.Model.User;
import com.example.mef.demo.Repository.UserRepository;
import com.example.mef.demo.Service.UserServices;
import com.example.mef.demo.Service.JwtUtil;
import io.lettuce.core.output.ScanOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository repository;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    UserServices userServices;
    @Autowired
    UserDetailsService userDetailsService;
    @PostMapping("/signup")
    public String registerUser( @RequestBody User user) {
        System.out.println(user.getEmail());
        System.out.println(user.getPassword());
        user.setPassword(encoder.encode(user.getPassword()));
        userServices.addUser(user);
        return "User registered successfully";
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody AuthenticationRequest request) {
        System.out.println(request.getUsername()+request.getPassword());
        try {
            this.authenticate(request.getUsername(),request.getPassword());
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(request.getUsername());
            String token = this.jwtUtil.generateToken(userDetails);
            JwtAuthResponse response = new JwtAuthResponse();
            response.setToken(token);
            ResponseCookie springCookie = ResponseCookie.from("session-token", token)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(30 * 60) // 30 minutes
                    .build();
            // It is standard practice to prefix the token with "Bearer "
            System.out.println("User :"+request.getUsername()+"   has been longed in");
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, springCookie.toString())
                    .body(response);

        } catch (Exception e) {

            throw new RuntimeException(e);
        }

    }
    //@PreAuthorize("hasRole('ADMIN')")
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/user/{userId}")
    public String hello(@PathVariable Integer userId) {
        System.out.println(userId);
        userServices.deleteUser(userId);
        return "Hello, Secure World!";
    }

    private void authenticate(String username, String password) throws Exception {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        // we might get exceptions here i.e. user disabled handling it in global exception
        try {
            this.authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        }
        catch(BadCredentialsException ex) {
            System.out.println("invalid details of user in request");
            throw new RuntimeException("Invalid Username or password");
        }
    }
}
