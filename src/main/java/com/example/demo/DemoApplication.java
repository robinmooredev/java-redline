package com.example.demo;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.aspose.words.License;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class DemoApplication {
    private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @PostConstruct
    public void loadAsposeLicense() {
        try {
            // Try filesystem first (for Docker/Railway where license is decoded at runtime)
            java.io.File licFile = new java.io.File("/app/Aspose.WordsforJava.lic");
            if (licFile.exists()) {
                License license = new License();
                license.setLicense(licFile.getAbsolutePath());
                logger.info("Aspose.Words license loaded from filesystem");
                return;
            }

            // Fall back to classpath (for local development)
            try (InputStream is = getClass().getResourceAsStream("/Aspose.WordsforJava.lic")) {
                if (is != null) {
                    License license = new License();
                    license.setLicense(is);
                    logger.info("Aspose.Words license loaded from classpath");
                } else {
                    logger.warn("Aspose.Words license file not found");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load Aspose.Words license", e);
        }
    }
}
