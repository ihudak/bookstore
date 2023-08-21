import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UpdateStorageComponent } from './update-storage.component';

describe('UpdateStorageComponent', () => {
  let component: UpdateStorageComponent;
  let fixture: ComponentFixture<UpdateStorageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ UpdateStorageComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UpdateStorageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
