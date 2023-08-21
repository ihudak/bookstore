import { Component } from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Cart} from "../cart";
import {CartService} from "../cart.service";

@Component({
  selector: 'app-cart-details',
  templateUrl: './cart-details.component.html',
  styleUrls: ['./cart-details.component.css']
})
export class CartDetailsComponent {
  id: number = 0;
  cart: Cart = new Cart();

  constructor(private route: ActivatedRoute, private cartService: CartService) {  }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.cart = new Cart();
    this.cartService.getCartById(this.id).subscribe(data => {
      this.cart = data;
    })
  }
}
