package com.example.backend1.User;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class LoginController {
    private final LoginService service;

    public LoginController(LoginService service){
        this.service = service;
    }

    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        boolean valid = service.authenticate(username, password);
        return valid ? "Login Success" : "Login Failed";
    }
}
