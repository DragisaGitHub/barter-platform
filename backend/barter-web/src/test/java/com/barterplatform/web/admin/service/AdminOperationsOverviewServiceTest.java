package com.barterplatform.web.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;

class AdminOperationsOverviewServiceTest {

    private UserRepository userRepository;
    private ItemRepository itemRepository;
    private TradeOfferRepository tradeOfferRepository;
    private ReportRepository reportRepository;
    private TradeReviewRepository tradeReviewRepository;
    private ItemImageRepository itemImageRepository;
    private DataSource dataSource;
    private Connection connection;
    private Environment environment;
    private ObjectProvider<BuildProperties> buildPropertiesProvider;
    private AdminOperationsOverviewService service;

    @BeforeEach
    void setUp() throws Exception {
        userRepository = mock(UserRepository.class);
        itemRepository = mock(ItemRepository.class);
        tradeOfferRepository = mock(TradeOfferRepository.class);
        reportRepository = mock(ReportRepository.class);
        tradeReviewRepository = mock(TradeReviewRepository.class);
        itemImageRepository = mock(ItemImageRepository.class);
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        environment = mock(Environment.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
        buildPropertiesProvider = provider;

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);
        when(environment.getProperty("spring.application.name", "barter-platform")).thenReturn("barter-platform");
        when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});
        when(environment.getProperty("barter.deployment.deployed-at")).thenReturn("2026-05-27T10:15:30Z");
        when(buildPropertiesProvider.getIfAvailable()).thenReturn(null);

        service = new AdminOperationsOverviewService(
                userRepository,
                itemRepository,
                tradeOfferRepository,
                reportRepository,
                tradeReviewRepository,
                itemImageRepository,
                dataSource,
                environment,
                buildPropertiesProvider,
                "azure");
    }

    @Test
    void getOverviewAggregatesOperationalCountersWithoutSensitiveDetails() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(7L);
        when(userRepository.countByStatus(UserStatus.SUSPENDED)).thenReturn(1L);
        when(userRepository.countByStatus(UserStatus.BANNED)).thenReturn(1L);
        when(userRepository.countByStatus(UserStatus.PENDING_VERIFICATION)).thenReturn(1L);
        when(itemRepository.count()).thenReturn(20L);
        when(itemRepository.countByStatusAndDeletedAtIsNull(ItemStatus.ACTIVE)).thenReturn(15L);
        when(itemRepository.countByStatus(ItemStatus.REMOVED)).thenReturn(2L);
        when(tradeOfferRepository.countByStatus(TradeOfferStatus.PENDING)).thenReturn(4L);
        when(tradeOfferRepository.countByStatus(TradeOfferStatus.COMPLETED)).thenReturn(3L);
        when(reportRepository.countByStatus(ReportStatus.OPEN)).thenReturn(5L);
        when(reportRepository.countByStatus(ReportStatus.IN_REVIEW)).thenReturn(2L);
        when(reportRepository.countByStatus(ReportStatus.RESOLVED)).thenReturn(8L);
        when(reportRepository.countByStatus(ReportStatus.DISMISSED)).thenReturn(1L);
        when(tradeReviewRepository.countByRating(TradeReviewRating.NEGATIVE)).thenReturn(6L);
        when(itemImageRepository.count()).thenReturn(25L);
        when(itemImageRepository.countByPrimaryTrue()).thenReturn(14L);

        var overview = service.getOverview();

        assertThat(overview.getSystem().getApplicationName()).isEqualTo("barter-platform");
        assertThat(overview.getSystem().getActiveProfiles()).containsExactly("test");
        assertThat(overview.getHealth().getOverallStatus()).isEqualTo("UP");
        assertThat(overview.getHealth().getDatabaseStatus()).isEqualTo("UP");
        assertThat(overview.getHealth().getStorageProviderType()).isEqualTo("azure");
        assertThat(overview.getUsers().getTotalUsers()).isEqualTo(10L);
        assertThat(overview.getUsers().getActiveUsers()).isEqualTo(7L);
        assertThat(overview.getMarketplace().getActiveListings()).isEqualTo(15L);
        assertThat(overview.getMarketplace().getOpenTradeOffers()).isEqualTo(4L);
        assertThat(overview.getModeration().getOpenReports()).isEqualTo(5L);
        assertThat(overview.getModeration().getNegativeReviews()).isEqualTo(6L);
        assertThat(overview.getStorage().getTotalImageRecords()).isEqualTo(25L);
        assertThat(overview.getStorage().getPrimaryImageCount()).isEqualTo(14L);
        assertThat(overview.getDeployment().getEnvironment()).isEqualTo("test");
        assertThat(overview.getDeployment().getDeploymentStateAvailability()).isEqualTo("configured");
        assertThat(overview.getDeployment().getLastDeploymentTimestamp()).isNotNull();
    }

    @Test
    void getOverviewMarksDatabaseDownWhenValidationFails() throws Exception {
        when(connection.isValid(1)).thenReturn(false);

        var overview = service.getOverview();

        assertThat(overview.getHealth().getDatabaseStatus()).isEqualTo("DOWN");
        assertThat(overview.getHealth().getOverallStatus()).isEqualTo("DEGRADED");
    }
}

