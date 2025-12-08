package com.example.backend1.User;

import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final Loginrepository repository;

    public LoginService(Loginrepository repository) {
        this.repository = repository;
    }

    /**
     * ✔ ใช้เมธอดเดิมของ Loginrepository: findByUsernameAndPassword
     * ✔ กัน username / password เป็น null เพื่อป้องกัน error
     * ✔ ถ้าไม่เจอ user หรือ password ไม่ตรง → คืน false (ไม่ error 500)
     */
    public boolean authenticate(String username, String password) {

        // ⭐ กันเคส frontend ส่ง null → ป้องกัน NullPointerException
        if (username == null || password == null) {
            return false;
        }

        // ⭐ ถ้ามี row ใน DB ที่ username + password ตรงกัน → login สำเร็จ
        return repository
                .findByUsernameAndPassword(username, password)
                .isPresent();
    }
}
