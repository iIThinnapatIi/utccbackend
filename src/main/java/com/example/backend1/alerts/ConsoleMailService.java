package com.example.backend1.alerts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class ConsoleMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMailService.class);

    @Override
    public void send(String to, String subject, String body) {
        // ตอนนี้ log ออกแทนการส่งจริง
        log.warn("MAIL -> to: {} | subject: {}\n{}", to, subject, body);
    }
}
