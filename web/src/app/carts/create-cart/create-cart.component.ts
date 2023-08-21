import { Component } from '@angular/core';
import {Router} from "@angular/router";
import {CartService} from "../cart.service";
import {Cart} from "../cart";

@Component({
  selector: 'app-create-cart',
  templateUrl: './create-cart.component.html',
  styleUrls: ['./create-cart.component.css']
})
export class CreateCartComponent {
  cart: Cart = new Cart();
  constructor(private cartService: CartService, private router: Router) {
  }

  saveCart() {
    this.cart.createdAt = "";
    this.cart.updatedAt = "";
    this.cartService.createCart(this.cart).subscribe(data => {
        console.log(data);
        this.goToCartsList();
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToCartsList() {
    this.router.navigate(['/carts']);
  }

  onSubmit(){
    this.saveCart();
  }
}
