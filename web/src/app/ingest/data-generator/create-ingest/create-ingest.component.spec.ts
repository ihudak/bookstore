import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateIngestComponent } from './create-ingest.component';

describe('IngestDetailsComponent', () => {
  let component: CreateIngestComponent;
  let fixture: ComponentFixture<CreateIngestComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CreateIngestComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateIngestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
