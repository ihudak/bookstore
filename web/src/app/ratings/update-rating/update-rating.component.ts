import { Component } from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {Rating} from "../rating";
import {RatingService} from "../rating.service";

@Component({
  selector: 'app-update-rating',
  templateUrl: './update-rating.component.html',
  styleUrls: ['./update-rating.component.css']
})
export class UpdateRatingComponent {
  id: number = 0;
  rating: Rating = new Rating();
  constructor(private ratingService: RatingService, private route: ActivatedRoute, private router: Router) {
  }

  onSubmit() {
    this.ratingService.updateRating(this.rating).subscribe(data => {
        console.log(data);
        this.goToCartsList();
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.ratingService.getRatingById(this.id).subscribe(data => {
        this.rating = data;
      },
      error => { console.log(error); alert(error.error.message); }
    );
  }

  goToCartsList() {
    this.router.navigate(['/ratings']);
  }
}
