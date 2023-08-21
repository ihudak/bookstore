import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Client} from "./client";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class ClientService {
  private baseURL=environment.clientsUrl;
  constructor(private httpClient: HttpClient) { }
  getClientsList(): Observable<Client[]> {
    return this.httpClient.get<Client[]>(`${this.baseURL}`);
  }

  getClientById(id: number): Observable<Client> {
    return this.httpClient.get<Client>(`${this.baseURL}/${id}`);
  }

  createClient(client: Client): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}`, client);
  }

  updateClient(id: number, client: Client): Observable<Object> {
    return this.httpClient.put(`${this.baseURL}/${id}`, client);
  }

  deleteClient(id: number): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/${id}`);
  }

  deleteAllClients(): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/delete-all`)
  }
}
