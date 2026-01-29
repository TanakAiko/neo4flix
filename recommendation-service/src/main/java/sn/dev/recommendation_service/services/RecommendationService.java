package sn.dev.recommendation_service.services;

import sn.dev.recommendation_service.web.dto.RecommendationDTO;

import java.util.List;

public interface RecommendationService {

    /**
     * Generates a list of recommendations for the user.
     * 1. Tries Collaborative Filtering (Neo4j).
     * 2. Falls back to Movie Service (Content-based) if needed.
     */
    List<RecommendationDTO> getRecommendations(String userId);
}
