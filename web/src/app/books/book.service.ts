import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Book} from "./book";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class BookService {
  private baseURL=environment.booksUrl;
  constructor(private httpClient: HttpClient) { }
  getBooksList(): Observable<Book[]> {
    return this.httpClient.get<Book[]>(`${this.baseURL}`);
  }

  getBookById(id: number): Observable<Book> {
    return this.httpClient.get<Book>(`${this.baseURL}/${id}`);
  }

  getBookByIsbn(isbn: string): Observable<Book> {
    return this.httpClient.get<Book>(`${this.baseURL}/find?isbn=${isbn}`);
  }

  createBook(book: Book): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}`, book);
  }

  updateBook(id: number, book: Book): Observable<Object> {
    return this.httpClient.put(`${this.baseURL}/${id}`, book);
  }

  vendBook(book: Book): Observable<Object> {
    return this.httpClient.post<Book>(`${this.baseURL}/vend?isbn=${book.isbn}`, book);
  }

  deleteBook(id: number): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/${id}`);
  }

  deleteAllBooks(): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/delete-all`);
  }

  vendAllBooks(): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}/vend-all`, "");
  }

  unvendAllBooks(): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/vend-all`);
  }
}
