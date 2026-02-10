import { Component, inject, signal, computed, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MovieService, MovieSummary } from '../../services/movie.service';
import { AuthService } from '../../services/auth.service';
import { WatchlistService } from '../../services/watchlist.service';

/**
 * Display model for movies in the home page
 */
interface MovieDisplayItem {
  tmdbId: number;
  title: string;
  year: number;
  rating: number;
  description: string;
  genre: string;
  poster: string;
  backdrop: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeComponent implements OnInit {
  // -------------------------------------------------------------------------
  // Dependency Injection (Angular 2026 Standard)
  // -------------------------------------------------------------------------
  readonly movieService = inject(MovieService);
  readonly authService = inject(AuthService);
  readonly watchlistService = inject(WatchlistService);
  
  // -------------------------------------------------------------------------
  // State Management with Signals
  // -------------------------------------------------------------------------
  
  /** Currently displayed hero movie */
  readonly heroMovie = signal<MovieDisplayItem | null>(null);
  
  /** Loading state */
  readonly isLoading = this.movieService.isLoading;

  // -------------------------------------------------------------------------
  // Computed Properties
  // -------------------------------------------------------------------------
  
  /** Trending movies converted to display format */
  readonly trendingMovies = computed(() => 
    this.movieService.trendingMovies().map(m => this.toDisplayItem(m))
  );
  
  /** Popular movies converted to display format */
  readonly popularMovies = computed(() => 
    this.movieService.popularMovies().map(m => this.toDisplayItem(m))
  );
  
  /** Top rated movies (sorted by rating) */
  readonly topRatedMovies = computed(() => {
    const all = [...this.movieService.trendingMovies(), ...this.movieService.popularMovies()];
    // Deduplicate
    const unique = all.filter((movie, index, self) => 
      index === self.findIndex(m => m.tmdbId === movie.tmdbId)
    );
    // Sort by vote average and take top 10
    return unique
      .sort((a, b) => b.voteAverage - a.voteAverage)
      .slice(0, 10)
      .map(m => this.toDisplayItem(m));
  });

  // -------------------------------------------------------------------------
  // Lifecycle Hooks
  // -------------------------------------------------------------------------
  ngOnInit(): void {
    // Fetch movies from API
    this.movieService.fetchAllMovies().subscribe({
      next: ([trending]) => {
        // Set hero movie to first trending movie
        if (trending.length > 0) {
          this.heroMovie.set(this.toDisplayItem(trending[0]));
        }
      }
    });
    
    // Fetch watchlist if user is logged in
    if (this.authService.isLoggedIn()) {
      this.watchlistService.fetchWatchlist().subscribe();
    }
  }

  // -------------------------------------------------------------------------
  // Public Methods
  // -------------------------------------------------------------------------
  
  /** Update Hero section on hover */
  setHeroMovie(movie: MovieDisplayItem): void {
    this.heroMovie.set(movie);
  }

  /** Toggle watchlist for a movie */
  toggleWatchlist(movie: MovieDisplayItem): void {
    this.watchlistService.toggleWatchlist(movie.tmdbId).subscribe();
  }

  /** Check if movie is in watchlist */
  isInWatchlist(tmdbId: number): boolean {
    return this.watchlistService.isInWatchlist(tmdbId);
  }

  scrollLeft(element: HTMLElement): void {
    element.scrollBy({ left: -300, behavior: 'smooth' });
  }

  scrollRight(element: HTMLElement): void {
    element.scrollBy({ left: 300, behavior: 'smooth' });
  }

  // -------------------------------------------------------------------------
  // Private Methods
  // -------------------------------------------------------------------------
  
  /** Convert MovieSummary to display format */
  private toDisplayItem(movie: MovieSummary): MovieDisplayItem {
    return {
      tmdbId: movie.tmdbId,
      title: movie.title,
      year: movie.releaseYear,
      rating: Math.round(movie.voteAverage * 10) / 10 / 2, // Convert 10-scale to 5-scale
      description: movie.overview,
      genre: '', // Will be filled when we have genres in summary
      poster: this.movieService.getPosterUrl(movie.posterPath),
      backdrop: this.movieService.getBackdropUrl(movie.backdropPath),
    };
  }
}
