package com.barterplatform.application.moderation.service.impl;

import com.barterplatform.api.model.*;
import com.barterplatform.application.common.pagination.PageRequestFactory;
import com.barterplatform.application.common.pagination.PageResponseMapper;
import com.barterplatform.application.moderation.mapper.ReportMapper;
import com.barterplatform.application.moderation.service.ReportService;
import com.barterplatform.application.moderation.service.ReportTargetResolver;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.moderation.report.ReportEntity;
import com.barterplatform.domain.moderation.report.ReportStatus;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.moderation.repository.ReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "status", "targetType");
    private static final EnumSet<ReportStatus> DUPLICATE_BLOCKING_STATUSES = EnumSet.of(
            ReportStatus.OPEN,
            ReportStatus.IN_REVIEW);

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReportTargetResolver reportTargetResolver;
    private final ReportMapper reportMapper;
    private final PageRequestFactory pageRequestFactory;
    private final PageResponseMapper pageResponseMapper;

    public ReportServiceImpl(
            ReportRepository reportRepository,
            UserRepository userRepository,
            ReportTargetResolver reportTargetResolver,
            ReportMapper reportMapper,
            PageRequestFactory pageRequestFactory,
            PageResponseMapper pageResponseMapper) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.reportTargetResolver = reportTargetResolver;
        this.reportMapper = reportMapper;
        this.pageRequestFactory = pageRequestFactory;
        this.pageResponseMapper = pageResponseMapper;
    }

    @Override
    public MessageResponse createReport(UUID reporterUserUuid, CreateReportRequest request) {
        UserEntity reporter = resolveUser(reporterUserUuid);
        com.barterplatform.domain.moderation.report.ReportTargetType targetType = reportMapper.map(request.getTargetType());
        reportTargetResolver.validateForCreate(targetType, request.getTargetUuid(), reporter);

        if (reportRepository.existsByReporterUserIdAndTargetTypeAndTargetUuidAndStatusIn(
                reporter.getId(),
                targetType,
                request.getTargetUuid(),
                DUPLICATE_BLOCKING_STATUSES)) {
            throw conflict("You already have an open report for this target.");
        }

        ReportEntity report = new ReportEntity();
        report.setReporterUserId(reporter.getId());
        report.setTargetType(targetType);
        report.setTargetUuid(request.getTargetUuid());
        report.setReasonCode(reportMapper.map(request.getReasonCode()));
        report.setDetails(normalize(request.getDetails()));
        report.setStatus(ReportStatus.OPEN);
        reportRepository.save(report);

        return new MessageResponse().message("Report submitted successfully.");
    }

    @Override
    @Transactional(readOnly = true)
    public ReportPagedResponse listReports(
            Integer page,
            Integer size,
            String sort,
            com.barterplatform.api.model.ReportStatus status,
            com.barterplatform.api.model.ReportTargetType targetType) {
        PageRequestFactory.ResolvedPageRequest pageRequest = pageRequestFactory.create(
                page,
                size,
                sort,
                DEFAULT_SORT_FIELD,
                ALLOWED_SORT_FIELDS);

        Specification<ReportEntity> specification = buildSpecification(
                reportMapper.map(status),
                reportMapper.map(targetType));

        Page<ReportEntity> reportPage = reportRepository.findAll(specification, pageRequest.pageable());
        Map<Long, UserEntity> usersById = loadUsersById(reportPage.getContent());

        List<com.barterplatform.api.model.ReportSummaryResponse> content = reportPage.getContent().stream()
                .map(report -> reportMapper.toSummaryResponse(
                        report,
                        resolveRequiredUser(usersById, report.getReporterUserId()),
                        resolveOptionalUser(usersById, report.getAssignedModeratorUserId()),
                        reportTargetResolver.resolveSummary(report.getTargetType(), report.getTargetUuid())))
                .toList();

        return pageResponseMapper.toReportPagedResponse(reportPage, content, pageRequest.sort());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponse getReport(UUID reportUuid) {
        ReportEntity report = resolveReport(reportUuid);
        Map<Long, UserEntity> usersById = loadUsersById(List.of(report));
        return reportMapper.toDetailResponse(
                report,
                resolveRequiredUser(usersById, report.getReporterUserId()),
                resolveOptionalUser(usersById, report.getAssignedModeratorUserId()),
                reportTargetResolver.resolveSummary(report.getTargetType(), report.getTargetUuid()));
    }

    @Override
    public ReportDetailResponse updateReport(UUID actorUserUuid, UUID reportUuid, AdminUpdateReportRequest request) {
        UserEntity actor = resolveUser(actorUserUuid);
        ReportEntity report = resolveReport(reportUuid);
        ReportStatus nextStatus = reportMapper.map(request.getStatus());

        validateTransition(report.getStatus(), nextStatus);

        report.setStatus(nextStatus);
        if (nextStatus == ReportStatus.IN_REVIEW) {
            report.setAssignedModeratorUserId(actor.getId());
            report.setResolutionNote(null);
            report.setResolvedAt(null);
        } else {
            if (report.getAssignedModeratorUserId() == null) {
                report.setAssignedModeratorUserId(actor.getId());
            }
            report.setResolutionNote(normalize(request.getResolutionNote()));
            report.setResolvedAt(OffsetDateTime.now());
        }

        ReportEntity saved = reportRepository.save(report);
        Map<Long, UserEntity> usersById = loadUsersById(List.of(saved));
        return reportMapper.toDetailResponse(
                saved,
                resolveRequiredUser(usersById, saved.getReporterUserId()),
                resolveOptionalUser(usersById, saved.getAssignedModeratorUserId()),
                reportTargetResolver.resolveSummary(saved.getTargetType(), saved.getTargetUuid()));
    }

    private Specification<ReportEntity> buildSpecification(
            ReportStatus status,
            com.barterplatform.domain.moderation.report.ReportTargetType targetType) {
        List<Specification<ReportEntity>> specifications = new ArrayList<>();
        if (status != null) {
            specifications.add(ReportSpecifications.statusEquals(status));
        }
        if (targetType != null) {
            specifications.add(ReportSpecifications.targetTypeEquals(targetType));
        }
        return specifications.isEmpty() ? Specification.unrestricted() : Specification.allOf(specifications);
    }

    private Map<Long, UserEntity> loadUsersById(List<ReportEntity> reports) {
        Set<Long> userIds = reports.stream()
                .flatMap(report -> java.util.stream.Stream.of(report.getReporterUserId(), report.getAssignedModeratorUserId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        return userIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    private void validateTransition(ReportStatus currentStatus, ReportStatus nextStatus) {
        if (currentStatus == nextStatus) {
            return;
        }

        boolean allowed = switch (currentStatus) {
            case OPEN -> nextStatus == ReportStatus.IN_REVIEW
                    || nextStatus == ReportStatus.RESOLVED
                    || nextStatus == ReportStatus.DISMISSED;
            case IN_REVIEW -> nextStatus == ReportStatus.RESOLVED
                    || nextStatus == ReportStatus.DISMISSED;
            case RESOLVED, DISMISSED -> false;
        };

        if (!allowed) {
            throw conflict("Report status transition is not allowed.");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ReportEntity resolveReport(UUID reportUuid) {
        return reportRepository.findByUuid(reportUuid)
                .orElseThrow(() -> notFound("Report with uuid '%s' was not found.".formatted(reportUuid)));
    }

    private UserEntity resolveUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> notFound("User with uuid '%s' was not found.".formatted(userUuid)));
    }

    private UserEntity resolveRequiredUser(Map<Long, UserEntity> usersById, Long userId) {
        UserEntity user = usersById.get(userId);
        if (user == null) {
            throw notFound("Report user was not found.");
        }
        return user;
    }

    private UserEntity resolveOptionalUser(Map<Long, UserEntity> usersById, Long userId) {
        return userId == null ? null : usersById.get(userId);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, message);
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, message);
    }
}

