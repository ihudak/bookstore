import { Component } from '@angular/core';
import {Config} from "../config";
import {ConfigService} from "../config.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-create-config',
  templateUrl: './create-config.component.html',
  styleUrls: ['./create-config.component.css']
})
export class CreateConfigComponent {

  config: Config = new Config();
  constructor(private configService: ConfigService, private router: Router) {
  }

  saveConfig() {
    this.configService.createConfig(this.config).subscribe(data => {
        console.log(data);
        this.goToConfigList();
    },
        error => { console.log(error); alert(error.error.message); }
    );
  }

  goToConfigList() {
    this.router.navigate(['/configs']);
  }

  onSubmit() {
    this.saveConfig();
  }
}
