import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import {ActivatedRoute, Router} from "@angular/router";
import {RatingService} from "../rating.service";
import {LoadOutcome, RatingToEdit} from "../rating-outcomes";
import {COMMENT_NOTE, KEEP_COMMENT_NOTE} from "../rating-texts";

@Component({
    selector: 'app-update-rating',
    templateUrl: './update-rating.component.html',
    styleUrls: ['./update-rating.component.css'],
    standalone: false
})
export class UpdateRatingComponent {
  load: LoadOutcome | undefined;
  stars: number | undefined;
  comment = '';
  sending = false;
  outcomeText = '';
  readonly commentNote = COMMENT_NOTE;
  readonly keepCommentNote = KEEP_COMMENT_NOTE;
  private destroyRef = inject(DestroyRef);

  constructor(private ratingService: RatingService, private route: ActivatedRoute, private router: Router) {
  }

  // the rating loaded for change, or undefined while loading or when it could not be loaded
  get loaded(): RatingToEdit | undefined {
    return this.load?.kind === 'loaded' ? this.load.rating : undefined;
  }

  // the text shown in place of the form when the rating is gone or could not be loaded
  get loadFailure(): string | undefined {
    return this.load !== undefined && this.load.kind !== 'loaded' ? this.load.text : undefined;
  }

  onSubmit() {
    const loaded = this.loaded;
    if (loaded === undefined || this.sending || this.stars === undefined) {
      return;
    }
    this.outcomeText = '';
    this.sending = true;
    this.ratingService.change(loaded, this.stars, this.comment)
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

  ngOnInit(): void {
    this.ratingService.loadRating(this.route.snapshot.params['id'])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(outcome => {
        this.load = outcome;
        if (outcome.kind === 'loaded') {
          this.stars = outcome.rating.stars;
        }
      });
  }

  goToRatingsList() {
    this.router.navigate(['/ratings']);
  }
}
