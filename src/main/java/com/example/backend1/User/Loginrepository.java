package com.example.backend1.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Loginrepository extends JpaRepository<login,Integer> {

    Optional<login> findByUsernameAndPassword(String username, String password);
}
