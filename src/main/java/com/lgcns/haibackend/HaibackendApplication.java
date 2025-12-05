package com.lgcns.haibackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HaibackendApplication {

    public static void main(String[] args) {
        // Load .env file
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                .ignoreIfMissing()
                .load();

        // Set environment variables as system properties
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
            System.out.println("Loaded env: " + entry.getKey() + " = " +
                    (entry.getKey().contains("SECRET") || entry.getKey().contains("PASSWORD")
                            ? "***"
                            : entry.getValue()));
        });

        SpringApplication.run(HaibackendApplication.class, args);
    }

}
