import { Component, EventEmitter, Input, Output } from '@angular/core';

// the whole-star choice of both rating forms: five native radios, 1 to 5
@Component({
    selector: 'app-star-choice',
    templateUrl: './star-choice.component.html',
    standalone: false
})
export class StarChoiceComponent {
  @Input() stars: number | undefined;
  @Output() starsChange = new EventEmitter<number>();
  @Input({ required: true }) name!: string;   // the radio group's name, one per form
  readonly choices = [1, 2, 3, 4, 5];
}
