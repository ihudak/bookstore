import { Component } from '@angular/core';
import {Router} from "@angular/router";
import {Book} from "../book";
import {BookService} from "../book.service";

@Component({
  selector: 'app-book-list',
  templateUrl: './book-list.component.html',
  styleUrls: ['./book-list.component.css']
})
export class BookListComponent {
  books: Book[] = [];
  ngOnInit(): void {
    this.getBooks();
  }

  private getBooks() {
    this.bookService.getBooksList().subscribe(data => {
      this.books = data;
    });
  }

  updateBook(id: number) {
    this.router.navigate(['update-book', id]);
  }
  constructor(private bookService: BookService, private router: Router) {
  }

  deleteBook(id: number) {
    if (!confirm("Are you sure to delete book " + id)) { return;}
    this.bookService.deleteBook(id).subscribe(data => {
      console.log(data);
      this.getBooks();
    })
  }

  bookDetails(isbn: string) {
    this.router.navigate(['book-details', isbn]);
  }

  vendBook(book: Book) {
    this.bookService.vendBook(book).subscribe(data => {
      console.log(data);
      this.getBooks();
    });
  }

  deleteAllBooks() {
    if (!confirm("Are you sure to delete ALL books")) { return;}
    this.bookService.deleteAllBooks().subscribe(data => {
      console.log(data);
      this.getBooks();
    })
  }

  vendAllBooks() {
    if (!confirm("Are you sure to vend books")) { return;}
    this.bookService.vendAllBooks().subscribe(data => {
      console.log(data);
      this.getBooks();
    })
  }

  unvendAllBooks() {
    if (!confirm("Are you sure to unvend books")) { return;}
    this.bookService.unvendAllBooks().subscribe(data => {
      console.log(data);
      this.getBooks();
    })
  }
}
