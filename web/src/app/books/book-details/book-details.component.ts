import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import {ActivatedRoute} from "@angular/router";
import {Book} from "../book";
import {BookService} from "../book.service";
import {RatingService} from "../../ratings/rating.service";
import {RatingLine} from "../../ratings/rating-outcomes";

@Component({
    selector: 'app-book-details',
    templateUrl: './book-details.component.html',
    styleUrls: ['./book-details.component.css'],
    standalone: false
})
export class BookDetailsComponent {
  isbn: string = "";
  book: Book = new Book();
  bookLoaded = false;
  ratingLine: RatingLine | undefined;
  private destroyRef = inject(DestroyRef);

  constructor(private route: ActivatedRoute, private bookService: BookService, private ratingService: RatingService) {  }

  ngOnInit(): void {
    this.isbn = this.route.snapshot.params['isbn'];
    this.book = new Book();
    this.bookService.getBookByIsbn(this.isbn).subscribe({
      next: data => {
        this.book = data;
        this.bookLoaded = true;
      },
      // a book that cannot be loaded shows no rating line
      error: () => {}
    });
    this.ratingService.bookLine(this.isbn).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(line => {
      this.ratingLine = line;
    });
  }
}
