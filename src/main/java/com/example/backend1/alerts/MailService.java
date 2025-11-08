package com.example.backend1.alerts;
public interface MailService {
    void send(String to, String subject, String body);
}
