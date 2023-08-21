import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import {AppComponent} from './app.component';
import {ClientListComponent} from './clients/client-list/client-list.component';
import {HttpClientModule} from "@angular/common/http";
import {AppRoutingModule} from "./app-routing.module";
import {CreateClientComponent} from './clients/create-client/create-client.component';
import {FormsModule} from "@angular/forms";
import { UpdateClientComponent } from './clients/update-client/update-client.component';
import { ClientDetailsComponent } from './clients/client-details/client-details.component';
import { BookListComponent } from './books/book-list/book-list.component';
import { CreateBookComponent } from './books/create-book/create-book.component';
import { BookDetailsComponent } from './books/book-details/book-details.component';
import { UpdateBookComponent } from './books/update-book/update-book.component';
import { CartListComponent } from './carts/cart-list/cart-list.component';
import { CreateCartComponent } from './carts/create-cart/create-cart.component';
import { UpdateCartComponent } from './carts/update-cart/update-cart.component';
import { StorageListComponent } from './storage/storage-list/storage-list.component';
import { IngestStorageComponent } from './storage/ingest-storage/ingest-storage.component';
import { SellStorageComponent } from './storage/sell-storage/sell-storage.component';
import { CreateStorageComponent } from './storage/create-storage/create-storage.component';
import { OrderListComponent } from './orders/order-list/order-list.component';
import { OrderDetailsComponent } from './orders/order-details/order-details.component';
import { CreateOrderComponent } from './orders/create-order/create-order.component';
import { UpdateOrderComponent } from './orders/update-order/update-order.component';
import { RatingListComponent } from './ratings/rating-list/rating-list.component';
import { RatingDetailsComponent } from './ratings/rating-details/rating-details.component';
import { CreateRatingComponent } from './ratings/create-rating/create-rating.component';
import { UpdateRatingComponent } from './ratings/update-rating/update-rating.component';
import { CartDetailsComponent } from './carts/cart-details/cart-details.component';
import { StorageDetailsComponent } from './storage/storage-details/storage-details.component';
import { UpdateStorageComponent } from './storage/update-storage/update-storage.component';

@NgModule({
  declarations: [
    AppComponent,
    ClientListComponent,
    CreateClientComponent,
    UpdateClientComponent,
    ClientDetailsComponent,
    BookListComponent,
    CreateBookComponent,
    BookDetailsComponent,
    UpdateBookComponent,
    CartListComponent,
    CreateCartComponent,
    UpdateCartComponent,
    StorageListComponent,
    IngestStorageComponent,
    SellStorageComponent,
    CreateStorageComponent,
    OrderListComponent,
    OrderDetailsComponent,
    CreateOrderComponent,
    UpdateOrderComponent,
    RatingListComponent,
    RatingDetailsComponent,
    CreateRatingComponent,
    UpdateRatingComponent,
    CartDetailsComponent,
    StorageDetailsComponent,
    UpdateStorageComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
