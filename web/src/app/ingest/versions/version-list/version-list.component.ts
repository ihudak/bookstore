import { Component } from '@angular/core';
import {Version} from "../version";
import {VersionService} from "../version.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-version-list',
  templateUrl: './version-list.component.html',
  styleUrls: ['./version-list.component.css']
})
export class VersionListComponent {
  versions: Version[] = [];

  constructor(private versionService: VersionService, private router: Router) {
  }

  ngOnInit(): void {
    this.getVersions();
  }

  private getVersions() {
    this.versionService.getVersionsList().subscribe(data => {
      this.versions = data;
    });
  }
}
