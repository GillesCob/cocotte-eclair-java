import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type IUnite =
  | 'GRAMME'
  | 'KILOGRAMME'
  | 'MILLILITRE'
  | 'LITRE'
  | 'UNITE'
  | 'CUILLERE_A_SOUPE'
  | 'CUILLERE_A_CAFE'
  | 'PINCEE';

export interface IRecetteIngredient {
  id: string;
  ingredientNom: string;
  quantite: number;
  unite: IUnite;
}

export interface IEtape {
  id: string;
  ordre: number;
  description: string;
  tempsCuissonMinutes: number | null;
}

export interface IRecette {
  id: string;
  titre: string;
  description: string | null;
  visibilite: 'PRIVEE' | 'PUBLIQUE';
  estRecetteDeBase: boolean;
  ingredients: IRecetteIngredient[];
  etapes: IEtape[];
}

export interface IRecetteRequest {
  titre: string;
  description: string | null;
  visibilite: 'PRIVEE' | 'PUBLIQUE';
}

export interface IRecetteIngredientRequest {
  ingredientNom: string;
  quantite: number;
  unite: IUnite;
}

export interface IEtapeRequest {
  ordre: number;
  description: string;
  tempsCuissonMinutes: number | null;
}

@Injectable({ providedIn: 'root' })
export class RecetteService {
  constructor(private http: HttpClient) {}

  findAll(): Observable<IRecette[]> {
    return this.http.get<IRecette[]>(`${environment.apiUrl}/recettes`);
  }

  findById(id: string): Observable<IRecette> {
    return this.http.get<IRecette>(`${environment.apiUrl}/recettes/${id}`);
  }

  create(request: IRecetteRequest): Observable<IRecette> {
    return this.http.post<IRecette>(`${environment.apiUrl}/recettes`, request);
  }

  addIngredient(recetteId: string, request: IRecetteIngredientRequest): Observable<IRecetteIngredient> {
    return this.http.post<IRecetteIngredient>(`${environment.apiUrl}/recettes/${recetteId}/ingredients`, request);
  }

  removeIngredient(recetteId: string, ingredientLineId: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/recettes/${recetteId}/ingredients/${ingredientLineId}`);
  }

  addEtape(recetteId: string, request: IEtapeRequest): Observable<IEtape> {
    return this.http.post<IEtape>(`${environment.apiUrl}/recettes/${recetteId}/etapes`, request);
  }

  removeEtape(recetteId: string, etapeId: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/recettes/${recetteId}/etapes/${etapeId}`);
  }
}
