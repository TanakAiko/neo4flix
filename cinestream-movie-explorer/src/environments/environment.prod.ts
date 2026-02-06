// ============================================================================
// PRODUCTION ENVIRONMENT CONFIGURATION
// ============================================================================

export const environment = {
  production: true,
  
  // API Configuration - Update with production URL
  apiBaseUrl: 'https://api.neo4flix.com',
  
  // TMDB Image Configuration
  tmdbImageBaseUrl: 'https://image.tmdb.org/t/p',
  
  // Feature Flags
  features: {
    enableMockData: false,
    enableDebugLogging: false,
  },
  
  // Authentication
  auth: {
    tokenRefreshBuffer: 30,
    storagePrefix: 'neo4flix_',
  }
} as const;

export type Environment = typeof environment;
