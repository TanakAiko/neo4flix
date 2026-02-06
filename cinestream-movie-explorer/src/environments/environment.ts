// ============================================================================
// ENVIRONMENT CONFIGURATION
// Centralized configuration for API endpoints and feature flags
// ============================================================================

export const environment = {
  production: false,
  
  // API Configuration
  apiBaseUrl: 'https://elanor-nonprofessed-venus.ngrok-free.dev',
  
  // TMDB Image Configuration
  tmdbImageBaseUrl: 'https://image.tmdb.org/t/p',
  
  // Feature Flags
  features: {
    enableMockData: true, // Use mock data when backend is unavailable
    enableDebugLogging: true,
  },
  
  // Authentication
  auth: {
    tokenRefreshBuffer: 30, // Seconds before expiry to refresh token
    storagePrefix: 'neo4flix_',
  }
} as const;

// Type for environment
export type Environment = typeof environment;
