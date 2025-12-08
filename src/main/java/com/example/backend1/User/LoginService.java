package com.example.backend1.User;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoginService {

    private final Loginrepository repository;

    public LoginService(Loginrepository repository) {
        this.repository = repository;
    }

    /**
     * ตรวจสอบว่า username / password ที่ส่งมา
     * มี record ใน DB อย่างน้อย 1 แถวหรือไม่
     */
    public boolean authenticate(String username, String password) {

        // กัน null จากฝั่งหน้าเว็บ
        if (username == null || password == null) {
            return false;
        }

        // ถ้ามีอย่างน้อย 1 แถว → login ผ่าน
        List<login> users = repository.findByUsernameAndPassword(username, password);
        return !users.isEmpty();
    }
}
