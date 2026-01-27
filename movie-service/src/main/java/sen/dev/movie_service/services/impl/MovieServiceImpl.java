package sen.dev.movie_service.services.impl;

import java.util.List;

import sen.dev.movie_service.services.MovieService;
import sen.dev.movie_service.web.dto.MovieDetailsDTO;
import sen.dev.movie_service.web.dto.MovieSummaryDTO;

public class MovieServiceImpl implements MovieService {

    @Override
    public List<MovieSummaryDTO> getTrendingMovies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MovieSummaryDTO> getPopularMovies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MovieSummaryDTO> searchMovies(String title) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MovieDetailsDTO getMovieByTmdbId(Integer tmdbId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addToWatchlist(String userId, Integer tmdbId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeFromWatchlist(String userId, Integer tmdbId) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<MovieSummaryDTO> getWatchlist(String userId) {
        // TODO Auto-generated method stub
        return null;
    }

}
