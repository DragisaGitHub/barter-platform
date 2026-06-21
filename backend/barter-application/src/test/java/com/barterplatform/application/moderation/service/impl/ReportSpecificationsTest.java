package com.barterplatform.application.moderation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.domain.moderation.report.entity.ReportEntity;
import com.barterplatform.domain.moderation.report.enums.ReportReasonCode;
import com.barterplatform.domain.moderation.report.enums.ReportStatus;
import com.barterplatform.domain.moderation.report.enums.ReportTargetType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ReportSpecificationsTest {

    @Mock private Root<ReportEntity> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder criteriaBuilder;
    @Mock private Path<Object> path;
    @Mock private Predicate predicate;

    @BeforeEach
    void setUp() {
        when(root.get(any(String.class))).thenReturn(path);
    }

    @Test
    void statusEquals_createsEqualPredicate() {
        when(criteriaBuilder.equal(path, ReportStatus.OPEN)).thenReturn(predicate);

        Specification<ReportEntity> spec = ReportSpecifications.statusEquals(ReportStatus.OPEN);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertEquals(predicate, result);
        verify(root).get("status");
        verify(criteriaBuilder).equal(path, ReportStatus.OPEN);
    }

    @Test
    void targetTypeEquals_createsEqualPredicate() {
        when(criteriaBuilder.equal(path, ReportTargetType.ITEM)).thenReturn(predicate);

        Specification<ReportEntity> spec = ReportSpecifications.targetTypeEquals(ReportTargetType.ITEM);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertEquals(predicate, result);
        verify(root).get("targetType");
    }

    @Test
    void reasonCodeEquals_createsEqualPredicate() {
        when(criteriaBuilder.equal(path, ReportReasonCode.PROHIBITED_ITEM)).thenReturn(predicate);

        Specification<ReportEntity> spec = ReportSpecifications.reasonCodeEquals(ReportReasonCode.PROHIBITED_ITEM);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertEquals(predicate, result);
        verify(root).get("reasonCode");
    }

    @Test
    void assignedToModerator_createsEqualPredicate() {
        when(criteriaBuilder.equal(path, 42L)).thenReturn(predicate);

        Specification<ReportEntity> spec = ReportSpecifications.assignedToModerator(42L);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertEquals(predicate, result);
        verify(root).get("assignedModeratorUserId");
        verify(criteriaBuilder).equal(path, 42L);
    }

    @Test
    void unassigned_createsIsNullPredicate() {
        when(criteriaBuilder.isNull(path)).thenReturn(predicate);

        Specification<ReportEntity> spec = ReportSpecifications.unassigned();
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertEquals(predicate, result);
        verify(root).get("assignedModeratorUserId");
        verify(criteriaBuilder).isNull(path);
    }

    @Test
    @SuppressWarnings("unchecked")
    void staleBefore_combinesStatusOpenAndCreatedAtLessThan() {
        Predicate statusPredicate = Mockito.mock(Predicate.class);
        Predicate datePredicate = Mockito.mock(Predicate.class);
        Predicate combinedPredicate = Mockito.mock(Predicate.class);

        Path<Object> statusPath = Mockito.mock(Path.class);
        Path<Object> createdAtPath = Mockito.mock(Path.class);

        when(root.get("status")).thenReturn(statusPath);
        when(root.get("createdAt")).thenReturn(createdAtPath);
        when(criteriaBuilder.equal(statusPath, ReportStatus.OPEN)).thenReturn(statusPredicate);
        when(criteriaBuilder.lessThan(any(), any(OffsetDateTime.class))).thenReturn(datePredicate);
        when(criteriaBuilder.and(statusPredicate, datePredicate)).thenReturn(combinedPredicate);

        OffsetDateTime threshold = OffsetDateTime.now().minusHours(48);
        Specification<ReportEntity> spec = ReportSpecifications.staleBefore(threshold);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertEquals(combinedPredicate, result);
    }
}
