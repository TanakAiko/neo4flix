import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, catchError, finalize } from 'rxjs/operators';
import { environment } from '../environments/environment';

// ============================================================================
// INTERFACES - Based on Backend API Documentation
// ============================================================================

/**
 * Rating request payload
 */
export interface RatingRequest {
  tmdbId: number;
  score: number; // 1-5
}

/**
 * User rating response
 */
export interface UserRating {
  tmdbId: number;
  title: string;
  posterPath: string;
  score: number;
  ratedDate: string;
}

// ============================================================================
// CONSTANTS
// ============================================================================

const API_BASE_URL = environment.apiBaseUrl;

// ============================================================================
// RATING SERVICE
// ============================================================================

@Injectable({
  providedIn: 'root'
})
export class RatingService {
  // -------------------------------------------------------------------------
  // Dependency Injection (Angular 2026 Standard)
  // -------------------------------------------------------------------------
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/api/ratings`;

  // -------------------------------------------------------------------------
  // State Management with Signals
  // -------------------------------------------------------------------------

  /** User's ratings */
  private readonly _userRatings = signal<UserRating[]>([]);
  
  /** Loading state */
  private readonly _isLoading = signal<boolean>(false);
  
  /** Error state */
  private readonly _error = signal<string | null>(null);

  // Public readonly signals
  readonly userRatings = this._userRatings.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  // -------------------------------------------------------------------------
  // API Methods
  // -------------------------------------------------------------------------

  /**
   * Submit or update a movie rating (1-5 stars)
   */
  rateMovie(tmdbId: number, score: number): Observable<void> {
    if (score < 1 || score > 5) {
      this._error.set('Rating must be between 1 and 5');
      return of(undefined);
    }

    this._isLoading.set(true);
    this._error.set(null);

    const request: RatingRequest = { tmdbId, score };

    return this.http.post<void>(this.apiUrl, request).pipe(
      tap(() => {
        // Update local state optimistically
        this._userRatings.update(ratings => {
          const existingIndex = ratings.findIndex(r => r.tmdbId === tmdbId);
          if (existingIndex >= 0) {
            const updated = [...ratings];
            updated[existingIndex] = { ...updated[existingIndex], score };
            return updated;
          }
          return ratings;
        });
      }),
      catchError((error) => {
        this._error.set('Failed to submit rating');
        throw error;
      }),
      finalize(() => this._isLoading.set(false))
    );
  }

  /**
   * Delete a rating
   */
  deleteRating(tmdbId: number): Observable<void> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.delete<void>(`${this.apiUrl}/${tmdbId}`).pipe(
      tap(() => {
        // Remove from local state
        this._userRatings.update(ratings => 
          ratings.filter(r => r.tmdbId !== tmdbId)
        );
      }),
      catchError((error) => {
        this._error.set('Failed to delete rating');
        throw error;
      }),
      finalize(() => this._isLoading.set(false))
    );
  }

  /**
   * Fetch all user's ratings
   */
  fetchUserRatings(): Observable<UserRating[]> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.get<UserRating[]>(this.apiUrl).pipe(
      tap((ratings) => this._userRatings.set(ratings)),
      catchError((error) => {
        this._error.set('Failed to fetch ratings');
        return of([]);
      }),
      finalize(() => this._isLoading.set(false))
    );
  }

  /**
   * Get user's rating for a specific movie
   */
  getUserRatingForMovie(tmdbId: number): Observable<number | null> {
    return this.http.get<number>(`${this.apiUrl}/movie/${tmdbId}`).pipe(
      catchError(() => of(null))
    );
  }

  /**
   * Get average rating for a movie (public endpoint)
   */
  getAverageRating(tmdbId: number): Observable<number | null> {
    return this.http.get<number>(`${this.apiUrl}/movie/${tmdbId}/average`).pipe(
      catchError(() => of(null))
    );
  }

  // -------------------------------------------------------------------------
  // Helper Methods
  // -------------------------------------------------------------------------

  /**
   * Check if user has rated a movie (from local state)
   */
  hasRated(tmdbId: number): boolean {
    return this._userRatings().some(r => r.tmdbId === tmdbId);
  }

  /**
   * Get rating for a movie from local state
   */
  getRating(tmdbId: number): number | undefined {
    return this._userRatings().find(r => r.tmdbId === tmdbId)?.score;
  }

  /**
   * Clear error state
   */
  clearError(): void {
    this._error.set(null);
  }
}
