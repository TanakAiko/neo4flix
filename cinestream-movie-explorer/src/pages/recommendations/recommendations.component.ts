import { Component, inject, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MovieService, Movie } from '../../services/movie.service';

interface RecommendedMovie {
  movie: Movie;
  reason: string;
}

@Component({
  selector: 'app-recommendations',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage],
  templateUrl: './recommendations.component.html',
  styleUrl: './recommendations.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecommendationsComponent {
  // -------------------------------------------------------------------------
  // Dependency Injection (Angular 2026 Standard)
  // -------------------------------------------------------------------------
  private readonly movieService = inject(MovieService);
  
  // -------------------------------------------------------------------------
  // Computed Properties
  // -------------------------------------------------------------------------
  readonly recommendations = computed<RecommendedMovie[]>(() => {
    const movies = this.movieService.movies();
    // Simulate recommendation reasons mapping
    const reasons = [
      "Because you like deep Sci-Fi worlds",
      "Matches your taste for high-stakes drama",
      "Highly rated by users like you",
      "Directed by a visionary you follow",
      "Trending in your region",
      "Similar to 'Inception' which you loved",
      "Critically acclaimed visual masterpiece",
      "Featuring actors from your favorite list"
    ];

    return movies.map((movie, index) => ({
      movie,
      reason: reasons[index % reasons.length]
    }));
  });
}
