import { Component } from '@angular/core';
import {Storage} from "../storage";
import {StorageService} from "../storage.service";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'app-create-storage',
  templateUrl: './create-storage.component.html',
  styleUrls: ['./create-storage.component.css']
})
export class CreateStorageComponent {
  storage: Storage = new Storage();
  constructor(private storageService: StorageService, private route: ActivatedRoute, private router: Router) {
  }

  saveStorage() {
    this.storageService.createStorage(this.storage).subscribe(data => {
        console.log(data);
        this.goToStorageList();
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToStorageList() {
    this.router.navigate(['/storage']);
  }

  onSubmit(){
    this.saveStorage();
  }
}
