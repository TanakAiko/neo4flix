import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgOptimizedImage, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginComponent {
  // -------------------------------------------------------------------------
  // Dependency Injection (Angular 2026 Standard)
  // -------------------------------------------------------------------------
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);

  // -------------------------------------------------------------------------
  // State Management with Signals
  // -------------------------------------------------------------------------
  readonly isLoginMode = signal(true);

  // -------------------------------------------------------------------------
  // Form Groups
  // -------------------------------------------------------------------------
  
  /** Login Form */
  readonly loginForm: FormGroup = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  /** Registration Form based on Backend DTO */
  readonly registerForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]],
    email: ['', [Validators.required, Validators.email]],
    firstname: ['', [Validators.required]],
    lastname: ['', [Validators.required]],
    password: ['', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/)
    ]]
  });

  // -------------------------------------------------------------------------
  // Actions
  // -------------------------------------------------------------------------

  toggleMode(): void {
    this.authService.clearError();
    this.isLoginMode.update(prev => !prev);
  }

  onLogin(): void {
    if (this.loginForm.valid) {
      const { username, password } = this.loginForm.value;
      this.authService.login({ username, password }).subscribe({
        next: () => {
          this.router.navigate(['/home']);
        }
      });
    }
  }

  onRegister(): void {
    if (this.registerForm.valid) {
      const { username, email, firstname, lastname, password } = this.registerForm.value;
      this.authService.register({ username, email, firstname, lastname, password }).subscribe({
        next: () => {
          // After successful registration, log the user in
          this.authService.login({ username, password }).subscribe({
            next: () => {
              this.router.navigate(['/home']);
            },
            error: () => {
              // Switch to login mode so user can login manually
              this.isLoginMode.set(true);
            }
          });
        }
      });
    }
  }
}
