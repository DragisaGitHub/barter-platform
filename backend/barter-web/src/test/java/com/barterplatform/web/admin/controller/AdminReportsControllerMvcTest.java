package com.barterplatform.web.admin.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.AdminReportQueueSummaryResponse;
import com.barterplatform.api.model.ReportDetailResponse;
import com.barterplatform.api.model.ReportPagedResponse;
import com.barterplatform.api.model.ReportReasonCode;
import com.barterplatform.api.model.ReportStatus;
import com.barterplatform.api.model.ReportTargetSummaryResponse;
import com.barterplatform.api.model.ReportTargetType;
import com.barterplatform.api.model.ReportUserSummaryResponse;
import com.barterplatform.application.moderation.service.ReportService;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminReportsControllerMvcTest {

    private static final UUID MODERATOR_UUID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private MockMvc mockMvc;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = mock(ReportService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminReportsController(reportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.clearContext();
        setModeratorUser();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnQueueSummary() throws Exception {
        when(reportService.getQueueSummary())
                .thenReturn(new AdminReportQueueSummaryResponse().openCount(4L).inReviewCount(2L).staleOpenCount(1L).staleThresholdHours(48));

        mockMvc.perform(apiGet("/admin/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCount").value(4))
                .andExpect(jsonPath("$.staleThresholdHours").value(48));

        verify(reportService).getQueueSummary();
    }

    @Test
    void shouldListAdminReportsWithReasonFilter() throws Exception {
        when(reportService.listReports(0, 20, "createdAt,desc", ReportStatus.OPEN, ReportTargetType.ITEM, ReportReasonCode.SPAM_SCAM))
                .thenReturn(new ReportPagedResponse().content(List.of()).page(0).size(20).totalElements(0L).totalPages(0).first(true).last(true).sort("createdAt,desc"));

        mockMvc.perform(apiGet("/admin/reports")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("sort", "createdAt,desc")
                        .queryParam("status", "OPEN")
                        .queryParam("targetType", "ITEM")
                        .queryParam("reasonCode", "SPAM_SCAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));

        verify(reportService).listReports(0, 20, "createdAt,desc", ReportStatus.OPEN, ReportTargetType.ITEM, ReportReasonCode.SPAM_SCAM);
    }

    @Test
    void shouldGetAdminReportDetail() throws Exception {
        UUID reportUuid = UUID.randomUUID();
        when(reportService.getReport(reportUuid)).thenReturn(reportDetail(reportUuid));

        mockMvc.perform(apiGet("/admin/reports/" + reportUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(reportUuid.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(reportService).getReport(reportUuid);
    }

    @Test
    void shouldUpdateAdminReport() throws Exception {
        UUID reportUuid = UUID.randomUUID();
        when(reportService.updateReport(eq(MODERATOR_UUID), eq(reportUuid), eq(new com.barterplatform.api.model.AdminUpdateReportRequest()
                .status(ReportStatus.RESOLVED)
                .resolutionNote("Handled by moderator."))))
                .thenReturn(reportDetail(reportUuid).status(ReportStatus.RESOLVED).resolutionNote("Handled by moderator."));

        mockMvc.perform(apiPatch("/admin/reports/" + reportUuid + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED",
                                  "resolutionNote": "Handled by moderator."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionNote").value("Handled by moderator."));

        verify(reportService).updateReport(eq(MODERATOR_UUID), eq(reportUuid), eq(new com.barterplatform.api.model.AdminUpdateReportRequest()
                .status(ReportStatus.RESOLVED)
                .resolutionNote("Handled by moderator.")));
    }

    @Test
    void shouldUpdateAdminReportAssignment() throws Exception {
        UUID reportUuid = UUID.randomUUID();
        when(reportService.updateReportAssignment(
                eq(MODERATOR_UUID),
                eq(false),
                eq(reportUuid),
                eq(new com.barterplatform.api.model.AdminAssignReportRequest().assigned(true))))
                .thenReturn(reportDetail(reportUuid));

        mockMvc.perform(apiPatch("/admin/reports/" + reportUuid + "/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assigned": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(reportUuid.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(reportService).updateReportAssignment(
                eq(MODERATOR_UUID),
                eq(false),
                eq(reportUuid),
                eq(new com.barterplatform.api.model.AdminAssignReportRequest().assigned(true)));
    }

    private ReportDetailResponse reportDetail(UUID reportUuid) {
        return new ReportDetailResponse()
                .uuid(reportUuid)
                .targetType(ReportTargetType.ITEM)
                .targetUuid(UUID.randomUUID())
                .reasonCode(ReportReasonCode.SPAM_SCAM)
                .status(ReportStatus.OPEN)
                .reporter(new ReportUserSummaryResponse().uuid(UUID.randomUUID()).username("reporter"))
                .assignedModerator(new ReportUserSummaryResponse().uuid(MODERATOR_UUID).username("moderator"))
                .targetSummary(new ReportTargetSummaryResponse().title("Target").subtitle("Subtitle").preview("Preview"));
    }

    private void setModeratorUser() {
        AuthenticatedUser principal = new AuthenticatedUser(MODERATOR_UUID, "moderator", List.of("MODERATOR"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiPatch(String path) {
        return patch("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }
}

