import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Cart} from "./cart";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private baseURL=environment.cartsUrl;

  constructor(private httpClient: HttpClient) { }

  getCartsList(): Observable<Cart[]> {
    return this.httpClient.get<Cart[]>(`${this.baseURL}`);
  }

  getCartById(id: number): Observable<Cart> {
    return this.httpClient.get<Cart>(`${this.baseURL}/${id}`);
  }

  getCartByEmail(email: string): Observable<Cart[]> {
    return this.httpClient.get<Cart[]>(`${this.baseURL}/findByEmail?email=${email}`);
  }

  getCartByISBN(isbn: string): Observable<Cart[]> {
    return this.httpClient.get<Cart[]>(`${this.baseURL}/findByISBN?isbn=${isbn}`);
  }

  createCart(cart: Cart): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}`, cart);
  }

  updateCart(cart: Cart): Observable<Object> {
    return this.httpClient.put(`${this.baseURL}`, cart);
  }

  deleteCart(id: number): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/${id}`);
  }

  deleteAllCarts(): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/delete-all`)
  }
}
