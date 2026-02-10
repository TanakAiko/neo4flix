import { Component, inject, computed, signal, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { RatingService, UserRating } from '../../services/rating.service';
import { RecommendationService, SharedRecommendation } from '../../services/recommendation.service';
import { MovieService } from '../../services/movie.service';

interface RatingDisplayItem {
  tmdbId: number;
  title: string;
  posterPath: string;
  poster: string;
  rating: number;
  comment: string;
  ratedDate: string;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProfileComponent implements OnInit {
  // -------------------------------------------------------------------------
  // Dependency Injection (Angular 2026 Standard)
  // -------------------------------------------------------------------------
  readonly authService = inject(AuthService);
  private readonly ratingService = inject(RatingService);
  private readonly recommendationService = inject(RecommendationService);
  private readonly movieService = inject(MovieService);
  
  // -------------------------------------------------------------------------
  // State Management with Signals
  // -------------------------------------------------------------------------
  readonly activeTab = signal<'reviews' | 'recommendations'>('reviews');

  // -------------------------------------------------------------------------
  // Computed Properties
  // -------------------------------------------------------------------------
  
  /** User's ratings converted to display format */
  readonly userReviews = computed<RatingDisplayItem[]>(() => {
    return this.ratingService.userRatings().map(rating => this.toDisplayItem(rating));
  });

  /** Received recommendations from friends */
  readonly userRecommendations = this.recommendationService.receivedShares;

  readonly isLoading = computed(() => 
    this.ratingService.isLoading() || this.recommendationService.isLoading()
  );

  // -------------------------------------------------------------------------
  // Lifecycle Hooks
  // -------------------------------------------------------------------------
  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.ratingService.fetchUserRatings().subscribe();
      this.recommendationService.fetchReceivedShares().subscribe();
    }
  }

  // -------------------------------------------------------------------------
  // Public Methods
  // -------------------------------------------------------------------------
  getPosterUrl(posterPath: string | null): string {
    return this.movieService.getPosterUrl(posterPath);
  }

  // -------------------------------------------------------------------------
  // Private Methods
  // -------------------------------------------------------------------------
  private toDisplayItem(rating: UserRating): RatingDisplayItem {
    return {
      tmdbId: rating.tmdbId,
      title: rating.title,
      posterPath: rating.posterPath,
      poster: this.movieService.getPosterUrl(rating.posterPath),
      rating: rating.score,
      comment: rating.comment || '',
      ratedDate: rating.ratedDate
    };
  }
}
