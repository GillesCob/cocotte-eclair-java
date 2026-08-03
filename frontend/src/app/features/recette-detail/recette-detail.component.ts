import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
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

const UNITE_ABBR: Record<IUnite, string> = {
  GRAMME: 'g',
  KILOGRAMME: 'kg',
  MILLILITRE: 'mL',
  LITRE: 'L',
  UNITE: '',
  CUILLERE_A_SOUPE: 'c. à s.',
  CUILLERE_A_CAFE: 'c. à c.',
  PINCEE: 'pincée'
};

type IModalType =
  | 'titre'
  | 'description'
  | 'visibilite'
  | 'add-ingredient'
  | 'edit-ingredient'
  | 'add-etape'
  | 'edit-etape'
  | 'delete-ingredient'
  | 'delete-etape'
  | 'delete-recette'
  | null;

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

  readonly activeModal = signal<IModalType>(null);
  readonly modalError = signal<string | null>(null);
  readonly isSaving = signal(false);

  readonly targetIngredient = signal<IRecetteIngredient | null>(null);
  readonly targetEtape = signal<IEtape | null>(null);

  readonly titreForm = this.fb.group({ titre: ['', [Validators.required]] });
  readonly descriptionForm = this.fb.group({ description: [''] });
  readonly visibiliteForm = this.fb.group({ visibilite: ['PRIVEE' as 'PRIVEE' | 'PUBLIQUE', [Validators.required]] });

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
      },
      error: () => this.isLoading.set(false)
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.activeModal()) {
      this.closeModal();
    }
  }

  uniteLabel(unite: IUnite): string {
    return UNITE_LABELS[unite];
  }

  uniteAbbr(unite: IUnite): string {
    return UNITE_ABBR[unite];
  }

  closeModal(): void {
    this.activeModal.set(null);
    this.modalError.set(null);
    this.isSaving.set(false);
    this.targetIngredient.set(null);
    this.targetEtape.set(null);
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeModal();
    }
  }

  private currentRequest() {
    const current = this.recette();
    return {
      titre: current!.titre,
      description: current!.description,
      visibilite: current!.visibilite
    };
  }

  openTitreModal(): void {
    this.titreForm.setValue({ titre: this.recette()!.titre });
    this.modalError.set(null);
    this.activeModal.set('titre');
  }

  saveTitre(): void {
    if (this.titreForm.invalid) {
      return;
    }
    this.isSaving.set(true);
    this.modalError.set(null);
    const request = { ...this.currentRequest(), titre: this.titreForm.getRawValue().titre! };
    this.recetteService.update(this.recetteId, request).subscribe({
      next: (recette) => {
        this.isSaving.set(false);
        this.recette.set(recette);
        this.closeModal();
      },
      error: (err) => {
        this.isSaving.set(false);
        this.modalError.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }

  openDescriptionModal(): void {
    this.descriptionForm.setValue({ description: this.recette()!.description ?? '' });
    this.modalError.set(null);
    this.activeModal.set('description');
  }

  saveDescription(): void {
    this.isSaving.set(true);
    this.modalError.set(null);
    const description = this.descriptionForm.getRawValue().description || null;
    const request = { ...this.currentRequest(), description };
    this.recetteService.update(this.recetteId, request).subscribe({
      next: (recette) => {
        this.isSaving.set(false);
        this.recette.set(recette);
        this.closeModal();
      },
      error: (err) => {
        this.isSaving.set(false);
        this.modalError.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }

  openVisibiliteModal(): void {
    this.visibiliteForm.setValue({ visibilite: this.recette()!.visibilite });
    this.modalError.set(null);
    this.activeModal.set('visibilite');
  }

  saveVisibilite(): void {
    this.isSaving.set(true);
    this.modalError.set(null);
    const request = { ...this.currentRequest(), visibilite: this.visibiliteForm.getRawValue().visibilite! };
    this.recetteService.update(this.recetteId, request).subscribe({
      next: (recette) => {
        this.isSaving.set(false);
        this.recette.set(recette);
        this.closeModal();
      },
      error: (err) => {
        this.isSaving.set(false);
        this.modalError.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }

  openAddIngredientModal(): void {
    this.ingredientForm.reset({ ingredientNom: '', quantite: null, unite: 'UNITE' });
    this.modalError.set(null);
    this.activeModal.set('add-ingredient');
  }

  openEditIngredientModal(ingredient: IRecetteIngredient): void {
    this.ingredientForm.setValue({
      ingredientNom: ingredient.ingredientNom,
      quantite: ingredient.quantite,
      unite: ingredient.unite
    });
    this.targetIngredient.set(ingredient);
    this.modalError.set(null);
    this.activeModal.set('edit-ingredient');
  }

  submitIngredientModal(): void {
    if (this.ingredientForm.invalid) {
      return;
    }
    this.isSaving.set(true);
    this.modalError.set(null);
    const { ingredientNom, quantite, unite } = this.ingredientForm.getRawValue();
    const request = { ingredientNom: ingredientNom!, quantite: quantite!, unite: unite! };
    const target = this.targetIngredient();

    const request$ =
      this.activeModal() === 'edit-ingredient' && target
        ? this.recetteService.updateIngredient(this.recetteId, target.id, request)
        : this.recetteService.addIngredient(this.recetteId, request);

    request$.subscribe({
      next: (ingredient) => {
        this.isSaving.set(false);
        const current = this.recette();
        if (!current) {
          return;
        }
        const ingredients = target
          ? current.ingredients.map((i) => (i.id === ingredient.id ? ingredient : i))
          : [...current.ingredients, ingredient];
        this.recette.set({ ...current, ingredients });
        this.closeModal();
      },
      error: (err) => {
        this.isSaving.set(false);
        this.modalError.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }

  openDeleteIngredientModal(ingredient: IRecetteIngredient): void {
    this.targetIngredient.set(ingredient);
    this.modalError.set(null);
    this.activeModal.set('delete-ingredient');
  }

  confirmDeleteIngredient(): void {
    const target = this.targetIngredient();
    if (!target) {
      return;
    }
    this.isSaving.set(true);
    this.modalError.set(null);
    this.recetteService.removeIngredient(this.recetteId, target.id).subscribe({
      next: () => {
        this.isSaving.set(false);
        const current = this.recette();
        if (!current) {
          return;
        }
        this.recette.set({ ...current, ingredients: current.ingredients.filter((i) => i.id !== target.id) });
        this.closeModal();
      },
      error: (err) => {
        this.isSaving.set(false);
        this.modalError.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }

  private nextOrdre(): number {
    const etapes = this.recette()?.etapes ?? [];
    return etapes.length === 0 ? 1 : Math.max(...etapes.map((e) => e.ordre)) + 1;
  }

  openAddEtapeModal(): void {
    this.etapeForm.reset({ ordre: this.nextOrdre(), description: '', tempsCuissonMinutes: null });
    this.modalError.set(null);
    this.activeModal.set('add-etape');
  }

  openEditEtapeModal(etape: IEtape): void {
    this.etapeForm.setValue({
      ordre: etape.ordre,
      description: etape.description,
      tempsCuissonMinutes: etape.tempsCuissonMinutes
    });
    this.targetEtape.set(etape);
    this.modalError.set(null);
    this.activeModal.set('edit-etape');
  }

  submitEtapeModal(): void {
    if (this.etapeForm.invalid) {
      return;
    }
    this.isSaving.set(true);
    this.modalError.set(null);
    const { ordre, description, tempsCuissonMinutes } = this.etapeForm.getRawValue();
    const request = { ordre: ordre!, description: description!, tempsCuissonMinutes };
    const target = this.targetEtape();

    const request$ =
      this.activeModal() === 'edit-etape' && target
        ? this.recetteService.updateEtape(this.recetteId, target.id, request)
        : this.recetteService.addEtape(this.recetteId, request);

    request$.subscribe({
      next: (etape) => {
        this.isSaving.set(false);
        const current = this.recette();
        if (!current) {
          return;
        }
        const etapes = (
          target ? current.etapes.map((e) => (e.id === etape.id ? etape : e)) : [...current.etapes, etape]
        ).sort((a, b) => a.ordre - b.ordre);
        this.recette.set({ ...current, etapes });
        this.closeModal();
      },
      error: (err) => {
        this.isSaving.set(false);
        this.modalError.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }

  openDeleteEtapeModal(etape: IEtape): void {
    this.targetEtape.set(etape);
    this.modalError.set(null);
    this.activeModal.set('delete-etape');
  }

  confirmDeleteEtape(): void {
    const target = this.targetEtape();
    if (!target) {
      return;
    }
    this.isSaving.set(true);
    this.modalError.set(null);
    this.recetteService.removeEtape(this.recetteId, target.id).subscribe({
      next: () => {
        this.isSaving.set(false);
        const current = this.recette();
        if (!current) {
          return;
        }
        this.recette.set({ ...current, etapes: current.etapes.filter((e) => e.id !== target.id) });
        this.closeModal();
      },
      error: (err) => {
        this.isSaving.set(false);
        this.modalError.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }

  openDeleteRecetteModal(): void {
    this.modalError.set(null);
    this.activeModal.set('delete-recette');
  }

  confirmDeleteRecette(): void {
    this.isSaving.set(true);
    this.modalError.set(null);
    this.recetteService.delete(this.recetteId).subscribe({
      next: () => {
        this.router.navigate(['/recettes']);
      },
      error: (err) => {
        this.isSaving.set(false);
        this.modalError.set(err.error?.message ?? 'Une erreur est survenue');
      }
    });
  }
}
