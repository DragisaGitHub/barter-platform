package com.barterplatform.web.admin.service;

import com.barterplatform.api.model.AdminOperationsDeploymentResponse;
import com.barterplatform.api.model.AdminOperationsHealthResponse;
import com.barterplatform.api.model.AdminOperationsMarketplaceResponse;
import com.barterplatform.api.model.AdminOperationsModerationResponse;
import com.barterplatform.api.model.AdminOperationsOverviewResponse;
import com.barterplatform.api.model.AdminOperationsStorageResponse;
import com.barterplatform.api.model.AdminOperationsSystemResponse;
import com.barterplatform.api.model.AdminOperationsUsersResponse;
import com.barterplatform.domain.catalog.enums.ItemStatus;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.domain.moderation.report.ReportStatus;
import com.barterplatform.domain.reputation.enums.TradeReviewRating;
import com.barterplatform.domain.trade.enums.TradeOfferStatus;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.moderation.repository.ReportRepository;
import com.barterplatform.infrastructure.reputation.repository.TradeReviewRepository;
import com.barterplatform.infrastructure.trade.repository.TradeOfferRepository;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOperationsOverviewService {

    private static final String DEFAULT_PROFILE = "default";
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DEGRADED = "DEGRADED";
    private static final String STATUS_DOWN = "DOWN";
    private static final String STORAGE_CONFIGURED_NOT_CHECKED = "CONFIGURED_NOT_CHECKED";
    private static final String DEPLOYMENT_UNAVAILABLE = "unavailable";
    private static final String DEPLOYMENT_CONFIGURED = "configured";

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final TradeOfferRepository tradeOfferRepository;
    private final ReportRepository reportRepository;
    private final TradeReviewRepository tradeReviewRepository;
    private final ItemImageRepository itemImageRepository;
    private final DataSource dataSource;
    private final Environment environment;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;
    private final String storageProviderType;

    public AdminOperationsOverviewService(
            UserRepository userRepository,
            ItemRepository itemRepository,
            TradeOfferRepository tradeOfferRepository,
            ReportRepository reportRepository,
            TradeReviewRepository tradeReviewRepository,
            ItemImageRepository itemImageRepository,
            DataSource dataSource,
            Environment environment,
            ObjectProvider<BuildProperties> buildPropertiesProvider,
            @Value("${barter.storage.type:local}") String storageProviderType) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.tradeOfferRepository = tradeOfferRepository;
        this.reportRepository = reportRepository;
        this.tradeReviewRepository = tradeReviewRepository;
        this.itemImageRepository = itemImageRepository;
        this.dataSource = dataSource;
        this.environment = environment;
        this.buildPropertiesProvider = buildPropertiesProvider;
        this.storageProviderType = normalizeStorageProvider(storageProviderType);
    }

    @Transactional(readOnly = true)
    public AdminOperationsOverviewResponse getOverview() {
        String databaseStatus = databaseStatus();
        OffsetDateTime lastDeploymentTimestamp = lastDeploymentTimestamp();

        return new AdminOperationsOverviewResponse()
                .system(system())
                .health(health(databaseStatus))
                .users(users())
                .marketplace(marketplace())
                .moderation(moderation())
                .storage(storage())
                .deployment(deployment(lastDeploymentTimestamp));
    }

    private AdminOperationsSystemResponse system() {
        return new AdminOperationsSystemResponse()
                .applicationName(environment.getProperty("spring.application.name", "barter-platform"))
                .applicationVersion(applicationVersion())
                .activeProfiles(activeProfiles())
                .serverTime(OffsetDateTime.now(ZoneOffset.UTC))
                .uptimeSeconds(Math.max(0, ManagementFactory.getRuntimeMXBean().getUptime() / 1_000));
    }

    private AdminOperationsHealthResponse health(String databaseStatus) {
        return new AdminOperationsHealthResponse()
                .overallStatus(STATUS_UP.equals(databaseStatus) ? STATUS_UP : STATUS_DEGRADED)
                .databaseStatus(databaseStatus)
                .storageProviderType(storageProviderType)
                .storageStatus(STORAGE_CONFIGURED_NOT_CHECKED)
                .storageStatusDetail("Configured provider is reported without remote object-storage probing.");
    }

    private AdminOperationsUsersResponse users() {
        return new AdminOperationsUsersResponse()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByStatus(UserStatus.ACTIVE))
                .suspendedUsers(userRepository.countByStatus(UserStatus.SUSPENDED))
                .bannedUsers(userRepository.countByStatus(UserStatus.BANNED))
                .pendingVerificationUsers(userRepository.countByStatus(UserStatus.PENDING_VERIFICATION));
    }

    private AdminOperationsMarketplaceResponse marketplace() {
        return new AdminOperationsMarketplaceResponse()
                .totalItems(itemRepository.count())
                .activeListings(itemRepository.countByStatusAndDeletedAtIsNull(ItemStatus.ACTIVE))
                .removedListings(itemRepository.countByStatus(ItemStatus.REMOVED))
                .openTradeOffers(tradeOfferRepository.countByStatus(TradeOfferStatus.PENDING))
                .completedTrades(tradeOfferRepository.countByStatus(TradeOfferStatus.COMPLETED));
    }

    private AdminOperationsModerationResponse moderation() {
        return new AdminOperationsModerationResponse()
                .openReports(reportRepository.countByStatus(ReportStatus.OPEN))
                .inReviewReports(reportRepository.countByStatus(ReportStatus.IN_REVIEW))
                .resolvedReports(reportRepository.countByStatus(ReportStatus.RESOLVED))
                .dismissedReports(reportRepository.countByStatus(ReportStatus.DISMISSED))
                .negativeReviews(tradeReviewRepository.countByRating(TradeReviewRating.NEGATIVE));
    }

    private AdminOperationsStorageResponse storage() {
        return new AdminOperationsStorageResponse()
                .totalImageRecords(itemImageRepository.count())
                .primaryImageCount(itemImageRepository.countByPrimaryTrue())
                .storageProviderType(storageProviderType);
    }

    private AdminOperationsDeploymentResponse deployment(OffsetDateTime lastDeploymentTimestamp) {
        return new AdminOperationsDeploymentResponse()
                .environment(String.join(",", activeProfiles()))
                .deploymentStateAvailability(lastDeploymentTimestamp == null ? DEPLOYMENT_UNAVAILABLE : DEPLOYMENT_CONFIGURED)
                .lastDeploymentTimestamp(lastDeploymentTimestamp);
    }

    private String databaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1) ? STATUS_UP : STATUS_DOWN;
        } catch (Exception ex) {
            return STATUS_DOWN;
        }
    }

    private String applicationVersion() {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        if (buildProperties != null && buildProperties.getVersion() != null && !buildProperties.getVersion().isBlank()) {
            return buildProperties.getVersion();
        }
        return AdminOperationsOverviewService.class.getPackage().getImplementationVersion();
    }

    private List<String> activeProfiles() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return List.of(DEFAULT_PROFILE);
        }
        return Arrays.asList(activeProfiles);
    }

    private OffsetDateTime lastDeploymentTimestamp() {
        String value = firstNonBlank(
                environment.getProperty("barter.deployment.deployed-at"),
                environment.getProperty("BARTER_DEPLOYED_AT"));
        if (value == null) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private String normalizeStorageProvider(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase();
    }
}

