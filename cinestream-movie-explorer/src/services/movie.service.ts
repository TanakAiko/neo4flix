import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { environment } from '../environments/environment';

// ============================================================================
// INTERFACES - Based on Backend API Documentation
// ============================================================================

/**
 * Movie summary for list views (from /api/movies/trending, /popular, /search)
 */
export interface MovieSummary {
  tmdbId: number;
  title: string;
  overview: string;
  posterPath: string;
  voteAverage: number;
  releaseYear: number;
}

/**
 * Full movie details (from /api/movies/{tmdbId})
 */
export interface MovieDetails {
  tmdbId: number;
  title: string;
  overview: string;
  releaseDate: string;
  posterPath: string;
  voteAverage: number;
  genres: string[];
  directors: Person[];
  cast: Person[];
}

/**
 * Person (actor/director)
 */
export interface Person {
  tmdbId: number;
  name: string;
  profilePath: string;
}

/**
 * Legacy Movie interface for backward compatibility with existing components
 */
export interface Movie {
  id: string;
  tmdbId?: number;
  title: string;
  year: number;
  genre: string[];
  rating: number;
  description: string;
  director: string;
  cast: string[];
  duration: string;
  poster: string;
  backdrop: string;
  reviews: { user: string; content: string; highlight?: boolean; rating?: number }[];
}

// ============================================================================
// CONSTANTS
// ============================================================================

const API_BASE_URL = environment.apiBaseUrl;
const TMDB_IMAGE_BASE_URL = environment.tmdbImageBaseUrl;

// ============================================================================
// MOVIE SERVICE
// ============================================================================

@Injectable({
  providedIn: 'root'
})
export class MovieService {
  // -------------------------------------------------------------------------
  // Dependency Injection (Angular 2026 Standard)
  // -------------------------------------------------------------------------
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/api/movies`;

  // -------------------------------------------------------------------------
  // State Management with Signals
  // -------------------------------------------------------------------------

  /** Trending movies */
  private readonly _trendingMovies = signal<MovieSummary[]>([]);
  
  /** Popular movies */
  private readonly _popularMovies = signal<MovieSummary[]>([]);
  
  /** Search results */
  private readonly _searchResults = signal<MovieSummary[]>([]);
  
  /** Currently selected movie details */
  private readonly _selectedMovie = signal<MovieDetails | null>(null);
  
  /** Loading state */
  private readonly _isLoading = signal<boolean>(false);
  
  /** Error state */
  private readonly _error = signal<string | null>(null);

  // Public readonly signals
  readonly trendingMovies = this._trendingMovies.asReadonly();
  readonly popularMovies = this._popularMovies.asReadonly();
  readonly searchResults = this._searchResults.asReadonly();
  readonly selectedMovie = this._selectedMovie.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  // -------------------------------------------------------------------------
  // Legacy support - Mock data for existing components
  // -------------------------------------------------------------------------
  readonly movies = signal<Movie[]>([
    {
      id: 'dune-2',
      tmdbId: 693134,
      title: 'Dune: Part Two',
      year: 2024,
      genre: ['Sci-Fi', 'Adventure'],
      rating: 4.8,
      description: 'Paul Atreides unites with Chani and the Fremen while on a warpath of revenge against the conspirators who destroyed his family.',
      director: 'Denis Villeneuve',
      cast: ['Timothée Chalamet', 'Zendaya', 'Rebecca Ferguson'],
      duration: '2h 46m',
      poster: 'https://picsum.photos/seed/dune2/400/600',
      backdrop: 'https://picsum.photos/seed/dune2bg/1200/800',
      reviews: [
        { user: 'SciFi_Fan_99', content: 'An absolute masterpiece of visual storytelling.', highlight: true, rating: 5 },
        { user: 'CinemaLover', content: 'The sound design and the score are on another level.', rating: 5 },
      ]
    },
    {
      id: 'oppenheimer',
      tmdbId: 872585,
      title: 'Oppenheimer',
      year: 2023,
      genre: ['Drama', 'History'],
      rating: 4.7,
      description: 'The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb.',
      director: 'Christopher Nolan',
      cast: ['Cillian Murphy', 'Emily Blunt', 'Matt Damon'],
      duration: '3h 00m',
      poster: 'https://picsum.photos/seed/oppenheimer/400/600',
      backdrop: 'https://picsum.photos/seed/oppenheimerbg/1200/800',
      reviews: []
    },
    {
      id: 'barbie',
      tmdbId: 346698,
      title: 'Barbie',
      year: 2023,
      genre: ['Comedy', 'Fantasy'],
      rating: 4.2,
      description: 'Barbie suffers a crisis that leads her to question her world and her existence.',
      director: 'Greta Gerwig',
      cast: ['Margot Robbie', 'Ryan Gosling'],
      duration: '1h 54m',
      poster: 'https://picsum.photos/seed/barbie/400/600',
      backdrop: 'https://picsum.photos/seed/barbiebg/1200/800',
      reviews: []
    },
    {
      id: 'john-wick-3',
      tmdbId: 458156,
      title: 'John Wick: Chapter 3 – Parabellum',
      year: 2019,
      genre: ['Action', 'Thriller'],
      rating: 4.5,
      description: 'John Wick is on the run after killing a member of the international assassin\'s guild.',
      director: 'Chad Stahelski',
      cast: ['Keanu Reeves', 'Halle Berry', 'Laurence Fishburne'],
      duration: '2h 11m',
      poster: 'https://picsum.photos/seed/jw3/400/600',
      backdrop: 'https://picsum.photos/seed/jw3bg/1200/800',
      reviews: []
    },
    {
      id: 'poor-things',
      tmdbId: 792307,
      title: 'Poor Things',
      year: 2023,
      genre: ['Comedy', 'Drama', 'Sci-Fi'],
      rating: 4.3,
      description: 'The incredible tale about the fantastical evolution of Bella Baxter.',
      director: 'Yorgos Lanthimos',
      cast: ['Emma Stone', 'Mark Ruffalo'],
      duration: '2h 21m',
      poster: 'https://picsum.photos/seed/poorthings/400/600',
      backdrop: 'https://picsum.photos/seed/poorthingsbg/1200/800',
      reviews: []
    },
    {
      id: 'killers',
      tmdbId: 466420,
      title: 'Killers of the Flower Moon',
      year: 2023,
      genre: ['Crime', 'Drama', 'History'],
      rating: 4.4,
      description: 'When oil is discovered in 1920s Oklahoma under Osage Nation land, the Osage people are murdered one by one.',
      director: 'Martin Scorsese',
      cast: ['Leonardo DiCaprio', 'Robert De Niro'],
      duration: '3h 26m',
      poster: 'https://picsum.photos/seed/killers/400/600',
      backdrop: 'https://picsum.photos/seed/killersbg/1200/800',
      reviews: []
    },
    {
      id: 'spider-verse',
      tmdbId: 569094,
      title: 'Spider-Man: Across the Spider-Verse',
      year: 2023,
      genre: ['Animation', 'Action', 'Adventure'],
      rating: 4.9,
      description: 'Miles Morales catapults across the Multiverse.',
      director: 'Joaquim Dos Santos',
      cast: ['Shameik Moore', 'Hailee Steinfeld'],
      duration: '2h 20m',
      poster: 'https://picsum.photos/seed/spider/400/600',
      backdrop: 'https://picsum.photos/seed/spiderbg/1200/800',
      reviews: []
    },
    {
      id: 'blade-runner',
      tmdbId: 335984,
      title: 'Blade Runner 2049',
      year: 2017,
      genre: ['Sci-Fi', 'Thriller'],
      rating: 4.7,
      description: 'Young Blade Runner K\'s discovery of a long-buried secret leads him to track down former Blade Runner Rick Deckard.',
      director: 'Denis Villeneuve',
      cast: ['Ryan Gosling', 'Harrison Ford'],
      duration: '2h 44m',
      poster: 'https://picsum.photos/seed/br2049/400/600',
      backdrop: 'https://picsum.photos/seed/br2049bg/1200/800',
      reviews: []
    }
  ]);

  // -------------------------------------------------------------------------
  // API Methods - Backend Integration
  // -------------------------------------------------------------------------

  /**
   * Fetch trending movies from the backend
   */
  fetchTrendingMovies(): Observable<MovieSummary[]> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.get<MovieSummary[]>(`${this.apiUrl}/trending`).pipe(
      tap((movies) => this._trendingMovies.set(movies)),
      catchError((error) => {
        this._error.set('Failed to fetch trending movies');
        return of([]);
      }),
      tap(() => this._isLoading.set(false))
    );
  }

  /**
   * Fetch popular movies from the backend
   */
  fetchPopularMovies(): Observable<MovieSummary[]> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.get<MovieSummary[]>(`${this.apiUrl}/popular`).pipe(
      tap((movies) => this._popularMovies.set(movies)),
      catchError((error) => {
        this._error.set('Failed to fetch popular movies');
        return of([]);
      }),
      tap(() => this._isLoading.set(false))
    );
  }

  /**
   * Search movies by title
   */
  searchMovies(title: string): Observable<MovieSummary[]> {
    if (!title.trim()) {
      this._searchResults.set([]);
      return of([]);
    }

    this._isLoading.set(true);
    this._error.set(null);

    return this.http.get<MovieSummary[]>(`${this.apiUrl}/search`, {
      params: { title }
    }).pipe(
      tap((movies) => this._searchResults.set(movies)),
      catchError((error) => {
        this._error.set('Failed to search movies');
        return of([]);
      }),
      tap(() => this._isLoading.set(false))
    );
  }

  /**
   * Get full movie details by TMDB ID
   */
  getMovieDetails(tmdbId: number): Observable<MovieDetails> {
    this._isLoading.set(true);
    this._error.set(null);

    return this.http.get<MovieDetails>(`${this.apiUrl}/${tmdbId}`).pipe(
      tap((movie) => this._selectedMovie.set(movie)),
      catchError((error) => {
        this._error.set('Failed to fetch movie details');
        throw error;
      }),
      tap(() => this._isLoading.set(false))
    );
  }

  /**
   * Get similar movies
   */
  getSimilarMovies(tmdbId: number): Observable<MovieSummary[]> {
    return this.http.get<MovieSummary[]>(`${this.apiUrl}/${tmdbId}/similar`).pipe(
      catchError((error) => {
        console.error('Failed to fetch similar movies:', error);
        return of([]);
      })
    );
  }

  // -------------------------------------------------------------------------
  // Helper Methods
  // -------------------------------------------------------------------------

  /**
   * Get full TMDB image URL
   */
  getImageUrl(path: string | null, size: 'w200' | 'w300' | 'w500' | 'original' = 'w500'): string {
    if (!path) {
      return 'https://via.placeholder.com/500x750?text=No+Image';
    }
    return `${TMDB_IMAGE_BASE_URL}/${size}${path}`;
  }

  /**
   * Clear search results
   */
  clearSearch(): void {
    this._searchResults.set([]);
  }

  /**
   * Clear selected movie
   */
  clearSelectedMovie(): void {
    this._selectedMovie.set(null);
  }

  // -------------------------------------------------------------------------
  // Legacy Methods - For backward compatibility
  // -------------------------------------------------------------------------

  getMovieById(id: string): Movie | undefined {
    return this.movies().find(m => m.id === id);
  }

  addReview(movieId: string, review: { user: string; content: string; rating: number }): void {
    this.movies.update(movies => 
      movies.map(movie => {
        if (movie.id === movieId) {
          return {
            ...movie,
            reviews: [...movie.reviews, review]
          };
        }
        return movie;
      })
    );
  }
}