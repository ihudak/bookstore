import { Component } from '@angular/core';
import {Storage} from "../storage";
import {StorageService} from "../storage.service";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'app-sell-storage',
  templateUrl: './sell-storage.component.html',
  styleUrls: ['./sell-storage.component.css']
})
export class SellStorageComponent {
  isbn: string = '';
  storage: Storage = new Storage();
  constructor(private storageService: StorageService, private route: ActivatedRoute, private router: Router) {
  }

  onSubmit() {
    this.storageService.sellFromStorage(this.storage).subscribe(data => {
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
