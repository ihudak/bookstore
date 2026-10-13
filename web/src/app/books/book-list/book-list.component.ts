import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import {Router} from "@angular/router";
import {Book} from "../book";
import {BookService} from "../book.service";
import {RatingService} from "../../ratings/rating.service";
import {RatingLine} from "../../ratings/rating-outcomes";

@Component({
    selector: 'app-book-list',
    templateUrl: './book-list.component.html',
    styleUrls: ['./book-list.component.css'],
    standalone: false
})
export class BookListComponent {
  books: Book[] = [];
  lineFor: ((isbn: string) => RatingLine) | undefined;
  private destroyRef = inject(DestroyRef);
  ngOnInit(): void {
    this.getBooks();
    // once per screen open; the in-place refreshes below reload the books only
    this.ratingService.catalogueLines().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(lineFor => {
      this.lineFor = lineFor;
    });
  }

  private getBooks() {
    this.bookService.getBooksList().subscribe(data => {
      this.books = data;
    });
  }

  updateBook(id: number) {
    this.router.navigate(['update-book', id]);
  }
  constructor(private bookService: BookService, private router: Router, private ratingService: RatingService) {
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
