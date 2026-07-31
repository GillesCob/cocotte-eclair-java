import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  readonly token = this.route.snapshot.queryParamMap.get('token');

  readonly errorMessage = signal<string | null>(null);
  readonly isSubmitting = signal(false);
  readonly isDone = signal(false);

  readonly form = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]]
  });

  onSubmit(): void {
    if (this.form.invalid || !this.token) {
      return;
    }

    this.errorMessage.set(null);
    this.isSubmitting.set(true);

    const { newPassword } = this.form.getRawValue();

    this.authService.resetPassword(this.token, newPassword!).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.isDone.set(true);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }
}
