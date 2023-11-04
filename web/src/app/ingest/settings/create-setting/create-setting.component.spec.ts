import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateSettingComponent } from './create-setting.component';

describe('CreateSettingComponent', () => {
  let component: CreateSettingComponent;
  let fixture: ComponentFixture<CreateSettingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CreateSettingComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateSettingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
