import { Component } from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {Storage} from "../storage";
import {StorageService} from "../storage.service";

@Component({
  selector: 'app-ingest-storage',
  templateUrl: './ingest-storage.component.html',
  styleUrls: ['./ingest-storage.component.css']
})
export class IngestStorageComponent {
  isbn: string = '';
  storage: Storage = new Storage();
  constructor(private storageService: StorageService, private route: ActivatedRoute, private router: Router) {
  }

  onSubmit() {
    this.storageService.ingestInStorage(this.storage).subscribe(data => {
        console.log(data);
        this.goToStorageList();
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  ngOnInit(): void {
    this.isbn = this.route.snapshot.params['isbn'];
    this.storageService.getStorageByISBN(this.isbn).subscribe(data => {
        this.storage = data;
        this.storage.quantity = 0;
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToStorageList() {
    this.router.navigate(['/storage']);
  }
}
