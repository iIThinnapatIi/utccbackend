package com.example.backend1.User;

import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")   // อนุญาตทุก origin (Netlify / Render)
@RestController
public class LoginController {

    private final LoginService service;

    public LoginController(LoginService service){
        this.service = service;
    }

    /**
     * ✔ รับ username / password แบบ RequestParam
     * ✔ ใช้กับ axios.post(LOGIN_URL, null, { params: { username, password } }) ได้ตรง ๆ
     * ✔ ถ้าไม่มี username/password → คืน "Login Failed" แทน error 400
     */
    @PostMapping("/login")
    public String login(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password
    ) {

        // ถ้าขาด param → ไม่ต้องโยน error ให้ Spring → ตอบว่า login ไม่ผ่านเฉย ๆ
        if (username == null || password == null) {
            return "Login Failed";
        }

        boolean valid = service.authenticate(username, password);
        return valid ? "Login Success" : "Login Failed";
    }
}
