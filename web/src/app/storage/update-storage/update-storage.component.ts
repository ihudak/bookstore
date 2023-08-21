import { Component } from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {Storage} from "../storage";
import {StorageService} from "../storage.service";

@Component({
  selector: 'app-update-storage',
  templateUrl: './update-storage.component.html',
  styleUrls: ['./update-storage.component.css']
})
export class UpdateStorageComponent {
  id: number = 0;
  storage: Storage = new Storage();
  constructor(private storageService: StorageService, private route: ActivatedRoute, private router: Router) {
  }

  onSubmit() {
    this.storageService.updateStorage(this.storage).subscribe(data => {
        console.log(data);
        this.goToStorageList();
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.storageService.getStorageById(this.id).subscribe(data => {
        this.storage = data;
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToStorageList() {
    this.router.navigate(['/storage']);
  }
}
