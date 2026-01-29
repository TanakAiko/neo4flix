package sn.dev.recommendation_service.web.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import sn.dev.recommendation_service.web.dto.RecommendationDTO;

import java.util.List;

@RequestMapping("/api/recommendations")
public interface RecommendationController {

    @GetMapping
    ResponseEntity<List<RecommendationDTO>> getRecommendations();
}
