package sn.dev.recommendation_service.web.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import sn.dev.recommendation_service.web.dto.RecommendationDTO;
import sn.dev.recommendation_service.web.dto.ShareRequestDTO;
import sn.dev.recommendation_service.web.dto.SharedRecommendationDTO;

import java.util.List;

@RequestMapping("/api/recommendations")
public interface RecommendationController {

    /**
     * Get personalized recommendations for the authenticated user.
     */
    @GetMapping
    ResponseEntity<List<RecommendationDTO>> getRecommendations();

    /**
     * Share a movie recommendation with another user.
     */
    @PostMapping("/share")
    ResponseEntity<Void> shareRecommendation(@Valid @RequestBody ShareRequestDTO request);

    /**
     * Get recommendations shared with the authenticated user (inbox).
     */
    @GetMapping("/shared/received")
    ResponseEntity<List<SharedRecommendationDTO>> getReceivedRecommendations();

    /**
     * Get recommendations the authenticated user has shared with others (sent).
     */
    @GetMapping("/shared/sent")
    ResponseEntity<List<SharedRecommendationDTO>> getSentRecommendations();
}
