package sn.dev.recommendation_service.web.controllers.impl;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sn.dev.recommendation_service.services.RecommendationService;
import sn.dev.recommendation_service.web.controllers.RecommendationController;
import sn.dev.recommendation_service.web.dto.RecommendationDTO;
import sn.dev.recommendation_service.web.dto.ShareRequestDTO;
import sn.dev.recommendation_service.web.dto.SharedRecommendationDTO;

@RestController
@RequiredArgsConstructor
public class RecommendationControllerImpl implements RecommendationController {

    private final RecommendationService recommendationService;

    @Override
    public ResponseEntity<List<RecommendationDTO>> getRecommendations() {
        // 1. Extract User ID from the Security Context
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Jwt jwt) {
            String userId = jwt.getSubject();

            // 2. Call Service
            List<RecommendationDTO> recommendations = recommendationService.getRecommendations(userId);

            return ResponseEntity.ok(recommendations);
        }

        // If unauthenticated, return 401 or an empty list depending on preference
        return ResponseEntity.status(401).build();
    }

    @Override
    public ResponseEntity<Void> shareRecommendation(ShareRequestDTO request) {
        recommendationService.shareRecommendation(request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<SharedRecommendationDTO>> getReceivedRecommendations() {
        List<SharedRecommendationDTO> received = recommendationService.getReceivedRecommendations();
        return ResponseEntity.ok(received);
    }

    @Override
    public ResponseEntity<List<SharedRecommendationDTO>> getSentRecommendations() {
        List<SharedRecommendationDTO> sent = recommendationService.getSentRecommendations();
        return ResponseEntity.ok(sent);
    }
}