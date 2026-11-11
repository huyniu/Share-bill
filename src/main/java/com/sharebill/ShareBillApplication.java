package com.sharebill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class ShareBillApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(ShareBillApplication.class);

		// Trên mạng: chỉ cần dán chuỗi kết nối Neon vào biến DATABASE_URL
		String databaseUrl = System.getenv("DATABASE_URL");
		if (databaseUrl != null && !databaseUrl.isBlank()) {
			Map<String, String> props = DatabaseUrl.toSpringProperties(databaseUrl);
			props.forEach(System::setProperty);
		}

		app.run(args);
	}

}
