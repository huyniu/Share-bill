package com.sharebill.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    /** Render gọi để kiểm tra server còn sống (không cần mật khẩu). */
    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    /** App gọi để kiểm tra mật khẩu: qua được PasswordFilter là đúng. */
    @GetMapping("/api/auth/check")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void checkPassword() {
    }
}
