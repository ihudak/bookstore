import { Component } from '@angular/core';
import {Setting} from "../setting";
import {SettingService} from "../setting.service";
import {Router} from "@angular/router";

@Component({
    selector: 'app-create-setting',
    templateUrl: './create-setting.component.html',
    styleUrls: ['./create-setting.component.css'],
    standalone: false
})
export class CreateSettingComponent {

  setting: Setting = new Setting();
  constructor(private settingService: SettingService, private router: Router) {
  }

  saveSetting() {
    this.settingService.createSetting(this.setting).subscribe(data => {
      this.goToSettingList();
    },
      error => { alert(error.error.message); }
    );
  }

  goToSettingList() {
    this.router.navigate(['/settings']);
  }
  onSubmit() {
    this.saveSetting();
  }
}
