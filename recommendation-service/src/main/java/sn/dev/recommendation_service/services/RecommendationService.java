package sn.dev.recommendation_service.services;

import sn.dev.recommendation_service.web.dto.RecommendationDTO;
import sn.dev.recommendation_service.web.dto.ShareRequestDTO;
import sn.dev.recommendation_service.web.dto.SharedRecommendationDTO;

import java.util.List;

public interface RecommendationService {

    /**
     * Generates a list of recommendations for the user.
     * 1. Tries Collaborative Filtering (Neo4j).
     * 2. Falls back to Movie Service (Content-based) if needed.
     */
    List<RecommendationDTO> getRecommendations(String userId);

    /**
     * Share a movie recommendation with another user.
     */
    void shareRecommendation(ShareRequestDTO request);

    /**
     * Get all recommendations shared with the current user.
     */
    List<SharedRecommendationDTO> getReceivedRecommendations();

    /**
     * Get all recommendations the current user has shared with others.
     */
    List<SharedRecommendationDTO> getSentRecommendations();
}
