import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StorageDetailsComponent } from './storage-details.component';

describe('StorageDetailsComponent', () => {
  let component: StorageDetailsComponent;
  let fixture: ComponentFixture<StorageDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ StorageDetailsComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StorageDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
