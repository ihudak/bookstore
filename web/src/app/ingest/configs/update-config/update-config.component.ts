import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {Config} from "../config";
import {ConfigService} from "../config.service";

@Component({
  selector: 'app-update-config',
  templateUrl: './update-config.component.html',
  styleUrls: ['./update-config.component.css']
})
export class UpdateConfigComponent implements OnInit {
  serviceId = '';
  id = '';
  config: Config = new Config();
  constructor(private configService: ConfigService, private route: ActivatedRoute, private router: Router) {
  }

  ngOnInit(): void {
    this.serviceId = this.route.snapshot.params['service'];
    this.id = this.route.snapshot.params['id'];
    this.configService.getConfigByServiceAndID(this.serviceId, this.id).subscribe(data => {
        this.config = data;
    },
        error => { console.log(error); alert(error.error.message); }
    );
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
