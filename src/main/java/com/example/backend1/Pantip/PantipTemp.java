package com.example.backend1.Pantip;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Table(name = "pantip_temp")
@Data
public class PantipTemp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String title;


    @Column(columnDefinition = "TEXT")
    private String preview;


    private String url;


    private LocalDateTime createdAt;
}