import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../environments/environment';

interface IAccessTokenResponse {
  accessToken: string;
}

// En-tete requis sur /refresh et /logout : ces deux routes s'appuient sur le
// cookie httpOnly (SameSite=Lax), contrairement aux autres appels qui utilisent le
// header Authorization. Ce header custom, combine a SameSite=Lax et a la politique
// CORS stricte du backend, sert de defense CSRF en profondeur sur ces deux routes
// (cf SecurityConfig/CsrfHeaderCheckFilter/RefreshCookieFactory cote backend).
const CSRF_HEADER = { 'X-Requested-With': 'XMLHttpRequest' };

@Injectable({ providedIn: 'root' })
export class AuthService {
  // L'access token n'est jamais persiste (ni localStorage, ni sessionStorage) :
  // garde uniquement en memoire JS, reconstitue au demarrage de l'app via
  // initializeSession() qui s'appuie sur le refresh token en cookie httpOnly.
  private accessToken: string | null = null;

  readonly isAuthenticated = signal<boolean>(false);

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<void> {
    return this.http
      .post<IAccessTokenResponse>(`${environment.apiUrl}/auth/login`, { email, password }, { withCredentials: true })
      .pipe(
        tap((response) => this.setSession(response.accessToken)),
        map(() => undefined)
      );
  }

  register(email: string, password: string): Observable<void> {
    return this.http
      .post<IAccessTokenResponse>(
        `${environment.apiUrl}/auth/register`,
        { email, password },
        { withCredentials: true }
      )
      .pipe(
        tap((response) => this.setSession(response.accessToken)),
        map(() => undefined)
      );
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/reset-password`, { token, newPassword });
  }

  // Appelee une seule fois au demarrage de l'app (provideAppInitializer, cf
  // app.config.ts) : tente de reconstituer la session depuis le cookie refresh,
  // sans jamais faire echouer le demarrage de l'app si ca ne marche pas (VPS
  // injoignable, pas de session existante...). Resout toujours, ne propage jamais
  // d'erreur.
  initializeSession(): Observable<boolean> {
    return this.refreshAccessToken().pipe(
      map(() => true),
      catchError(() => {
        this.clearSession();
        return of(false);
      })
    );
  }

  refreshAccessToken(): Observable<string> {
    return this.http
      .post<IAccessTokenResponse>(
        `${environment.apiUrl}/auth/refresh`,
        {},
        { withCredentials: true, headers: CSRF_HEADER }
      )
      .pipe(
        tap((response) => this.setSession(response.accessToken)),
        map((response) => response.accessToken)
      );
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${environment.apiUrl}/auth/logout`, {}, { withCredentials: true, headers: CSRF_HEADER })
      .pipe(
        map(() => undefined),
        catchError(() => of(undefined)),
        tap(() => this.clearSession())
      );
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  private setSession(accessToken: string): void {
    this.accessToken = accessToken;
    this.isAuthenticated.set(true);
  }

  private clearSession(): void {
    this.accessToken = null;
    this.isAuthenticated.set(false);
  }
}
