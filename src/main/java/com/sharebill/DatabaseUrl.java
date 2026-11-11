package com.sharebill;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chuyển chuỗi kết nối kiểu Neon/Heroku ({@code postgresql://user:pass@host/db?sslmode=require})
 * thành các thuộc tính spring.datasource.* mà JDBC hiểu.
 */
final class DatabaseUrl {

    private DatabaseUrl() {
    }

    static Map<String, String> toSpringProperties(String databaseUrl) {
        URI uri = URI.create(databaseUrl.trim());
        String scheme = uri.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            throw new IllegalArgumentException("DATABASE_URL phải bắt đầu bằng postgresql:// hoặc postgres://");
        }

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() != -1) {
            jdbc.append(':').append(uri.getPort());
        }
        jdbc.append(uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/postgres" : uri.getRawPath());

        // Giữ các tham số JDBC hiểu được (sslmode...), bỏ channel_binding vì driver dùng tên khác
        List<String> params = new ArrayList<>();
        if (uri.getRawQuery() != null) {
            for (String p : uri.getRawQuery().split("&")) {
                if (!p.isEmpty() && !p.startsWith("channel_binding=")) {
                    params.add(p);
                }
            }
        }
        if (params.stream().noneMatch(p -> p.startsWith("sslmode="))) {
            params.add("sslmode=require");
        }
        jdbc.append('?').append(String.join("&", params));

        Map<String, String> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", jdbc.toString());
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            String user = colon >= 0 ? userInfo.substring(0, colon) : userInfo;
            props.put("spring.datasource.username", decode(user));
            if (colon >= 0) {
                props.put("spring.datasource.password", decode(userInfo.substring(colon + 1)));
            }
        }
        return props;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
