import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

// Route protégée : ne jamais monter une page qui dépend de données authentifiées
// sans vérifier la session au préalable (checklist V1 native).
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/connexion']);
  return false;
};
