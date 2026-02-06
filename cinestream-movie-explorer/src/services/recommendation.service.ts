import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, catchError, finalize } from 'rxjs/operators';
import { environment } from '../environments/environment';

// ============================================================================
// INTERFACES - Based on Backend API Documentation
// ============================================================================

/**
 * Recommendation from the recommendation engine
 */
export interface Recommendation {
  tmdbId: number;
  title: string;
  posterPath: string;
  overview: string;
  voteAverage: number;
  releaseYear: number;
  reason: string; // e.g., "Popular among users who liked Inception"
}

/**
 * Share recommendation request
 */
export interface ShareRequest {
  tmdbId: number;
  recipientUsername: string;
  message?: string;
}

/**
 * Shared recommendation (received or sent)
 */
export interface SharedRecommendation {
  // Movie info
  tmdbId: number;
  title: string;
  posterPath: string;
  overview: string;
  voteAverage: number;
  releaseYear: number;
  // Share info
  fromUsername: string;
  message: string;
  sharedAt: string;
}

// ============================================================================
// CONSTANTS
// ============================================================================

const API_BASE_URL = environment.apiBaseUrl;

// ============================================================================
// RECOMMENDATION SERVICE
// ============================================================================

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {
  // -------------------------------------------------------------------------
  // Dependency Injection (Angular 2026 Standard)
  // -------------------------------------------------------------------------
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/api/recommendations`;

  // -------------------------------------------------------------------------
  // State Management with Signals
  // -------------------------------------------------------------------------

  /** Personalized recommendations */
  private readonly _recommendations = signal<Recommendation[]>([]);
  
  /** Recommendations received from other users */
  private readonly _receivedShares = signal<SharedRecommendation[]>([]);
  
  /** Recommendations sent to other users */
  private readonly _sentShares = signal<SharedRecommendation[]>([]);
  
  /** Loading state */
  private readonly _isLoading = signal<boolean>(false);
  
  /** Error state */
  private readonly _error = signal<string | null>(null);

  // Public readonly signals
  readonly recommendations = this._recommendations.asReadonly();
  readonly receivedShares = this._receivedShares.asReadonly();
  readonly sentShares = this._sentShares.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  // -------------------------------------------------------------------------
  // API Methods
  // -------------------------------------------------------------------------

  /**
   * Fetch personalized recommendations based on user's ratings and behavior
   */
  fetchRecommendations(): Observable<Recommendation[]> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.get<Recommendation[]>(this.apiUrl).pipe(
      tap((recommendations) => this._recommendations.set(recommendations)),
      catchError((error) => {
        this._error.set('Failed to fetch recommendations');
        console.error('Recommendations error:', error);
        return of([]);
      }),
      finalize(() => this._isLoading.set(false))
    );
  }

  /**
   * Share a movie recommendation with another user
   */
  shareRecommendation(tmdbId: number, recipientUsername: string, message?: string): Observable<void> {
    this._isLoading.set(true);
    this._error.set(null);

    const request: ShareRequest = { tmdbId, recipientUsername, message };

    return this.http.post<void>(`${this.apiUrl}/share`, request).pipe(
      tap(() => {
        // Refresh sent shares after successful share
        this.fetchSentShares().subscribe();
      }),
      catchError((error) => {
        this._error.set('Failed to share recommendation');
        throw error;
      }),
      finalize(() => this._isLoading.set(false))
    );
  }

  /**
   * Fetch recommendations received from other users
   */
  fetchReceivedShares(): Observable<SharedRecommendation[]> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.get<SharedRecommendation[]>(`${this.apiUrl}/shared/received`).pipe(
      tap((shares) => this._receivedShares.set(shares)),
      catchError((error) => {
        this._error.set('Failed to fetch received recommendations');
        return of([]);
      }),
      finalize(() => this._isLoading.set(false))
    );
  }

  /**
   * Fetch recommendations sent to other users
   */
  fetchSentShares(): Observable<SharedRecommendation[]> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.get<SharedRecommendation[]>(`${this.apiUrl}/shared/sent`).pipe(
      tap((shares) => this._sentShares.set(shares)),
      catchError((error) => {
        this._error.set('Failed to fetch sent recommendations');
        return of([]);
      }),
      finalize(() => this._isLoading.set(false))
    );
  }

  // -------------------------------------------------------------------------
  // Helper Methods
  // -------------------------------------------------------------------------

  /**
   * Clear all recommendation state
   */
  clearRecommendations(): void {
    this._recommendations.set([]);
    this._receivedShares.set([]);
    this._sentShares.set([]);
  }

  /**
   * Clear error state
   */
  clearError(): void {
    this._error.set(null);
  }

  /**
   * Get count of unread received recommendations
   * Note: This is a placeholder - backend doesn't track read status yet
   */
  get unreadCount(): number {
    return this._receivedShares().length;
  }
}
