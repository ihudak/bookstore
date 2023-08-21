import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateRatingComponent } from './create-rating.component';

describe('CreateRatingComponent', () => {
  let component: CreateRatingComponent;
  let fixture: ComponentFixture<CreateRatingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CreateRatingComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateRatingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
