import { Component } from '@angular/core';
import {Router} from "@angular/router";
import {Order} from "../order";
import {OrderService} from "../order.service";

@Component({
  selector: 'app-create-order',
  templateUrl: './create-order.component.html',
  styleUrls: ['./create-order.component.css']
})
export class CreateOrderComponent {
  order: Order = new Order();
  constructor(private orderService: OrderService, private router: Router) {
  }

  saveOrder() {
    this.order.createdAt = "";
    this.order.updatedAt = "";
    this.order.completed = false;
    this.order.price = 0.0;
    this.orderService.createOrder(this.order).subscribe(data => {
        console.log(data);
        this.goToOrdersList();
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToOrdersList() {
    this.router.navigate(['/orders']);
  }

  onSubmit(){
    this.saveOrder();
  }
}
