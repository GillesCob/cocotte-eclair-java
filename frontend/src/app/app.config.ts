import { ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';
import { AuthService } from './core/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    // Tente de reconstituer la session (access token en memoire) depuis le cookie
    // refresh avant que le routeur ne monte la premiere route : authGuard peut
    // ainsi rester synchrone sur le signal isAuthenticated. initializeSession()
    // resout toujours (jamais de rejet), un backend injoignable au chargement ne
    // bloque donc jamais le demarrage de l'app.
    provideAppInitializer(() => firstValueFrom(inject(AuthService).initializeSession()))
  ]
};
