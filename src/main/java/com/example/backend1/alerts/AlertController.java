package com.example.backend1.alerts;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    private final AlertService service;
    private final MailService mail;

    public AlertController(AlertService service, MailService mail) {
        this.service = service;
        this.mail = mail;
    }

    /** ตรวจอัตรา negative แล้วแจ้งเตือนถ้าเกิน threshold */
    @GetMapping("/evaluate")
    public Map<String, String> evaluate() {
        String result = service.evaluateAndNotify();
        return Map.of("result", result);
    }

    /** ส่งเมลทดสอบ (ตอนนี้เป็นการ log แทนการส่งจริง) */
    @PostMapping("/send-test")
    public Map<String, String> sendTest(
            @RequestParam(defaultValue = "marketing@utcc.local") String to) {
        mail.send(to, "UTCC Test", "This is a test message.");
        return Map.of("status", "sent", "to", to);
    }
}
