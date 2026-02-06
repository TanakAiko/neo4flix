// ============================================================================
// SERVICES BARREL FILE
// Central export point for all application services
// ============================================================================

// Authentication
export { AuthService } from './auth.service';
export type { 
  User, 
  UserProfile, 
  PublicProfile, 
  LoginRequest, 
  RegistrationRequest, 
  TokenResponse 
} from './auth.service';

// Movies
export { MovieService } from './movie.service';
export type { 
  Movie, 
  MovieSummary, 
  MovieDetails, 
  Person 
} from './movie.service';

// Ratings
export { RatingService } from './rating.service';
export type { 
  RatingRequest, 
  UserRating 
} from './rating.service';

// Recommendations
export { RecommendationService } from './recommendation.service';
export type { 
  Recommendation, 
  ShareRequest, 
  SharedRecommendation 
} from './recommendation.service';

// Watchlist
export { WatchlistService } from './watchlist.service';

// Interceptors
export { authInterceptor } from './auth.interceptor';
