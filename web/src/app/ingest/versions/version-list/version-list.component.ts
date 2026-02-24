import {Component, OnInit} from '@angular/core';
import {Version} from "../version";
import {VersionService} from "../version.service";
import {Router} from "@angular/router";

@Component({
    selector: 'app-version-list',
    templateUrl: './version-list.component.html',
    styleUrls: ['./version-list.component.css'],
    standalone: false
})
export class VersionListComponent implements OnInit {
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
