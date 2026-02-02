package sn.dev.recommendation_service.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sharing a movie recommendation with another user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareRequestDTO {
    
    @NotNull(message = "tmdbId is required")
    private Integer tmdbId;
    
    @NotNull(message = "recipientUsername is required")
    private String recipientUsername;
    
    private String message; // Optional personal message
}
