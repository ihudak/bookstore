import { Component } from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {Cart} from "../cart";
import {CartService} from "../cart.service";

@Component({
  selector: 'app-update-cart',
  templateUrl: './update-cart.component.html',
  styleUrls: ['./update-cart.component.css']
})
export class UpdateCartComponent {
  id: number = 0;
  cart: Cart = new Cart();
  constructor(private cartService: CartService, private route: ActivatedRoute, private router: Router) {
  }

  onSubmit() {
    this.cartService.updateCart(this.cart).subscribe(data => {
        console.log(data);
        this.goToCartsList();
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.cartService.getCartById(this.id).subscribe(data => {
        this.cart = data;
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToCartsList() {
    this.router.navigate(['/carts']);
  }
}
