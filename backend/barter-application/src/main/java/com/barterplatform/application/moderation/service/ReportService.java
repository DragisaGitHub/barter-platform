package com.barterplatform.application.moderation.service;

import com.barterplatform.api.model.AdminUpdateReportRequest;
import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.api.model.ReportDetailResponse;
import com.barterplatform.api.model.ReportPagedResponse;
import com.barterplatform.api.model.ReportStatus;
import com.barterplatform.api.model.ReportTargetType;
import com.barterplatform.api.model.CreateReportRequest;
import java.util.UUID;

public interface ReportService {

    MessageResponse createReport(UUID reporterUserUuid, CreateReportRequest request);

    ReportPagedResponse listReports(
            Integer page,
            Integer size,
            String sort,
            ReportStatus status,
            ReportTargetType targetType);

    ReportDetailResponse getReport(UUID reportUuid);

    ReportDetailResponse updateReport(UUID actorUserUuid, UUID reportUuid, AdminUpdateReportRequest request);
}

