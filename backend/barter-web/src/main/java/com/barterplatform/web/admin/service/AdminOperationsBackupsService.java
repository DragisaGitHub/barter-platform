package com.barterplatform.web.admin.service;

import com.barterplatform.api.model.AdminOperationsBackupsResponse;
import org.springframework.stereotype.Service;

/**
 * Provides placeholder backup status information for the Admin Operations Center.
 *
 * <p>Real backup integration (Azure Blob Storage cron jobs, pg_dump pipelines, restore
 * verification) is deferred. This service returns safe static data that correctly represents
 * the current state — backups are not yet automated — without exposing configuration
 * secrets or storage connection strings.
 */
@Service
public class AdminOperationsBackupsService {

    private static final String AVAILABILITY_PLACEHOLDER = "placeholder";

    public AdminOperationsBackupsResponse getBackups() {
        return new AdminOperationsBackupsResponse()
                .availability(AVAILABILITY_PLACEHOLDER)
                .lastBackupTimestamp(null)
                .nextScheduledBackupTimestamp(null)
                .backupStorageType(null)
                .scheduledBackupEnabled(false)
                .note("Automated backup integration is not yet configured. "
                        + "Manual database snapshots are performed via deployment scripts.");
    }
}

