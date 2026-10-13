import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import {ActivatedRoute} from "@angular/router";
import {RatingService} from "../rating.service";
import {Rating} from "../rating";

@Component({
    selector: 'app-rating-details',
    templateUrl: './rating-details.component.html',
    styleUrls: ['./rating-details.component.css'],
    standalone: false
})
export class RatingDetailsComponent {
  id: number = 0;
  rating: Rating = new Rating();
  private destroyRef = inject(DestroyRef);

  constructor(private route: ActivatedRoute, private ratingService: RatingService) {  }

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.rating = new Rating();
    this.ratingService.getRatingById(this.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(data => {
      this.rating = data;
    })
  }
}
