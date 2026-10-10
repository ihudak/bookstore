import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { provideRouter } from '@angular/router';

import { UpdateConfigComponent } from './update-config.component';

describe('UpdateConfigComponent', () => {
  let component: UpdateConfigComponent;
  let fixture: ComponentFixture<UpdateConfigComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ UpdateConfigComponent ],
      imports: [ FormsModule ],
      providers: [ provideRouter([]) ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UpdateConfigComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
