import {Component, OnInit} from '@angular/core';
import {Setting} from "../setting";
import {SettingService} from "../setting.service";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
    selector: 'app-update-setting',
    templateUrl: './update-setting.component.html',
    styleUrls: ['./update-setting.component.css'],
    standalone: false
})
export class UpdateSettingComponent implements OnInit {
  id: string = "";
  setting: Setting = new Setting();
  constructor(private settingService: SettingService, private route: ActivatedRoute, private router: Router) {
  }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.settingService.getSettingByID(this.id).subscribe(data => {
      this.setting = data;
    },
        error => { alert(error.error.message); }
    );
  }

  goToSettingList() {
    this.router.navigate(['/settings']);
  }

  onSubmit() {
    this.settingService.updateSetting(this.id, this.setting).subscribe(data => {
      this.goToSettingList();
    },
      error => { alert(error.error.message); }
    );
  }
}
