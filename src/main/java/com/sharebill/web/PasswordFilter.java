package com.sharebill.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Bảo vệ toàn bộ /api/** bằng một mật khẩu chung (header {@code X-App-Password}).
 * Không đặt APP_PASSWORD thì bỏ qua kiểm tra (tiện khi chạy ở máy tính).
 * Giao diện web (HTML/JS) vẫn tải được, nhưng không đọc/ghi được dữ liệu nếu sai mật khẩu.
 */
@Component
public class PasswordFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-App-Password";

    private final byte[] expected;

    public PasswordFilter(@Value("${sharebill.password:}") String password) {
        this.expected = password.isEmpty() ? null : password.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return expected == null
                || !path.startsWith("/api/")
                || path.equals("/api/health")
                || "OPTIONS".equalsIgnoreCase(request.getMethod()); // CORS preflight
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String given = request.getHeader(HEADER);
        // So sánh thời gian cố định để không lộ mật khẩu qua thời gian phản hồi
        if (given != null && MessageDigest.isEqual(expected, given.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json;charset=UTF-8");
        response.getWriter().write("{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"Sai mật khẩu\"}");
    }
}
