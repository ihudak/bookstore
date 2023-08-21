import { Component } from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Storage} from "../storage";
import {StorageService} from "../storage.service";

@Component({
  selector: 'app-storage-details',
  templateUrl: './storage-details.component.html',
  styleUrls: ['./storage-details.component.css']
})
export class StorageDetailsComponent {
  id: number = 0;
  storage: Storage = new Storage();

  constructor(private route: ActivatedRoute, private storageService: StorageService) {  }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.storage = new Storage();
    this.storageService.getStorageById(this.id).subscribe(data => {
      this.storage = data;
    })
  }
}
