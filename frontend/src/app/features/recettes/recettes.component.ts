import { Component, OnInit, signal } from '@angular/core';
import { RecetteService, IRecette } from '../../core/recette.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-recettes',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './recettes.component.html',
  styleUrl: './recettes.component.scss'
})
export class RecettesComponent implements OnInit {
  readonly recettes = signal<IRecette[]>([]);
  readonly isLoading = signal(true);

  constructor(private recetteService: RecetteService) {}

  ngOnInit(): void {
    this.recetteService.findAll().subscribe({
      next: (recettes) => {
        this.recettes.set(recettes);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }
}
