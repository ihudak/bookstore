import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SettingListComponent } from './setting-list.component';

describe('SettingListComponent', () => {
  let component: SettingListComponent;
  let fixture: ComponentFixture<SettingListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SettingListComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SettingListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
