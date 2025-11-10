package com.example.backend1.ingest.pantip;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ingest/pantip")
public class PantipController {

    private final PantipService pantipService;

    public PantipController(PantipService pantipService) {
        this.pantipService = pantipService;
    }

    @GetMapping("/pantip")
    public List<PantipPost> getPantip(@RequestParam String keyword) {
        return pantipService.searchPosts(keyword);
    }
}
