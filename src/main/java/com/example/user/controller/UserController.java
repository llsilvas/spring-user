package com.example.user.controller;

import com.example.user.entities.User;
import com.example.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String getUser(){
        return "Service Users";
    }

    @PostMapping
    public User save(@RequestBody User user) {
        return userService.save(user);
    }
}
