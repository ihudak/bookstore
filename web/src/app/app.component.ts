import { Component } from '@angular/core';
import {environment} from "../environments/environment";

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    standalone: false
})
export class AppComponent {
  title = 'Book Store';
  buildVer = environment.verGUI;
  buildDate = environment.dateGUI;
  tenantId = environment.selectedTenant;
}
