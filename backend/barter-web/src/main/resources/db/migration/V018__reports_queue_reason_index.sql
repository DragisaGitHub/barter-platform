-- Support moderation queue filtering by report reason without full-table scans.

CREATE INDEX idx_reports_reason_status_created_at_desc
    ON reports (reason_code, status, created_at DESC);

