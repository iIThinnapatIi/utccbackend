package com.example.backend1.User;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(Loginrepository repository) {
        return args -> {

            if (repository.count() == 0) {
                repository.save(new login("UTCC1", "1234"));
                repository.save(new login("admin", "admin"));
                System.out.println("✅ Default users created");
            } else {
                System.out.println("ℹ Users already exist");
            }

        };
    }
}
