import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Rating} from "./rating";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class RatingService {

  private baseURL=environment.ratingsUrl;

  constructor(private httpClient: HttpClient) { }

  getRatingList(): Observable<Rating[]> {
    return this.httpClient.get<Rating[]>(`${this.baseURL}`);
  }

  getRatingById(id: number): Observable<Rating> {
    return this.httpClient.get<Rating>(`${this.baseURL}/${id}`);
  }

  getRatingByEmail(email: string): Observable<Rating[]> {
    return this.httpClient.get<Rating[]>(`${this.baseURL}/findByEmail?email=${email}`);
  }

  getRatingByISBN(isbn: string): Observable<Rating[]> {
    return this.httpClient.get<Rating[]>(`${this.baseURL}/findByISBN?isbn=${isbn}`);
  }

  createRating(rating: Rating): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}`, rating);
  }

  updateRating(rating: Rating): Observable<Object> {
    return this.httpClient.put(`${this.baseURL}/${rating.id}`, rating);
  }

  deleteRating(id: number): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/${id}`);
  }

  deleteAllRatings(): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/delete-all`)
  }
}
