import { Injectable } from '@angular/core';
import {environment} from "../../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {firstValueFrom, Observable} from "rxjs";
import {Setting} from "./setting";

@Injectable({
  providedIn: 'root'
})
export class SettingService {
  private baseURL=`${environment.ingestSrvUrl}/api/v1/settings`;

  constructor(private httpClient: HttpClient) { }

  getSettingsList(): Observable<Setting[]>{
    return this.httpClient.get<Setting[]>(`${this.baseURL}`);
  }

  createSetting(setting: Setting): Observable<Setting> {
    return this.httpClient.post<Setting>(`${this.baseURL}`, setting);
  }

  getSettingByID(id: string): Observable<Setting> {
    return this.httpClient.get<Setting>(`${this.baseURL}/tenant/${id}`);
  }

  updateSetting(id: string, setting: Setting): Observable<Setting> {
    return this.httpClient.put<Setting>(`${this.baseURL}/${id}`, setting);
  }

  deleteSetting(id: string): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/${id}`);
  }

}
