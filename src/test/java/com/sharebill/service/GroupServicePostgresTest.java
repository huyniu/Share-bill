package com.sharebill.service;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

/**
 * Chạy lại toàn bộ test của GroupServiceTest trên PostgreSQL thật (nhúng, không cần Docker),
 * để chắc chắn app chạy đúng trên Neon như trên H2.
 * <p>
 * DirtiesContext đóng Spring context (và pool kết nối) ngay sau lớp test này;
 * Postgres nhúng được tắt khi JVM kết thúc.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GroupServicePostgresTest extends GroupServiceTest {

    private static final EmbeddedPostgres POSTGRES = start();

    private static EmbeddedPostgres start() {
        try {
            EmbeddedPostgres pg = EmbeddedPostgres.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    pg.close();
                } catch (IOException ignored) {
                    // JVM đang tắt
                }
            }));
            return pg;
        } catch (IOException e) {
            throw new IllegalStateException("Không khởi động được Postgres nhúng", e);
        }
    }

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }
}
