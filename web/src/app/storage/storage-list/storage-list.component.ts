import { Component } from '@angular/core';
import {Storage} from "../storage";
import {Router} from "@angular/router";
import {StorageService} from "../storage.service";

@Component({
  selector: 'app-storage-list',
  templateUrl: './storage-list.component.html',
  styleUrls: ['./storage-list.component.css']
})
export class StorageListComponent {
  storages: Storage[] = [];
  ngOnInit(): void {
    this.getStorages();
  }

  constructor(private storageService: StorageService, private router: Router) {
  }

  private getStorages() {
    this.storageService.getStorageList().subscribe(data => {
      this.storages = data;
    });
  }

  updateStorage(id: number) {
    this.router.navigate(['update-storage', id]);
  }

  ingestToStorage(isbn: string) {
    this.router.navigate(['ingest-storage', isbn]);
  }

  sellFromStorage(isbn: string) {
    this.router.navigate(['sell-storage', isbn]);
  }

  deleteStorage(storage: Storage) {
    if (!confirm("Are you sure to delete book from storage " + storage.isbn)) { return;}
    this.storageService.deleteStorage(storage.id).subscribe(data => {
      console.log(data);
      this.getStorages();
    })
  }

  storageDetails(id: number) {
    this.router.navigate(['storage-details', id]);
  }

  deleteAllStorage() {
    if (!confirm("Are you sure to delete ALL books from storage")) { return;}
    this.storageService.deleteAllStorages().subscribe(data => {
      console.log(data);
      this.getStorages();
    })
  }
}
