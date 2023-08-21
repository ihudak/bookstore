import { Component } from '@angular/core';
import {Router} from "@angular/router";
import {Rating} from "../rating";
import {RatingService} from "../rating.service";

@Component({
  selector: 'app-rating-list',
  templateUrl: './rating-list.component.html',
  styleUrls: ['./rating-list.component.css']
})
export class RatingListComponent {
  ratings: Rating[] = [];
  ngOnInit(): void {
    this.getRatings();
  }

  constructor(private ratingService: RatingService, private router: Router) {
  }

  private getRatings() {
    this.ratingService.getRatingList().subscribe(data => {
      this.ratings = data;
    });
  }

  getRatingsByEmail(email: string) {
    this.ratingService.getRatingByEmail(email).subscribe(data => {
      this.ratings = data;
    });
  }

  getRatingsByISBN(isbn: string) {
    this.ratingService.getRatingByISBN(isbn).subscribe(data => {
      this.ratings = data;
    });
  }

  updateRating(id: number) {
    this.router.navigate(['update-rating', id]);
  }

  deleteRating(rating: Rating) {
    if (!confirm("Are you sure to delete rating for user " + rating.email + " and book " + rating.isbn)) { return;}
    this.ratingService.deleteRating(rating.id).subscribe(data => {
      console.log(data);
      this.getRatings();
    })
  }

  ratingDetails(id: number) {
    this.router.navigate(['rating-details', id]);
  }

  deleteAllRatings() {
    if (!confirm("Are you sure to delete ALL ratings")) { return;}
    this.ratingService.deleteAllRatings().subscribe(data => {
      console.log(data);
      this.getRatings();
    })
  }
}
