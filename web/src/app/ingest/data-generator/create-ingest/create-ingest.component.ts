import { Component } from '@angular/core';
import {Ingest} from "../ingest";
import {IngestService} from "../ingest.service";
import {Router} from "@angular/router";
import {Version} from "../../versions/version";
import {VersionService} from "../../versions/version.service";

@Component({
  selector: 'app-create-ingest',
  templateUrl: './create-ingest.component.html',
  styleUrls: ['./create-ingest.component.css']
})
export class CreateIngestComponent {

  ingest: Ingest = new Ingest();
  genInProgress: boolean = false;
  status: string = '';

  constructor(private ingestService: IngestService, private router: Router, private versionService: VersionService) {
  }

  ngOnInit(): void {
    this.versionService.getIngestVersion().subscribe(data => {
        let version: Version = data;

        if (version.serviceId === 'ingest') {
          this.status = version.status || '';

          if (version.status?.includes('progress') ||
              version.status?.includes('ON') ||
              version.status?.includes('Progress')
          ) {
            this.genInProgress = true;
          }
        }
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  startIngest(serviceId: string) {
    this.ingestService.createIngest(this.ingest, serviceId).subscribe(data => {
        console.log(data);
        this.goToIngest();
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToVersionList() {
    this.router.navigate(['/versions']);
  }

  goToIngest() {
    this.router.navigate(['/ingest']);
  }
  onSubmit() {
    this.startIngest('all');
  }

  onSubmitService(serviceId: string) {
    this.startIngest(serviceId);
  }
}
