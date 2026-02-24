import { Injectable } from '@angular/core';
import {environment} from "../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import {Observable} from "rxjs";
import {Version} from "./version";

@Injectable({
  providedIn: 'root'
})
export class VersionService {

    private baseURL=`${environment.ingestSrvUrl}/api/v1/version`;
    constructor(private httpClient: HttpClient) { }

    getVersionsList(): Observable<Version[]> {
        return this.httpClient.get<Version[]>(`${this.baseURL}`);
    }

    getIngestVersion(): Observable<Version> {
        return this.httpClient.get<Version>(`${this.baseURL}/ingest`);
    }
}
