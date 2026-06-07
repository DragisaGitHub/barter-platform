package com.barterplatform.infrastructure.moderation.repository;

import com.barterplatform.domain.moderation.report.entity.ReportEntity;
import com.barterplatform.domain.moderation.report.enums.ReportStatus;
import com.barterplatform.domain.moderation.report.enums.ReportTargetType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReportRepository extends JpaRepository<ReportEntity, Long>, JpaSpecificationExecutor<ReportEntity> {

    Optional<ReportEntity> findByUuid(UUID uuid);

    boolean existsByReporterUserIdAndTargetTypeAndTargetUuidAndStatusIn(
            Long reporterUserId,
            ReportTargetType targetType,
            UUID targetUuid,
            Collection<ReportStatus> statuses);

    long countByStatus(ReportStatus status);

    long countByStatusAndCreatedAtBefore(ReportStatus status, OffsetDateTime createdAtBefore);
}

