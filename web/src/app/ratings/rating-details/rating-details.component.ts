import { Component } from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {RatingService} from "../rating.service";
import {Rating} from "../rating";

@Component({
  selector: 'app-rating-details',
  templateUrl: './rating-details.component.html',
  styleUrls: ['./rating-details.component.css']
})
export class RatingDetailsComponent {
  id: number = 0;
  rating: Rating = new Rating();

  constructor(private route: ActivatedRoute, private ratingService: RatingService) {  }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.rating = new Rating();
    this.ratingService.getRatingById(this.id).subscribe(data => {
      this.rating = data;
    })
  }
}
