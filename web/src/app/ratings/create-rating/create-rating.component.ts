import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import {Router} from "@angular/router";
import {RatingService} from "../rating.service";
import {COMMENT_NOTE, KEEP_COMMENT_NOTE} from "../rating-texts";

@Component({
    selector: 'app-create-rating',
    templateUrl: './create-rating.component.html',
    styleUrls: ['./create-rating.component.css'],
    standalone: false
})
export class CreateRatingComponent {
  email = '';
  isbn = '';
  stars: number | undefined;
  comment = '';
  sending = false;
  outcomeText = '';
  readonly commentNote = COMMENT_NOTE;
  readonly keepCommentNote = KEEP_COMMENT_NOTE;
  private destroyRef = inject(DestroyRef);

  constructor(private ratingService: RatingService, private router: Router) {
  }

  // an e-mail and an ISBN each holding a character other than a space, a star chosen, nothing in flight
  get canSubmit(): boolean {
    return /[^ ]/.test(this.email) && /[^ ]/.test(this.isbn) && this.stars !== undefined && !this.sending;
  }

  goToRatingsList() {
    this.router.navigate(['/ratings']);
  }

  onSubmit(){
    if (!this.canSubmit || this.stars === undefined) {
      return;
    }
    this.outcomeText = '';
    this.sending = true;
    this.ratingService.give({email: this.email, isbn: this.isbn, rating: this.stars, comment: this.comment})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(outcome => {
        this.sending = false;
        if (outcome.stored) {
          this.goToRatingsList();
        } else {
          this.outcomeText = outcome.text;
        }
      });
  }
}
