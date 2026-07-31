import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { environment } from '../../environments/environment';

describe('authInterceptor', () => {
  let httpMock: HttpTestingController;
  let http: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()]
    });
    httpMock = TestBed.inject(HttpTestingController);
    http = TestBed.inject(HttpClient);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('sur deux 401 concurrents, un seul refresh est declenche et les deux requetes sont rejouees', () => {
    let resultA: unknown;
    let resultB: unknown;

    http.get('/api/recettes').subscribe((r) => (resultA = r));
    http.get('/api/recettes/1').subscribe((r) => (resultB = r));

    httpMock.expectOne('/api/recettes').flush(null, { status: 401, statusText: 'Unauthorized' });
    httpMock.expectOne('/api/recettes/1').flush(null, { status: 401, statusText: 'Unauthorized' });

    // Un seul appel /refresh malgre les deux 401, meme s'ils arrivent concurremment.
    const refreshReq = httpMock.expectOne((r) => r.url.includes('/auth/refresh'));
    refreshReq.flush({ accessToken: 'nouveau-token' });

    const retryA = httpMock.expectOne('/api/recettes');
    const retryB = httpMock.expectOne('/api/recettes/1');
    expect(retryA.request.headers.get('Authorization')).toBe('Bearer nouveau-token');
    expect(retryB.request.headers.get('Authorization')).toBe('Bearer nouveau-token');

    retryA.flush({ ok: 'A' });
    retryB.flush({ ok: 'B' });

    expect(resultA).toEqual({ ok: 'A' });
    expect(resultB).toEqual({ ok: 'B' });
  });

  it('un 401 sur /auth/refresh lui-meme ne redeclenche pas de second refresh (pas de boucle)', () => {
    let error: { status?: number } | undefined;

    http.post(`${environment.apiUrl}/auth/refresh`, {}).subscribe({
      error: (e) => (error = e)
    });

    const refreshReq = httpMock.expectOne((r) => r.url.includes('/auth/refresh'));
    refreshReq.flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(error?.status).toBe(401);
    httpMock.verify();
  });

  it('un echec du refresh propage une erreur a toutes les requetes en attente, aucune ne reste bloquee', () => {
    let errorA: unknown;
    let errorB: unknown;

    http.get('/api/recettes').subscribe({ error: (e) => (errorA = e) });
    http.get('/api/recettes/1').subscribe({ error: (e) => (errorB = e) });

    httpMock.expectOne('/api/recettes').flush(null, { status: 401, statusText: 'Unauthorized' });
    httpMock.expectOne('/api/recettes/1').flush(null, { status: 401, statusText: 'Unauthorized' });

    const refreshReq = httpMock.expectOne((r) => r.url.includes('/auth/refresh'));
    refreshReq.flush(null, { status: 401, statusText: 'Unauthorized' });

    // Le refresh a echoue : authService.logout() est appele, ce qui declenche son propre appel serveur.
    const logoutReq = httpMock.expectOne((r) => r.url.includes('/auth/logout'));
    logoutReq.flush(null);

    expect(errorA).toBeTruthy();
    expect(errorB).toBeTruthy();
  });
});
