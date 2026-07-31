import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { RecetteService } from '../../core/recette.service';

@Component({
  selector: 'app-nouvelle-recette',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './nouvelle-recette.component.html',
  styleUrl: './nouvelle-recette.component.scss'
})
export class NouvelleRecetteComponent {
  private readonly fb = inject(FormBuilder);
  private readonly recetteService = inject(RecetteService);
  private readonly router = inject(Router);

  readonly errorMessage = signal<string | null>(null);
  readonly isSubmitting = signal(false);

  readonly form = this.fb.group({
    titre: ['', [Validators.required]],
    description: [''],
    visibilite: ['PRIVEE' as 'PRIVEE' | 'PUBLIQUE', [Validators.required]]
  });

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }

    this.errorMessage.set(null);
    this.isSubmitting.set(true);

    const { titre, description, visibilite } = this.form.getRawValue();

    this.recetteService.create({ titre: titre!, description: description || null, visibilite: visibilite! }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.router.navigate(['/recettes']);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }
}
