import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'recettes', pathMatch: 'full' },
  {
    path: 'connexion',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'inscription',
    loadComponent: () => import('./features/register/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: 'recettes',
    loadComponent: () => import('./features/recettes/recettes.component').then((m) => m.RecettesComponent),
    canActivate: [authGuard]
  },
  {
    path: 'recettes/nouvelle',
    loadComponent: () =>
      import('./features/nouvelle-recette/nouvelle-recette.component').then((m) => m.NouvelleRecetteComponent),
    canActivate: [authGuard]
  },
  {
    path: 'recettes/:id',
    loadComponent: () =>
      import('./features/recette-detail/recette-detail.component').then((m) => m.RecetteDetailComponent),
    canActivate: [authGuard]
  }
];
