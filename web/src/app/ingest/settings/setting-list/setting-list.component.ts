import {Component, OnInit} from '@angular/core';
import {Setting} from "../setting";
import {SettingService} from "../setting.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-setting-list',
  templateUrl: './setting-list.component.html',
  styleUrls: ['./setting-list.component.css']
})
export class SettingListComponent implements OnInit {
  settings: Setting[] = [];

  constructor(private settingService: SettingService, private router: Router) {
  }

  ngOnInit(): void {
    this.getSettings();
  }

  private getSettings() {
    this.settingService.getSettingsList().subscribe(data => {
      this.settings = data;
    });
  }

  updateSetting(id: string) {
    this.router.navigate(['update-setting', id])
  }

  deleteSetting(id: string) {
    this.settingService.deleteSetting(id).subscribe(data => {
      this.getSettings();
    })
  }
}
