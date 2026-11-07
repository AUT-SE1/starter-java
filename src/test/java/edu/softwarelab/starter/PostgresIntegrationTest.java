package edu.softwarelab.starter;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * پایه‌ی تست‌های یکپارچه: یک Postgres واقعی در Docker (یک بار برای همه‌ی کلاس‌ها).
 * به Docker روشن نیاز دارد. listenerهای Kafka در تست غیرفعال‌اند.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "kafka.listeners.auto-startup=false",
        "settlement.rtgs.latency-ms=0"
})
public abstract class PostgresIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
