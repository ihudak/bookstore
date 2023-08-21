import {Component, OnInit} from '@angular/core';
import {Client} from "../client";
import {ClientService} from "../client.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-client-list',
  templateUrl: './client-list.component.html',
  styleUrls: ['./client-list.component.css']
})
export class ClientListComponent implements OnInit {

  clients: Client[] = [];
  ngOnInit(): void {
    this.getClients();
  }

  private getClients() {
    this.clientService.getClientsList().subscribe(data => {
      this.clients = data;
    });
  }

  updateClient(id: number) {
    this.router.navigate(['update-client', id]);
  }
  constructor(private clientService: ClientService, private router: Router) {
  }

  deleteClient(id: number) {
    if (!confirm("Are you sure to delete client " + id)) { return;}
    this.clientService.deleteClient(id).subscribe(data => {
      console.log(data);
      this.getClients();
    })
  }

  clientDetails(id: number) {
    this.router.navigate(['client-details', id]);
  }

  deleteAllClients() {
    if (!confirm("Are you sure to delete ALL clients")) { return;}
    this.clientService.deleteAllClients().subscribe(data => {
      console.log(data);
      this.getClients();
    })
  }
}
