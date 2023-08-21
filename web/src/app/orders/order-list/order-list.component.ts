import { Component } from '@angular/core';
import {Router} from "@angular/router";
import {Order} from "../order";
import {OrderService} from "../order.service";

@Component({
  selector: 'app-order-list',
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.css']
})
export class OrderListComponent {
  orders: Order[] = [];
  ngOnInit(): void {
    this.getOrders();
  }

  constructor(private orderService: OrderService, private router: Router) {
  }

  private getOrders() {
    this.orderService.getOrdersList().subscribe(data => {
      this.orders = data;
    });
  }

  getOrdersByEmail(email: string) {
    this.orderService.getOrdersByEmail(email).subscribe(data => {
      this.orders = data;
    });
  }

  getOrdersByISBN(isbn: string) {
    this.orderService.getOrdersByISBN(isbn).subscribe(data => {
      this.orders = data;
    });
  }

  updateOrder(id: number) {
    this.router.navigate(['update-order', id]);
  }

  orderDetails(id: number) {
    this.router.navigate(['order-details', id]);
  }

  payOrder(order: Order) {
    this.orderService.payOrder(order).subscribe(data => {
      console.log(data);
      this.getOrders();
    },
      error => { console.log(error); alert(error.error.message || error.message || error.error.error); }
    );
  }

  cancelOrder(order: Order) {
    this.orderService.cancelOrder(order).subscribe(data => {
      console.log(data);
      this.getOrders();
    },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  deleteOrder(id: number) {
    if (!confirm("Are you sure to delete order " + id)) { return;}
    this.orderService.deleteOrder(id).subscribe(data => {
      console.log(data);
      this.getOrders();
    })
  }

  deleteAllOrders() {
    if (!confirm("Are you sure to delete ALL orders")) { return;}
    this.orderService.deleteAllOrders().subscribe(data => {
      console.log(data);
      this.getOrders();
    })
  }
}
