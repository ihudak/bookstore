import { Component } from '@angular/core';
import {Client} from "../client";
import {ClientService} from "../client.service";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'app-update-client',
  templateUrl: './update-client.component.html',
  styleUrls: ['./update-client.component.css']
})
export class UpdateClientComponent {
  id: number = 0;
  client: Client = new Client();

  onSubmit() {
    this.clientService.updateClient(this.id, this.client).subscribe(data => {
      console.log(data);
      this.goToClientList();
    },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.clientService.getClientById(this.id).subscribe(data => {
      this.client = data;
    },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  constructor(private clientService: ClientService, private route: ActivatedRoute, private router: Router) {
  }

  goToClientList() {
    this.router.navigate(['/clients']);
  }
}
