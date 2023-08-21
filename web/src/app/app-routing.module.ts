import {RouterModule, Routes} from "@angular/router";
import {NgModule} from "@angular/core";
import {ClientListComponent} from "./clients/client-list/client-list.component";
import {CreateClientComponent} from "./clients/create-client/create-client.component";
import {UpdateClientComponent} from "./clients/update-client/update-client.component";
import {ClientDetailsComponent} from "./clients/client-details/client-details.component";
import {BookListComponent} from "./books/book-list/book-list.component";
import {BookDetailsComponent} from "./books/book-details/book-details.component";
import {CreateBookComponent} from "./books/create-book/create-book.component";
import {UpdateBookComponent} from "./books/update-book/update-book.component";
import {CartListComponent} from "./carts/cart-list/cart-list.component";
import {CreateCartComponent} from "./carts/create-cart/create-cart.component";
import {UpdateCartComponent} from "./carts/update-cart/update-cart.component";
import {StorageListComponent} from "./storage/storage-list/storage-list.component";
import {IngestStorageComponent} from "./storage/ingest-storage/ingest-storage.component";
import {SellStorageComponent} from "./storage/sell-storage/sell-storage.component";
import {CreateStorageComponent} from "./storage/create-storage/create-storage.component";
import {OrderListComponent} from "./orders/order-list/order-list.component";
import {CreateOrderComponent} from "./orders/create-order/create-order.component";
import {UpdateOrderComponent} from "./orders/update-order/update-order.component";
import {OrderDetailsComponent} from "./orders/order-details/order-details.component";
import {RatingListComponent} from "./ratings/rating-list/rating-list.component";
import {RatingDetailsComponent} from "./ratings/rating-details/rating-details.component";
import {CreateRatingComponent} from "./ratings/create-rating/create-rating.component";
import {UpdateRatingComponent} from "./ratings/update-rating/update-rating.component";
import {CartDetailsComponent} from "./carts/cart-details/cart-details.component";
import {StorageDetailsComponent} from "./storage/storage-details/storage-details.component";
import {UpdateStorageComponent} from "./storage/update-storage/update-storage.component";

const routes: Routes = [
  { path: 'clients', component: ClientListComponent },
  { path: 'create-client', component: CreateClientComponent },
  { path: 'update-client/:id', component: UpdateClientComponent },
  { path: 'client-details/:id', component: ClientDetailsComponent },
  { path: 'books', component: BookListComponent },
  { path: 'book-details/:isbn', component: BookDetailsComponent },
  { path: 'create-book', component: CreateBookComponent },
  { path: 'update-book/:id', component: UpdateBookComponent },
  { path: 'carts', component: CartListComponent },
  { path: 'create-cart', component: CreateCartComponent },
  { path: 'update-cart/:id', component: UpdateCartComponent },
  { path: 'cart-details/:id', component: CartDetailsComponent },
  { path: 'storage', component: StorageListComponent },
  { path: 'create-storage', component: CreateStorageComponent },
  { path: 'update-storage/:id', component: UpdateStorageComponent },
  { path: 'ingest-storage/:isbn', component: IngestStorageComponent },
  { path: 'storage-details/:id', component: StorageDetailsComponent },
  { path: 'sell-storage/:isbn', component: SellStorageComponent },
  { path: 'orders', component: OrderListComponent },
  { path: 'create-order', component: CreateOrderComponent },
  { path: 'update-order/:id', component: UpdateOrderComponent },
  { path: 'order-details/:id', component: OrderDetailsComponent },
  { path: 'ratings', component: RatingListComponent },
  { path: 'create-rating', component: CreateRatingComponent },
  { path: 'update-rating/:id', component: UpdateRatingComponent },
  { path: 'rating-details/:id', component: RatingDetailsComponent },
  { path: '', redirectTo: 'clients', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
