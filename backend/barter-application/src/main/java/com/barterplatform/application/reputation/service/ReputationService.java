package com.barterplatform.application.reputation.service;

import com.barterplatform.api.model.ReputationSummaryResponse;

public interface ReputationService {

    ReputationSummaryResponse getReputationSummary(Long reviewedUserId);
}

