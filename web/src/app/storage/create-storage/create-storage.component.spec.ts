import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateStorageComponent } from './create-storage.component';

describe('CreateStorageComponent', () => {
  let component: CreateStorageComponent;
  let fixture: ComponentFixture<CreateStorageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CreateStorageComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateStorageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
