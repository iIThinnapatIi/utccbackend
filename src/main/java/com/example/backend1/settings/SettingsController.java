package com.example.backend1.settings;

import org.springframework.web.bind.annotation.*;

// ถ้าต้องการให้ front-end เรียกข้ามโดเมน ใส่ @CrossOrigin ได้
//@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService service;

    // ✅ เขียน constructor เอง (แทน Lombok @RequiredArgsConstructor)
    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    public SettingsDto get() {
        return service.get();
    }

    @PutMapping
    public SettingsDto update(@RequestBody SettingsDto dto) {
        return service.update(dto);
    }
}
