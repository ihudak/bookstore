import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SellStorageComponent } from './sell-storage.component';

describe('SellStorageComponent', () => {
  let component: SellStorageComponent;
  let fixture: ComponentFixture<SellStorageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SellStorageComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SellStorageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
