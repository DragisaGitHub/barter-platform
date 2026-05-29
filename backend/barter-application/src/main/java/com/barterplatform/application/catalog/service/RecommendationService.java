package com.barterplatform.application.catalog.service;

import com.barterplatform.api.model.RecommendationPagedResponse;
import java.util.UUID;

public interface RecommendationService {

    RecommendationPagedResponse listRecommendations(UUID requesterUuid, Integer page, Integer size, String sort);
}

