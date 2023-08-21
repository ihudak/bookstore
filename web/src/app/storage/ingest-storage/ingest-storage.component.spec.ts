import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IngestStorageComponent } from './ingest-storage.component';

describe('IngestStorageComponent', () => {
  let component: IngestStorageComponent;
  let fixture: ComponentFixture<IngestStorageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ IngestStorageComponent ]
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
