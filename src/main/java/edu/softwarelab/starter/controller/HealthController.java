package edu.softwarelab.starter.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * بررسی وضعیت اجرای پروسس (Liveness)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    /**
     * بررسی آمادگی سرویس و اتصال به دیتابیس طبق الزامات GEN-06 و GEN-10
     */
    @GetMapping("/ready")
    public ResponseEntity<?> ready(HttpServletRequest request) {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) { // بررسی با مهلت ۲ ثانیه
                return ResponseEntity.ok(Map.of(
                        "status", "UP",
                        "database", "UP"
                ));
            } else {
                return buildUnavailableResponse("Database validation query timed out or connection is invalid.", request);
            }
        } catch (Exception e) {
            return buildUnavailableResponse("Failed to connect to PostgreSQL database: " + e.getMessage(), request);
        }
    }

    /**
     * ساخت ساختار استاندارد خطای RFC 9457 و application/problem+json
     */
    private ResponseEntity<Map<String, Object>> buildUnavailableResponse(String detail, HttpServletRequest request) {
        // دریافت یا تولید correlation_id طبق الزام GEN-08
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("type", "https://errors.labbank.example/service-unavailable");
        errorBody.put("title", "Service Unavailable");
        errorBody.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        errorBody.put("code", "SERVICE_UNAVAILABLE");
        errorBody.put("detail", detail);
        errorBody.put("correlation_id", correlationId);

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(errorBody);
    }
}