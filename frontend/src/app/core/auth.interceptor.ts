import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { ReplaySubject, catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

// Une seule requete de refresh en vol a la fois : refreshNotifier reste non-null
// tant qu'un cycle de refresh est en cours. Toute requete qui recoit un 401
// pendant ce temps se branche sur CE MEME ReplaySubject plutot que de declencher
// un second appel /refresh. Le refresh reel est lance une seule fois via un
// .subscribe() manuel explicite (pas via un partage automatique type shareReplay),
// et son resultat (succes ou echec) est repousse a la main dans le ReplaySubject
// pour que chaque requete en attente le recoive, garantissant qu'aucune requete ne
// reste bloquee indefiniment quelle que soit l'issue.
let refreshNotifier: ReplaySubject<string> | null = null;

function withAuthHeader(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const isAuthRoute = req.url.includes('/auth/');
  const token = authService.getAccessToken();

  const authedReq = token && !isAuthRoute ? withAuthHeader(req, token) : req;

  return next(authedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // /auth/refresh exclu explicitement : un 401 sur cette route ne doit jamais
      // redeclencher ce mecanisme, sinon boucle infinie de refresh-sur-refresh.
      if (error.status !== 401 || isAuthRoute) {
        return throwError(() => error);
      }

      if (!refreshNotifier) {
        const notifier = new ReplaySubject<string>(1);
        refreshNotifier = notifier;

        authService.refreshAccessToken().subscribe({
          next: (newToken) => notifier.next(newToken),
          error: (refreshError) => {
            refreshNotifier = null;
            authService.logout().subscribe();
            notifier.error(refreshError);
          },
          complete: () => {
            refreshNotifier = null;
            notifier.complete();
          }
        });
      }

      const notifier = refreshNotifier;

      return notifier.pipe(switchMap((newToken) => next(withAuthHeader(req, newToken))));
    })
  );
};
