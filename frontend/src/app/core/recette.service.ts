import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface IRecette {
  id: string;
  titre: string;
  description: string | null;
  visibilite: 'PRIVEE' | 'PUBLIQUE';
  estRecetteDeBase: boolean;
}

export interface IRecetteRequest {
  titre: string;
  description: string | null;
  visibilite: 'PRIVEE' | 'PUBLIQUE';
}

@Injectable({ providedIn: 'root' })
export class RecetteService {
  constructor(private http: HttpClient) {}

  findAll(): Observable<IRecette[]> {
    return this.http.get<IRecette[]>(`${environment.apiUrl}/recettes`);
  }

  create(request: IRecetteRequest): Observable<IRecette> {
    return this.http.post<IRecette>(`${environment.apiUrl}/recettes`, request);
  }
}
