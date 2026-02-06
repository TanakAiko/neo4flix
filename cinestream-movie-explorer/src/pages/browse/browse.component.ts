import { Component, inject, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MovieService } from '../../services/movie.service';

@Component({
  selector: 'app-browse',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage, FormsModule],
  templateUrl: './browse.component.html',
  styleUrl: './browse.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BrowseComponent {
  // -------------------------------------------------------------------------
  // Dependency Injection (Angular 2026 Standard)
  // -------------------------------------------------------------------------
  private readonly movieService = inject(MovieService);
  
  // -------------------------------------------------------------------------
  // State Management with Signals
  // -------------------------------------------------------------------------
  readonly movies = this.movieService.movies;
  readonly genres = ['Action', 'Comedy', 'Drama', 'Sci-Fi', 'Thriller', 'Horror', 'Romance'];
  readonly decades = ['2020s', '2010s', '2000s', '1990s', 'Classic'];

  // Filters
  readonly searchQuery = signal('');
  readonly selectedGenres = signal<string[]>([]);
  readonly selectedDecade = signal<string>('');
  readonly minRating = signal(0);
  readonly maxRating = signal(5);

  // -------------------------------------------------------------------------
  // Computed Properties
  // -------------------------------------------------------------------------
  readonly filteredMovies = computed(() => {
    const query = this.searchQuery().toLowerCase();
    const genres = this.selectedGenres();
    const decade = this.selectedDecade();
    const min = this.minRating();
    const max = this.maxRating();

    return this.movies().filter(movie => {
      // Search
      if (query && !movie.title.toLowerCase().includes(query)) {
        return false;
      }
      
      // Rating Range Check
      if (movie.rating < min || movie.rating > max) {
        return false;
      }

      // Genre (OR logic)
      if (genres.length > 0) {
        const hasGenre = movie.genre.some(g => genres.includes(g));
        if (!hasGenre) return false;
      }

      // Decade
      if (decade) {
          const year = movie.year;
          if (decade === '2020s' && (year < 2020 || year > 2029)) return false;
          if (decade === '2010s' && (year < 2010 || year > 2019)) return false;
          if (decade === '2000s' && (year < 2000 || year > 2009)) return false;
          if (decade === '1990s' && (year < 1990 || year > 1999)) return false;
          if (decade === 'Classic' && year >= 1990) return false;
      }

      return true;
    });
  });

  readonly hasActiveFilters = computed(() => {
    return this.searchQuery() !== '' || 
           this.selectedGenres().length > 0 || 
           this.selectedDecade() !== '' || 
           this.minRating() > 0 ||
           this.maxRating() < 5;
  });

  // -------------------------------------------------------------------------
  // Public Methods
  // -------------------------------------------------------------------------
  updateMin(value: number): void {
    if (value > this.maxRating()) {
      this.minRating.set(this.maxRating());
    } else {
      this.minRating.set(value);
    }
  }

  updateMax(value: number): void {
    if (value < this.minRating()) {
      this.maxRating.set(this.minRating());
    } else {
      this.maxRating.set(value);
    }
  }

  toggleGenre(genre: string): void {
    this.selectedGenres.update(current => {
       if (current.includes(genre)) {
         return current.filter(g => g !== genre);
       } else {
         return [...current, genre];
       }
    });
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.selectedGenres.set([]);
    this.selectedDecade.set('');
    this.minRating.set(0);
    this.maxRating.set(5);
  }
}
