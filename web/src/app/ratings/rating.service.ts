import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams, HttpResponse } from "@angular/common/http";
import {Observable, catchError, map, of, timeout} from "rxjs";
import {Rating} from "./rating";
import {environment} from "../../environments/environment";
import {
  EnteredRating,
  LoadOutcome,
  RATINGS_BOUNDED_TIME_MS,
  RatingLine,
  RatingToEdit,
  WriteOutcome,
  classifyLoad,
  classifyWrite,
  readSummaries,
  readSummary
} from "./rating-outcomes";
import {CANNOT_LOAD, bookLineFor, catalogueLineFor} from "./rating-texts";

interface Answer { status: number; body: unknown; }

// bounds a request and reduces its response or error to a status and a body; never errors
function answerOf(request: Observable<HttpResponse<unknown>>): Observable<Answer> {
  return request.pipe(
    timeout({ first: RATINGS_BOUNDED_TIME_MS }),
    map((response): Answer => ({ status: response.status, body: response.body })),
    catchError((error: unknown) => of<Answer>(error instanceof HttpErrorResponse
      ? { status: error.status, body: error.error }
      : { status: 0, body: null }))
  );
}

@Injectable({
  providedIn: 'root'
})
export class RatingService {

  private baseURL=`${environment.ratingsSrvUrl}/api/v1/ratings`;

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

  give(entry: EnteredRating): Observable<WriteOutcome> {
    const body = { email: entry.email, isbn: entry.isbn, rating: entry.rating, comment: entry.comment };
    return answerOf(this.httpClient.post<unknown>(`${this.baseURL}`, body, { observe: 'response' })).pipe(
      map(answer => classifyWrite(answer.status, answer.body, 'give'))
    );
  }

  change(loaded: RatingToEdit, stars: number, comment: string): Observable<WriteOutcome> {
    const body = { id: loaded.id, email: loaded.email, isbn: loaded.isbn, rating: stars, comment };
    return answerOf(this.httpClient.put<unknown>(`${this.baseURL}/${loaded.id}`, body, { observe: 'response' })).pipe(
      map(answer => classifyWrite(answer.status, answer.body, 'change'))
    );
  }

  loadRating(id: string | number): Observable<LoadOutcome> {
    // only a plain run of digits is a rating id; Number() alone would read ' ', '0x7' or '1e2' as one
    const digits = String(id);
    if (!/^\d+$/.test(digits)) {
      return of<LoadOutcome>({ kind: 'cannot-load', text: CANNOT_LOAD });
    }
    const n = Number(digits);
    return answerOf(this.httpClient.get<unknown>(`${this.baseURL}/${n}`, { observe: 'response' })).pipe(
      map(answer => classifyLoad(answer.status, answer.body, n))
    );
  }

  bookLine(isbn: string): Observable<RatingLine> {
    const params = new HttpParams().set('isbn', isbn);
    return answerOf(this.httpClient.get<unknown>(`${this.baseURL}/summary`, { params, observe: 'response' })).pipe(
      map(answer => bookLineFor(answer.status === 200 ? readSummary(answer.body) : null))
    );
  }

  catalogueLines(): Observable<(isbn: string) => RatingLine> {
    return answerOf(this.httpClient.get<unknown>(`${this.baseURL}/summaries`, { observe: 'response' })).pipe(
      map(answer => {
        const summaries = answer.status === 200 ? readSummaries(answer.body) : null;
        return (isbn: string) => catalogueLineFor(summaries, isbn);
      })
    );
  }
}
