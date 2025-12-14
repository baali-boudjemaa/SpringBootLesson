package com.example.mef.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.mef.demo.Model.User;

@RestController
public class UserController {
    @GetMapping("/users")
    List<User> getAllUsers(){
        return null;

    }

}
