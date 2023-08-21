import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Order} from "./order";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private baseURL=environment.ordersUrl;
  constructor(private httpClient: HttpClient) { }

  getOrdersList(): Observable<Order[]> {
    return this.httpClient.get<Order[]>(`${this.baseURL}`);
  }

  getOrdersByEmail(email: string): Observable<Order[]> {
    return this.httpClient.get<Order[]>(`${this.baseURL}/findByEmail?email=${email}`);
  }

  getOrdersByISBN(isbn: string): Observable<Order[]> {
    return this.httpClient.get<Order[]>(`${this.baseURL}/findByISBN?isbn=${isbn}`);
  }

  getOrderById(id: number): Observable<Order> {
    return this.httpClient.get<Order>(`${this.baseURL}/${id}`);
  }

  createOrder(order: Order): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}`, order);
  }

  updateOrder(id: number, order: Order): Observable<Object> {
    return this.httpClient.put(`${this.baseURL}/${id}`, order);
  }

  deleteOrder(id: number): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/${id}`);
  }

  deleteAllOrders(): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/delete-all`)
  }

  payOrder(order: Order): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}/submit`, order);
  }

  cancelOrder(order: Order): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}/cancel`, order);
  }
}
