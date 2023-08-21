import { Component } from '@angular/core';
import {Router} from "@angular/router";
import {Rating} from "../rating";
import {RatingService} from "../rating.service";

@Component({
  selector: 'app-create-rating',
  templateUrl: './create-rating.component.html',
  styleUrls: ['./create-rating.component.css']
})
export class CreateRatingComponent {
  rating: Rating = new Rating();
  constructor(private ratingService: RatingService, private router: Router) {
  }

  saveRating() {
    this.rating.createdAt = "";
    this.rating.updatedAt = "";
    this.ratingService.createRating(this.rating).subscribe(data => {
        console.log(data);
        this.goToRatingsList();
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToRatingsList() {
    this.router.navigate(['/ratings']);
  }

  onSubmit(){
    this.saveRating();
  }
}
