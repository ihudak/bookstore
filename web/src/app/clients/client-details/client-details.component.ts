import { Component } from '@angular/core';
import {Client} from "../client";
import {ActivatedRoute} from "@angular/router";
import {ClientService} from "../client.service";

@Component({
  selector: 'app-client-details',
  templateUrl: './client-details.component.html',
  styleUrls: ['./client-details.component.css']
})
export class ClientDetailsComponent {
  id: number = 0;
  client: Client = new Client();

  constructor(private route: ActivatedRoute, private clientService: ClientService) {  }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.client = new Client();
    this.clientService.getClientById(this.id).subscribe(data => {
      this.client = data;
    })
  }
}
