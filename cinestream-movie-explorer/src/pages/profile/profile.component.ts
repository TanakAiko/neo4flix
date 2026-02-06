import { Component, inject, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MovieService, Movie } from '../../services/movie.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, NgOptimizedImage, RouterLink],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProfileComponent {
  // -------------------------------------------------------------------------
  // Dependency Injection (Angular 2026 Standard)
  // -------------------------------------------------------------------------
  readonly authService = inject(AuthService);
  private readonly movieService = inject(MovieService);
  
  // -------------------------------------------------------------------------
  // State Management with Signals
  // -------------------------------------------------------------------------
  readonly activeTab = signal<'reviews' | 'recommendations'>('reviews');

  // -------------------------------------------------------------------------
  // Computed Properties
  // -------------------------------------------------------------------------
  
  /** Compute User's specific reviews from the global movie list */
  readonly userReviews = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return [];

    const reviews: { movie: Movie; rating: number; content: string }[] = [];
    for (const movie of this.movieService.movies()) {
       const userReview = movie.reviews.find(r => r.user === user.username);
       if (userReview) {
          reviews.push({
             movie: movie,
             rating: userReview.rating || 0,
             content: userReview.content
          });
       }
    }
    return reviews;
  });

  /** Mock incoming recommendations based on available movies */
  readonly userRecommendations = computed(() => {
     const movies = this.movieService.movies();
     const recs = [
        { movie: movies[3], from: 'Tony Stark', message: 'You have to see the choreography in this one!' },
        { movie: movies[6], from: 'Peter Parker', message: 'The animation style is mind blowing.' },
        { movie: movies[5], from: 'Bruce Wayne', message: 'A bit dark, but fits your style.' }
     ].filter(r => r.movie !== undefined);
     return recs;
  });
}
