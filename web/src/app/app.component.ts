import { Component } from '@angular/core';
import {environment} from "../environments/environment";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'Book Store' + environment.selectedTenant == '' ? '' : `: ${environment.selectedTenant}`;
  buildVer = environment.verGUI;
  buildDate = environment.dateGUI;
  tenantId = environment.selectedTenant;
}
