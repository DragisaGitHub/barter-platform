package com.barterplatform.infrastructure.moderation.repository;

import com.barterplatform.domain.moderation.report.entity.ReportHistoryEntryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportHistoryEntryRepository extends JpaRepository<ReportHistoryEntryEntity, Long> {

    List<ReportHistoryEntryEntity> findByReportIdOrderByCreatedAtDescIdDesc(Long reportId);
}