package edu.softwarelab.starter.contract;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@WireMockTest(httpPort = 8089)
public class S3WebhookStubTest {

    // خواندن آدرس تیم همسایه (در صورت نبودن، از لوکال‌هاستِ استاب استفاده می‌کند)
    @Value("${NEIGHBOR_BASE_URL:http://localhost:8089}")
    private String neighborBaseUrl;

    @Test
    public void testCallS3SettlementCallback_Success() {
        // ۱. تنظیم Stub: به WireMock می‌گوییم اگر درخواستی به S3 آمد، چه جوابی بدهد
        stubFor(post(urlEqualTo("/api/v1/settlement-callbacks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"received\"}")));

        // ۲. رفتار سرویس شما: ارسال درخواست به همسایه
        RestTemplate restTemplate = new RestTemplate();
        String payload = "{\"transactionId\": \"tx-1001\", \"status\": \"COMPLETED\"}";

        ResponseEntity<String> response = restTemplate.postForEntity(
                neighborBaseUrl + "/api/v1/settlement-callbacks",
                payload,
                String.class
        );

        // ۳. بررسی (Assert) اینکه سرویس شما جواب موفق دریافت کرده است
        assertEquals(200, response.getStatusCode().value());

        // تایید اینکه سرویس شما واقعاً همسایه را صدا زده است
        verify(1, postRequestedFor(urlEqualTo("/api/v1/settlement-callbacks")));
    }
}