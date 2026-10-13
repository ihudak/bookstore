import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import {Router} from "@angular/router";
import {Rating} from "../rating";
import {RatingService} from "../rating.service";

@Component({
    selector: 'app-rating-list',
    templateUrl: './rating-list.component.html',
    styleUrls: ['./rating-list.component.css'],
    standalone: false
})
export class RatingListComponent {
  ratings: Rating[] = [];
  private destroyRef = inject(DestroyRef);
  ngOnInit(): void {
    this.getRatings();
  }

  constructor(private ratingService: RatingService, private router: Router) {
  }

  private getRatings() {
    this.ratingService.getRatingList().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(data => {
      this.ratings = data;
    });
  }

  getRatingsByEmail(email: string) {
    this.ratingService.getRatingByEmail(email).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(data => {
      this.ratings = data;
    });
  }

  getRatingsByISBN(isbn: string) {
    this.ratingService.getRatingByISBN(isbn).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(data => {
      this.ratings = data;
    });
  }

  updateRating(id: number) {
    this.router.navigate(['update-rating', id]);
  }

  ratingDetails(id: number) {
    this.router.navigate(['rating-details', id]);
  }
}
