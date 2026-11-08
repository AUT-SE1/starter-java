package edu.softwarelab.starter.controller;

import edu.softwarelab.starter.dto.SettlementDailyReportEvent;
import edu.softwarelab.starter.dto.SettlementReportSummary;
import edu.softwarelab.starter.service.SettlementReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettlementReportController.class)
class SettlementReportApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SettlementReportService reportService;

    @Test
    @DisplayName("API 1: دریافت موفق گزارش با تاریخ معتبر ISO (200 OK)")
    void testGetReportSuccess() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 20);
        UUID reportId = UUID.randomUUID();

        SettlementDailyReportEvent.ReportData mockData = new SettlementDailyReportEvent.ReportData(
                reportId,
                date,
                OffsetDateTime.now(),
                new SettlementReportSummary(2L, 200_000L, 2L, 0L),
                Collections.emptyList()
        );

        SettlementDailyReportEvent mockEvent = new SettlementDailyReportEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, "S5-STL", "SettlementDailyReport.v1", mockData
        );

        when(reportService.generateDailyReport(eq(date), any(), any())).thenReturn(mockEvent);

        mockMvc.perform(get("/api/v1/settlement-reports")
                        .param("date", "2026-10-20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.report_id").value(reportId.toString()))
                .andExpect(jsonPath("$.business_date").value("2026-10-20"))
                .andExpect(jsonPath("$.summary.totalCount").value(2))
                .andExpect(jsonPath("$.summary.totalAmount").value(200000));
    }

    @Test
    @DisplayName("API 2: ارسال تاریخ نامعتبر و دریافت خطای اعتبارسنجی (400 Bad Request)")
    void testGetReportInvalidDate() throws Exception {
        mockMvc.perform(get("/api/v1/settlement-reports")
                        .param("date", "invalid-date-format")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}