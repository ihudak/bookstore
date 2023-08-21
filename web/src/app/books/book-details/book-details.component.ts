import { Component } from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Book} from "../book";
import {BookService} from "../book.service";

@Component({
  selector: 'app-book-details',
  templateUrl: './book-details.component.html',
  styleUrls: ['./book-details.component.css']
})
export class BookDetailsComponent {
  isbn: string = "";
  book: Book = new Book();

  constructor(private route: ActivatedRoute, private bookService: BookService) {  }

  ngOnInit(): void {
    this.isbn = this.route.snapshot.params['isbn'];
    this.book = new Book();
    this.bookService.getBookByIsbn(this.isbn).subscribe(data => {
      this.book = data;
    })
  }
}
