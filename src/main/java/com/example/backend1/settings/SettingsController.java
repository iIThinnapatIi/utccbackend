package com.example.backend1.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings") // ตรงกับที่ frontend เรียก
@RequiredArgsConstructor
@CrossOrigin(
        origins = { "http://localhost:5173", "http://127.0.0.1:5173" },
        allowedHeaders = "*",
        methods = { RequestMethod.GET, RequestMethod.PUT },
        allowCredentials = "true"
)
public class SettingsController {

    private final SettingsService service;

    @GetMapping
    public SettingsDto get() {
        return service.get();
    }

    @PutMapping
    public SettingsDto update(@RequestBody SettingsDto body) {
        return service.update(body);
    }
}
