import { Component } from '@angular/core';
import {Config} from "../config";
import {ConfigService} from "../config.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-config-list',
  templateUrl: './config-list.component.html',
  styleUrls: ['./config-list.component.css']
})
export class ConfigListComponent {
  configs: { [key: string]: Config[]; } = {};
  configsArray: Config[] = [];

  constructor(private configService: ConfigService, private router: Router) {
  }

  ngOnInit(): void {
    this.getConfigs();
  }

  private getConfigs() {
    this.configService.getConfigsList().subscribe(data => {
      this.configs = data;
      this.configsArray = [];

      for (let service in this.configs) {
        for (let conf of this.configs[service]) {
          this.configsArray.push(conf);
        }
      }
    });
  }

  updateConfig(service: string, id: string) {
    this.router.navigate(['update-config', service, id]);
  }

  toggleConfig(config: Config) {
    this.configService.toggleConfig(config).subscribe(data => {
      this.getConfigs();
    })
  }
}
