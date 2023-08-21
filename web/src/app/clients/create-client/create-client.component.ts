import { Component } from '@angular/core';
import {Client} from "../client";
import {ClientService} from "../client.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-create-client',
  templateUrl: './create-client.component.html',
  styleUrls: ['./create-client.component.css']
})
export class CreateClientComponent {
  client: Client = new Client();
  constructor(private clientService: ClientService, private router: Router) {
  }

  saveClient() {
    this.clientService.createClient(this.client).subscribe(data => {
      console.log(data);
      this.goToClientList();
    },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToClientList() {
    this.router.navigate(['/clients']);
  }

  onSubmit(){
    this.saveClient();
  }
}
