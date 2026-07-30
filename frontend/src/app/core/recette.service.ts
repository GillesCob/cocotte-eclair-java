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

@Injectable({ providedIn: 'root' })
export class RecetteService {
  constructor(private http: HttpClient) {}

  findAll(): Observable<IRecette[]> {
    return this.http.get<IRecette[]>(`${environment.apiUrl}/recettes`);
  }
}
