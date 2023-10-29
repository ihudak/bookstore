import { Injectable } from '@angular/core';
import {environment} from "../../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Config} from "./config";

@Injectable({
  providedIn: 'root'
})
export class ConfigService {

    private baseURL=`${environment.ingestSrvUrl}/api/v1/config`;
    constructor(private httpClient: HttpClient) { }

    getConfigsList(): Observable<{[key: string]: Config[] }> {
        return this.httpClient.get<{[key: string]: Config[] }>(`${this.baseURL}`);
    }

    createConfig(config: Config): Observable<Object> {
        let url: string = config.serviceId == 'all' ? `${this.baseURL}` : `${this.baseURL}/${config.serviceId}`;
        return this.httpClient.post(url, config);
    }
}
