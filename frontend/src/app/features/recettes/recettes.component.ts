import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RecetteService, IRecette } from '../../core/recette.service';
import { AuthService } from '../../core/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-recettes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recettes.component.html',
  styleUrl: './recettes.component.scss'
})
export class RecettesComponent implements OnInit {
  readonly recettes = signal<IRecette[]>([]);
  readonly isLoading = signal(true);

  constructor(
    private recetteService: RecetteService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.recetteService.findAll().subscribe({
      next: (recettes) => {
        this.recettes.set(recettes);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/connexion']);
  }
}
