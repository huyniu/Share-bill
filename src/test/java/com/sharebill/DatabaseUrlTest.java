package com.sharebill;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseUrlTest {

    @Test
    void convertsNeonConnectionString() {
        Map<String, String> p = DatabaseUrl.toSpringProperties(
                "postgresql://neondb_owner:p%40ss@ep-cool-1234.ap-southeast-1.aws.neon.tech/neondb"
                        + "?sslmode=require&channel_binding=require");

        assertEquals("jdbc:postgresql://ep-cool-1234.ap-southeast-1.aws.neon.tech/neondb?sslmode=require",
                p.get("spring.datasource.url"));
        assertEquals("neondb_owner", p.get("spring.datasource.username"));
        assertEquals("p@ss", p.get("spring.datasource.password"));
    }

    @Test
    void keepsPortAndAddsSslWhenMissing() {
        Map<String, String> p = DatabaseUrl.toSpringProperties("postgres://u:pw@db.example.com:6543/app");
        assertEquals("jdbc:postgresql://db.example.com:6543/app?sslmode=require", p.get("spring.datasource.url"));
    }

    @Test
    void rejectsOtherSchemes() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseUrl.toSpringProperties("mysql://u:p@h/db"));
    }
}
