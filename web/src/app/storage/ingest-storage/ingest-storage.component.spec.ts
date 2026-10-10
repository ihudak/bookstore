import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { provideRouter } from '@angular/router';

import { IngestStorageComponent } from './ingest-storage.component';

describe('IngestStorageComponent', () => {
  let component: IngestStorageComponent;
  let fixture: ComponentFixture<IngestStorageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ IngestStorageComponent ],
      imports: [ FormsModule ],
      providers: [ provideRouter([]) ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IngestStorageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
