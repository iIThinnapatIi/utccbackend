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
     * ✔ รับ username/password แบบ RequestParam
     * ✔ รองรับ axios.post(..., null, { params:{} })
     * ✔ ใช้งานกับหน้าเว็บ Netlify ได้ทันที
     * ✔ ไม่ใช้ @RequestBody เพราะ frontend ส่งแบบ query param
     */
    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password
    ) {

        boolean valid = service.authenticate(username, password);
        return valid ? "Login Success" : "Login Failed";
    }
}
