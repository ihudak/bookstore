import { Component } from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {Order} from "../order";
import {OrderService} from "../order.service";

@Component({
  selector: 'app-update-order',
  templateUrl: './update-order.component.html',
  styleUrls: ['./update-order.component.css']
})
export class UpdateOrderComponent {
  id: number = 0;
  order: Order = new Order();
  constructor(private orderService: OrderService, private route: ActivatedRoute, private router: Router) {
  }

  onSubmit() {
    this.orderService.updateOrder(this.id, this.order).subscribe(data => {
        console.log(data);
        this.goToCartsList();
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.orderService.getOrderById(this.id).subscribe(data => {
        this.order = data;
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToCartsList() {
    this.router.navigate(['/orders']);
  }
}
