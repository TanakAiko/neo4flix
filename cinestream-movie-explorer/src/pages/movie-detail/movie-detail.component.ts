import { Component, inject, computed, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { ActivatedRoute, RouterLink, ParamMap } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MovieService } from '../../services/movie.service';
import { WatchlistService } from '../../services/watchlist.service';
import { AuthService } from '../../services/auth.service';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-movie-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage, FormsModule],
  templateUrl: './movie-detail.component.html',
  styleUrl: './movie-detail.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MovieDetailComponent {
  // -------------------------------------------------------------------------
  // Dependency Injection (Angular 2026 Standard)
  // -------------------------------------------------------------------------
  private readonly route = inject(ActivatedRoute);
  private readonly movieService = inject(MovieService);
  private readonly authService = inject(AuthService);
  readonly watchlistService = inject(WatchlistService);
  
  // -------------------------------------------------------------------------
  // Route Parameters
  // -------------------------------------------------------------------------
  private readonly idParam = toSignal(
    this.route.paramMap.pipe(map((params: ParamMap) => params.get('id')))
  );

  // -------------------------------------------------------------------------
  // State Management with Signals
  // -------------------------------------------------------------------------
  readonly currentRating = signal(0);
  readonly reviewComment = signal('');
  readonly showRecommendModal = signal(false);
  readonly recommendationMessage = signal('');

  // Mock Friends List
  readonly friendsList = ['Sarah Connor', 'Tony Stark', 'Bruce Wayne'];

  // -------------------------------------------------------------------------
  // Computed Properties
  // -------------------------------------------------------------------------
  readonly movie = computed(() => {
    const id = this.idParam();
    return (id && typeof id === 'string') ? this.movieService.getMovieById(id) : undefined;
  });

  readonly otherMovies = computed(() => {
    const currentId = this.movie()?.id;
    return this.movieService.movies().filter(m => m.id !== currentId).slice(0, 4);
  });

  // -------------------------------------------------------------------------
  // Public Methods
  // -------------------------------------------------------------------------
  
  /** Helper for generating consistent images */
  getPersonImage(name: string): string {
    return `https://picsum.photos/seed/${name.replace(/[^a-zA-Z]/g, '')}/200/200`;
  }

  setRating(star: number): void {
    this.currentRating.set(star);
  }

  submitReview(movieId: string): void {
    if (this.currentRating() === 0 || !this.reviewComment()) return;

    const user = this.authService.currentUser()?.username || 'Guest';
    const newReview = {
      user: user,
      content: this.reviewComment(),
      rating: this.currentRating(),
      highlight: false
    };

    this.movieService.addReview(movieId, newReview);
    
    // Reset form
    this.currentRating.set(0);
    this.reviewComment.set('');
  }

  openRecommendModal(): void {
    this.recommendationMessage.set('');
    this.showRecommendModal.set(true);
  }

  closeRecommendModal(): void {
    this.showRecommendModal.set(false);
  }

  sendRecommendationTo(friend: string): void {
    console.log(`Sending recommendation to ${friend} with message: ${this.recommendationMessage()}`);
    this.closeRecommendModal();
  }

  getCurrentUrl(): string {
    return window.location.href;
  }
}
