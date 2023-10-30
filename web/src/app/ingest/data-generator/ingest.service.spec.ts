import { TestBed } from '@angular/core/testing';

import { IngestService } from './ingest.service';

describe('IngestService', () => {
  let service: IngestService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(IngestService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
