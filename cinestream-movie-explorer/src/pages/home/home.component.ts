import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MovieService, Movie } from '../../services/movie.service';
import { AuthService } from '../../services/auth.service';
import { WatchlistService } from '../../services/watchlist.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage],
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
  readonly movies = this.movieService.movies;
  readonly heroMovie = signal<Movie | undefined>(undefined);

  // -------------------------------------------------------------------------
  // Lifecycle Hooks
  // -------------------------------------------------------------------------
  ngOnInit(): void {
    // Initialize hero section with Dune 2 or fallback to first
    const initialMovie = this.movies().find(m => m.id === 'dune-2') || this.movies()[0];
    this.heroMovie.set(initialMovie);
  }

  // -------------------------------------------------------------------------
  // Public Methods
  // -------------------------------------------------------------------------
  
  /** Update Hero section on hover */
  setHeroMovie(movie: Movie): void {
    this.heroMovie.set(movie);
  }

  /** Computed top rated movies */
  bestRatedMovies(): Movie[] {
    return [...this.movies()].sort((a, b) => b.rating - a.rating).slice(0, 10);
  }

  scrollLeft(element: HTMLElement): void {
    element.scrollBy({ left: -300, behavior: 'smooth' });
  }

  scrollRight(element: HTMLElement): void {
    element.scrollBy({ left: 300, behavior: 'smooth' });
  }
}
