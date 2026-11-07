package edu.softwarelab.starter.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteExternalTransferTest {

    @Test
    void testDtoSerializationAndDeserialization() throws Exception {
        // تنظیمات ObjectMapper برای پشتیبانی از تاریخ و زمان (OffsetDateTime)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // ساخت یک شیء نمونه
        ExecuteExternalTransfer dto = new ExecuteExternalTransfer();
        UUID messageId = UUID.randomUUID();
        dto.setMessageId(messageId);
        dto.setTransactionId(UUID.randomUUID());
        dto.setOccurredAt(OffsetDateTime.now());

        ExecuteExternalTransfer.TransferData data = new ExecuteExternalTransfer.TransferData();
        data.setAmount(2500000L);
        data.setDestIban("IR430120000000000000000001");
        dto.setData(data);

        // تبدیل شیء به رشته JSON (Serialization)
        String json = mapper.writeValueAsString(dto);

        // بررسی وجود کلیدهای استاندارد در رشته JSON
        assertTrue(json.contains("\"message_id\""));
        assertTrue(json.contains("\"dest_iban\""));
        assertTrue(json.contains("\"IR430120000000000000000001\""));

        // تبدیل مجدد JSON به شیء جاوا (Deserialization)
        ExecuteExternalTransfer deserializedDto = mapper.readValue(json, ExecuteExternalTransfer.class);

        // بررسی صحت مقادیر نگاشت‌شده
        assertEquals(messageId, deserializedDto.getMessageId());
        assertNotNull(deserializedDto.getData());
        assertEquals(2500000L, deserializedDto.getData().getAmount());
    }
}