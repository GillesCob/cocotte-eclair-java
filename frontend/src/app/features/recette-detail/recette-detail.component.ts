import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { IEtape, IRecette, IRecetteIngredient, IUnite, RecetteService } from '../../core/recette.service';

const UNITE_LABELS: Record<IUnite, string> = {
  GRAMME: 'Gramme',
  KILOGRAMME: 'Kilogramme',
  MILLILITRE: 'Millilitre',
  LITRE: 'Litre',
  UNITE: 'Unité',
  CUILLERE_A_SOUPE: 'Cuillère à soupe',
  CUILLERE_A_CAFE: 'Cuillère à café',
  PINCEE: 'Pincée'
};

@Component({
  selector: 'app-recette-detail',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './recette-detail.component.html',
  styleUrl: './recette-detail.component.scss'
})
export class RecetteDetailComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly recetteService = inject(RecetteService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly uniteOptions = (Object.keys(UNITE_LABELS) as IUnite[]).map((value) => ({
    value,
    label: UNITE_LABELS[value]
  }));

  readonly recette = signal<IRecette | null>(null);
  readonly isLoading = signal(true);
  readonly isEditing = signal(false);
  readonly deleteError = signal<string | null>(null);
  readonly isDeleting = signal(false);

  readonly ingredientError = signal<string | null>(null);
  readonly isAddingIngredient = signal(false);

  readonly etapeError = signal<string | null>(null);
  readonly isAddingEtape = signal(false);

  readonly ingredientForm = this.fb.group({
    ingredientNom: ['', [Validators.required]],
    quantite: [null as number | null, [Validators.required, Validators.min(0.01)]],
    unite: ['UNITE' as IUnite, [Validators.required]]
  });

  readonly etapeForm = this.fb.group({
    ordre: [1, [Validators.required, Validators.min(1)]],
    description: ['', [Validators.required]],
    tempsCuissonMinutes: [null as number | null]
  });

  private recetteId = '';

  ngOnInit(): void {
    this.recetteId = this.route.snapshot.paramMap.get('id')!;

    this.recetteService.findById(this.recetteId).subscribe({
      next: (recette) => {
        this.recette.set(recette);
        this.isLoading.set(false);
        this.etapeForm.patchValue({ ordre: this.nextOrdre(recette.etapes) });
      },
      error: () => this.isLoading.set(false)
    });
  }

  uniteLabel(unite: IUnite): string {
    return UNITE_LABELS[unite];
  }

  addIngredient(): void {
    if (this.ingredientForm.invalid) {
      return;
    }

    this.ingredientError.set(null);
    this.isAddingIngredient.set(true);

    const { ingredientNom, quantite, unite } = this.ingredientForm.getRawValue();

    this.recetteService.addIngredient(this.recetteId, { ingredientNom: ingredientNom!, quantite: quantite!, unite: unite! }).subscribe({
      next: (ingredient) => {
        this.isAddingIngredient.set(false);
        this.appendIngredient(ingredient);
        this.ingredientForm.reset({ ingredientNom: '', quantite: null, unite: 'UNITE' });
      },
      error: (err) => {
        this.isAddingIngredient.set(false);
        this.ingredientError.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }

  removeIngredient(ingredientLineId: string): void {
    this.recetteService.removeIngredient(this.recetteId, ingredientLineId).subscribe({
      next: () => {
        const current = this.recette();
        if (!current) {
          return;
        }
        this.recette.set({
          ...current,
          ingredients: current.ingredients.filter((i) => i.id !== ingredientLineId)
        });
      },
      error: (err) => this.ingredientError.set(err.error?.message ?? 'Une erreur est survenue')
    });
  }

  addEtape(): void {
    if (this.etapeForm.invalid) {
      return;
    }

    this.etapeError.set(null);
    this.isAddingEtape.set(true);

    const { ordre, description, tempsCuissonMinutes } = this.etapeForm.getRawValue();

    this.recetteService.addEtape(this.recetteId, { ordre: ordre!, description: description!, tempsCuissonMinutes }).subscribe({
      next: (etape) => {
        this.isAddingEtape.set(false);
        this.appendEtape(etape);
        this.etapeForm.reset({
          ordre: this.nextOrdre(this.recette()?.etapes ?? []),
          description: '',
          tempsCuissonMinutes: null
        });
      },
      error: (err) => {
        this.isAddingEtape.set(false);
        this.etapeError.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }

  removeEtape(etapeId: string): void {
    this.recetteService.removeEtape(this.recetteId, etapeId).subscribe({
      next: () => {
        const current = this.recette();
        if (!current) {
          return;
        }
        this.recette.set({
          ...current,
          etapes: current.etapes.filter((e) => e.id !== etapeId)
        });
      },
      error: (err) => this.etapeError.set(err.error?.message ?? 'Une erreur est survenue')
    });
  }

  deleteRecette(): void {
    const current = this.recette();
    if (!current) {
      return;
    }

    if (!confirm(`Supprimer définitivement la recette "${current.titre}" ? Cette action est irréversible.`)) {
      return;
    }

    this.deleteError.set(null);
    this.isDeleting.set(true);

    this.recetteService.delete(this.recetteId).subscribe({
      next: () => {
        this.router.navigate(['/recettes']);
      },
      error: (err) => {
        this.isDeleting.set(false);
        this.deleteError.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }

  private appendIngredient(ingredient: IRecetteIngredient): void {
    const current = this.recette();
    if (!current) {
      return;
    }
    this.recette.set({ ...current, ingredients: [...current.ingredients, ingredient] });
  }

  private appendEtape(etape: IEtape): void {
    const current = this.recette();
    if (!current) {
      return;
    }
    const etapes = [...current.etapes, etape].sort((a, b) => a.ordre - b.ordre);
    this.recette.set({ ...current, etapes });
  }

  private nextOrdre(etapes: IEtape[]): number {
    return etapes.length === 0 ? 1 : Math.max(...etapes.map((e) => e.ordre)) + 1;
  }
}
