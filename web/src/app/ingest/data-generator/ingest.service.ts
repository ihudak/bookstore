import { Injectable } from '@angular/core';
import {environment} from "../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import {Observable} from "rxjs";
import {Ingest} from "./ingest";

@Injectable({
  providedIn: 'root'
})
export class IngestService {

  private baseURL=`${environment.ingestSrvUrl}/api/v1/ingest`;
  constructor(private httpClient: HttpClient) { }

  createIngest(ingest: Ingest, serviceId: string): Observable<Object> {
    let url: string = serviceId == 'all' ? `${this.baseURL}` : `${this.baseURL}/${serviceId}`;
    return this.httpClient.post(url, ingest);
  }
}
