import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: '',
    title: 'CocotteEclair',
    loadComponent: () => import('./features/landing/landing.component').then((m) => m.LandingComponent)
  },
  {
    path: 'recettes-publiques',
    title: 'Recettes publiques · CocotteEclair',
    loadComponent: () =>
      import('./features/recettes-publiques/recettes-publiques.component').then((m) => m.RecettesPubliquesComponent)
  },
  {
    path: 'connexion',
    title: 'Connexion · CocotteEclair',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'inscription',
    title: 'Inscription · CocotteEclair',
    loadComponent: () => import('./features/register/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: 'forgot-password',
    title: 'Mot de passe oublié · CocotteEclair',
    loadComponent: () =>
      import('./features/forgot-password/forgot-password.component').then((m) => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    title: 'Réinitialiser le mot de passe · CocotteEclair',
    loadComponent: () =>
      import('./features/reset-password/reset-password.component').then((m) => m.ResetPasswordComponent)
  },
  {
    path: 'recettes',
    title: 'Mes recettes · CocotteEclair',
    loadComponent: () => import('./features/recettes/recettes.component').then((m) => m.RecettesComponent),
    canActivate: [authGuard]
  },
  {
    path: 'recettes/nouvelle',
    title: 'Nouvelle recette · CocotteEclair',
    loadComponent: () =>
      import('./features/nouvelle-recette/nouvelle-recette.component').then((m) => m.NouvelleRecetteComponent),
    canActivate: [authGuard]
  },
  {
    path: 'recettes/:id',
    title: 'Recette · CocotteEclair',
    loadComponent: () =>
      import('./features/recette-detail/recette-detail.component').then((m) => m.RecetteDetailComponent),
    canActivate: [authGuard]
  },
  {
    path: '**',
    title: 'Page introuvable · CocotteEclair',
    loadComponent: () => import('./features/not-found/not-found.component').then((m) => m.NotFoundComponent)
  }
];
