import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { RatingDetailsComponent } from './rating-details.component';
import { environment } from '../../../environments/environment';
import type { TestRequest } from '@angular/common/http/testing';
// an answer body of any shape, as the ratings service may send it
type FlushBody = Parameters<TestRequest['flush']>[0];

describe('RatingDetailsComponent', () => {
  const ratingsUrl = `${environment.ratingsSrvUrl}/api/v1/ratings`;
  let routeParams: { [key: string]: string };
  let fixture: ComponentFixture<RatingDetailsComponent>;
  let httpMock: HttpTestingController;

  const isGet = (id: string) => (r: HttpRequest<unknown>) => r.method === 'GET' && r.url === `${ratingsUrl}/${id}`;

  beforeEach(async () => {
    routeParams = { id: '7' };
    await TestBed.configureTestingModule({
      declarations: [RatingDetailsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { params: routeParams } } }
      ]
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    expect(httpMock.match(r => r.method === 'DELETE')).toEqual([]);
    httpMock.verify();
  });

  // must run inside the fakeAsync body (the TestBed is zoneless with auto-detect)
  function open(id: string, body: unknown): void {
    routeParams['id'] = id;
    fixture = TestBed.createComponent(RatingDetailsComponent);
    fixture.detectChanges();
    tick();
    httpMock.expectOne(isGet(id)).flush(body as FlushBody, { status: 200, statusText: 'OK' });
    fixture.detectChanges();
  }
  const root = () => fixture.nativeElement as HTMLElement;
  const rowTexts = () => Array.from(root().querySelectorAll(':scope > div > div'))
    .map(d => (d.textContent ?? '').replace(/\s+/g, ' ').trim());

  it('[U05][AC05][TC01] shows ISBN, e-mail, stars, Updated At and Created At, and no comment', fakeAsync(() => {
    open('7', { id: 7, email: 'reader@example.com', isbn: '9780000000017', rating: 4, updatedAt: '2026-10-13T10:00:00', createdAt: '2026-10-12T09:00:00' });
    expect(rowTexts()).toEqual([
      'ISBN: 9780000000017',
      'e-mail: reader@example.com',
      'Rating: 4',
      'Updated At: 2026-10-13T10:00:00',
      'Created At: 2026-10-12T09:00:00'
    ]);
    expect(root().textContent).not.toContain('Comment');
  }));

  it('[U05][AC05][TC02] shows no comment label and not the comment a read carries', fakeAsync(() => {
    open('7', { id: 7, email: 'reader@example.com', isbn: '9780000000017', rating: 4, comment: 'Great plot', updatedAt: '2026-10-13T10:00:00', createdAt: '2026-10-12T09:00:00' });
    const text = root().textContent ?? '';
    expect(text).not.toContain('Great plot');
    expect(text).not.toContain('Comment');
    expect(rowTexts().length).toBe(5);
  }));

  it('[U05][AC05][TC03] a rating with no comment shows no comment label or value', fakeAsync(() => {
    open('8', { id: 8, email: 'second@example.com', isbn: '9780000000017', rating: 3, updatedAt: '2026-10-13T11:00:00', createdAt: '2026-10-12T09:00:00' });
    expect(rowTexts()).toEqual([
      'ISBN: 9780000000017',
      'e-mail: second@example.com',
      'Rating: 3',
      'Updated At: 2026-10-13T11:00:00',
      'Created At: 2026-10-12T09:00:00'
    ]);
    expect(root().textContent).not.toContain('Comment');
  }));

  it('[U06][AC02][TC01] reading the details sends no DELETE', fakeAsync(() => {
    open('7', { id: 7, email: 'reader@example.com', isbn: '9780000000017', rating: 4 });
    expect(root().querySelectorAll('button').length).toBe(0);
    // the afterEach hook asserts that no DELETE request was made
  }));

  it('[U05][AC05] leaving the screen before the read answers cancels the load', fakeAsync(() => {
    fixture = TestBed.createComponent(RatingDetailsComponent);
    fixture.detectChanges();
    tick();
    const req = httpMock.expectOne(isGet('7'));
    fixture.destroy();
    expect(req.cancelled).toBeTrue();
  }));
});
