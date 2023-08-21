import { Component } from '@angular/core';
import {Router} from "@angular/router";
import {CartService} from "../cart.service";
import {Cart} from "../cart";

@Component({
  selector: 'app-cart-list',
  templateUrl: './cart-list.component.html',
  styleUrls: ['./cart-list.component.css']
})
export class CartListComponent {
  carts: Cart[] = [];
  ngOnInit(): void {
    this.getCarts();
  }

  constructor(private cartService: CartService, private router: Router) {
  }

  private getCarts() {
    this.cartService.getCartsList().subscribe(data => {
      this.carts = data;
    });
  }

  getCartsByEmail(email: string) {
    this.cartService.getCartByEmail(email).subscribe(data => {
      this.carts = data;
    });
  }

  getCartsByISBN(isbn: string) {
    this.cartService.getCartByISBN(isbn).subscribe(data => {
      this.carts = data;
    });
  }

  updateCart(id: number) {
    this.router.navigate(['update-cart', id]);
  }

  deleteCart(cart: Cart) {
    if (!confirm("Are you sure to delete cart for user " + cart.email + " and book " + cart.isbn)) { return;}
    this.cartService.deleteCart(cart.id).subscribe(data => {
      console.log(data);
      this.getCarts();
    })
  }

  cartDetails(id: number) {
    this.router.navigate(['cart-details', id]);
  }

  deleteAllCarts() {
    if (!confirm("Are you sure to delete ALL carts")) { return;}
    this.cartService.deleteAllCarts().subscribe(data => {
      console.log(data);
      this.getCarts();
    })
  }
}
