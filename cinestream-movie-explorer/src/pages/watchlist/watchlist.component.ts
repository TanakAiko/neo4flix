import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';
import { WatchlistService } from '../../services/watchlist.service';

@Component({
  selector: 'app-watchlist',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage],
  templateUrl: './watchlist.component.html',
  styleUrl: './watchlist.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WatchlistComponent {
  // -------------------------------------------------------------------------
  // Dependency Injection (Angular 2026 Standard)
  // -------------------------------------------------------------------------
  readonly watchlistService = inject(WatchlistService);
}
