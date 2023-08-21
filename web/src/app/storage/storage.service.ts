import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Storage} from "./storage";
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class StorageService {
  private baseURL=environment.storageUrl;

  constructor(private httpClient: HttpClient) { }

  getStorageList(): Observable<Storage[]> {
    return this.httpClient.get<Storage[]>(`${this.baseURL}`);
  }

  getStorageById(id: number): Observable<Storage> {
    return this.httpClient.get<Storage>(`${this.baseURL}/${id}`);
  }

  getStorageByISBN(isbn: string): Observable<Storage> {
    return this.httpClient.get<Storage>(`${this.baseURL}/findByISBN?isbn=${isbn}`);
  }

  createStorage(storage: Storage): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}/ingest-book`, storage);
  }

  ingestInStorage(storage: Storage): Observable<Object>{
    return this.httpClient.post(`${this.baseURL}/ingest-book`, storage);
  }

  sellFromStorage(storage: Storage): Observable<Object>{
    return this.httpClient.post(`${this.baseURL}/sell-book`, storage);
  }

  updateStorage(storage: Storage): Observable<Object> {
    return this.httpClient.put(`${this.baseURL}/${storage.id}`, storage);
  }

  deleteStorage(id: number): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/${id}`);
  }

  deleteAllStorages(): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/delete-all`)
  }
}
