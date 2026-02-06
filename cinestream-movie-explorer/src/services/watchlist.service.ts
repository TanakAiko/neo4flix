import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, catchError, finalize } from 'rxjs/operators';
import { Movie, MovieSummary } from './movie.service';
import { environment } from '../environments/environment';

// ============================================================================
// CONSTANTS
// ============================================================================

const API_BASE_URL = environment.apiBaseUrl;

// ============================================================================
// WATCHLIST SERVICE
// ============================================================================

@Injectable({
  providedIn: 'root'
})
export class WatchlistService {
  // -------------------------------------------------------------------------
  // Dependency Injection (Angular 2026 Standard)
  // -------------------------------------------------------------------------
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/api/movies`;

  // -------------------------------------------------------------------------
  // State Management with Signals
  // -------------------------------------------------------------------------

  /** User's watchlist (from API) */
  private readonly _watchlistMovies = signal<MovieSummary[]>([]);
  
  /** Legacy watchlist for backward compatibility */
  private readonly _legacyWatchlist = signal<Movie[]>([]);
  
  /** Loading state */
  private readonly _isLoading = signal<boolean>(false);
  
  /** Error state */
  private readonly _error = signal<string | null>(null);

  // Public readonly signals
  readonly watchlistMovies = this._watchlistMovies.asReadonly();
  readonly watchlist = this._legacyWatchlist.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  // -------------------------------------------------------------------------
  // API Methods - Backend Integration
  // -------------------------------------------------------------------------

  /**
   * Fetch user's watchlist from the backend
   */
  fetchWatchlist(): Observable<MovieSummary[]> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.get<MovieSummary[]>(`${this.apiUrl}/watchlist`).pipe(
      tap((movies) => this._watchlistMovies.set(movies)),
      catchError((error) => {
        this._error.set('Failed to fetch watchlist');
        console.error('Watchlist fetch error:', error);
        return of([]);
      }),
      finalize(() => this._isLoading.set(false))
    );
  }

  /**
   * Add a movie to watchlist (by TMDB ID)
   */
  addToWatchlistByTmdbId(tmdbId: number): Observable<void> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.post<void>(`${this.apiUrl}/${tmdbId}/watchlist`, {}).pipe(
      tap(() => {
        // Refresh watchlist after adding
        this.fetchWatchlist().subscribe();
      }),
      catchError((error) => {
        this._error.set('Failed to add to watchlist');
        throw error;
      }),
      finalize(() => this._isLoading.set(false))
    );
  }

  /**
   * Remove a movie from watchlist (by TMDB ID)
   */
  removeFromWatchlistByTmdbId(tmdbId: number): Observable<void> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.delete<void>(`${this.apiUrl}/${tmdbId}/watchlist`).pipe(
      tap(() => {
        // Update local state
        this._watchlistMovies.update(movies => 
          movies.filter(m => m.tmdbId !== tmdbId)
        );
      }),
      catchError((error) => {
        this._error.set('Failed to remove from watchlist');
        throw error;
      }),
      finalize(() => this._isLoading.set(false))
    );
  }

  /**
   * Check if a movie is in the watchlist (by TMDB ID)
   */
  isInWatchlistByTmdbId(tmdbId: number): boolean {
    return this._watchlistMovies().some(m => m.tmdbId === tmdbId);
  }

  /**
   * Toggle watchlist status (by TMDB ID)
   */
  toggleWatchlistByTmdbId(tmdbId: number): Observable<void> {
    if (this.isInWatchlistByTmdbId(tmdbId)) {
      return this.removeFromWatchlistByTmdbId(tmdbId);
    } else {
      return this.addToWatchlistByTmdbId(tmdbId);
    }
  }

  // -------------------------------------------------------------------------
  // Legacy Methods - For backward compatibility
  // -------------------------------------------------------------------------

  addToWatchlist(movie: Movie): void {
    this._legacyWatchlist.update(list => {
      if (list.some(m => m.id === movie.id)) return list;
      return [...list, movie];
    });

    // Also add to backend if movie has tmdbId
    if (movie.tmdbId) {
      this.addToWatchlistByTmdbId(movie.tmdbId).subscribe();
    }
  }

  removeFromWatchlist(movieId: string): void {
    const movie = this._legacyWatchlist().find(m => m.id === movieId);
    this._legacyWatchlist.update(list => list.filter(m => m.id !== movieId));

    // Also remove from backend if movie has tmdbId
    if (movie?.tmdbId) {
      this.removeFromWatchlistByTmdbId(movie.tmdbId).subscribe();
    }
  }

  isInWatchlist(movieId: string): boolean {
    return this._legacyWatchlist().some(m => m.id === movieId);
  }

  toggleWatchlist(movie: Movie): void {
    if (this.isInWatchlist(movie.id)) {
      this.removeFromWatchlist(movie.id);
    } else {
      this.addToWatchlist(movie);
    }
  }

  // -------------------------------------------------------------------------
  // Helper Methods
  // -------------------------------------------------------------------------

  /**
   * Clear error state
   */
  clearError(): void {
    this._error.set(null);
  }

  /**
   * Get watchlist count
   */
  get count(): number {
    return this._watchlistMovies().length + this._legacyWatchlist().length;
  }
}