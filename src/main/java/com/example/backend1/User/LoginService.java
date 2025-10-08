package com.example.backend1.User;

import java.util.Optional;

@org.springframework.stereotype.Service
public class LoginService {
    private final Loginrepository repository;

    public LoginService(Loginrepository repository) {
        this.repository = repository;
    }

    public boolean authenticate(String username, String password) {
        return repository.findByUsernameAndPassword(username, password).isPresent();
    }
}
