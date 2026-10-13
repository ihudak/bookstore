import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { RatingListComponent } from './rating-list.component';
import { environment } from '../../../environments/environment';

// an answer body of any shape, as the ratings service may send it
type FlushBody = Parameters<TestRequest['flush']>[0];

describe('RatingListComponent', () => {
  const ratingsUrl = `${environment.ratingsSrvUrl}/api/v1/ratings`;
  let fixture: ComponentFixture<RatingListComponent>;
  let httpMock: HttpTestingController;
  let navigate: jasmine.Spy;

  const isList = (r: HttpRequest<unknown>) => r.method === 'GET' && r.url === ratingsUrl;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [RatingListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
  });

  afterEach(() => {
    expect(httpMock.match(r => r.method === 'DELETE')).toEqual([]);
    httpMock.verify();
  });

  // must run inside the fakeAsync body (the TestBed is zoneless with auto-detect)
  function open(body: unknown): void {
    fixture = TestBed.createComponent(RatingListComponent);
    fixture.detectChanges();
    tick();
    httpMock.expectOne(isList).flush(body as FlushBody, { status: 200, statusText: 'OK' });
    fixture.detectChanges();
  }
  const root = () => fixture.nativeElement as HTMLElement;
  const rows = () => Array.from(root().querySelectorAll('tbody tr'));
  const cells = (row: Element) => Array.from(row.querySelectorAll('td')).map(td => (td.textContent ?? '').trim());
  const buttonsOf = (row: Element) => Array.from(row.querySelectorAll('button'));
  const buttonTexts = (scope: Element) => Array.from(scope.querySelectorAll('button')).map(b => (b.textContent ?? '').trim());

  const withComments = [
    { id: 7, email: 'reader@example.com', isbn: '9780000000017', rating: 4, comment: 'Great plot', createdAt: '2026-10-12T09:00:00', updatedAt: '2026-10-13T10:00:00' },
    { id: 8, email: 'second@example.com', isbn: '9780000000017', rating: 2, comment: 'Slow start', createdAt: '2026-10-12T09:00:00', updatedAt: '2026-10-13T11:00:00' },
    { id: 9, email: 'third@example.com', isbn: '9780000000024', rating: 5, comment: 'Gift for my mother', createdAt: '2026-10-12T09:00:00', updatedAt: '2026-10-13T12:00:00' }
  ];

  it('[U05][AC04][TC01] shows e-mail, ISBN, stars and the update time under the headings, with no comment column', fakeAsync(() => {
    open([withComments[0]]);
    const headings = Array.from(root().querySelectorAll('thead th')).map(th => th.textContent);
    expect(headings).toEqual(['email', 'ISBN', 'Ratings', 'LastUpdated', 'Actions']);
    expect(rows().length).toBe(1);
    expect(cells(rows()[0]).slice(0, 4)).toEqual(['reader@example.com', '9780000000017', '4', '2026-10-13T10:00:00']);
    expect(root().textContent).not.toContain('Comment');
  }));

  it('[U05][AC04][TC02] shows none of the three comments a read carries', fakeAsync(() => {
    open(withComments);
    expect(rows().length).toBe(3);
    const text = root().textContent ?? '';
    expect(text).not.toContain('Great plot');
    expect(text).not.toContain('Slow start');
    expect(text).not.toContain('Gift for my mother');
  }));

  it('[U05][AC04][TC03] a rating with no comment shows the same columns', fakeAsync(() => {
    open([{ id: 8, email: 'second@example.com', isbn: '9780000000017', rating: 3, updatedAt: '2026-10-13T11:00:00' }]);
    expect(cells(rows()[0]).slice(0, 4)).toEqual(['second@example.com', '9780000000017', '3', '2026-10-13T11:00:00']);
    expect(rows()[0].querySelectorAll('td').length).toBe(5);
  }));

  it('[U02][AC01][TC01] Update opens the update form for rating 7', fakeAsync(() => {
    open([withComments[0]]);
    const update = buttonsOf(rows()[0]).find(b => b.textContent!.trim() === 'Update')!;
    update.click();
    expect(navigate).toHaveBeenCalledOnceWith(['update-rating', 7]);
  }));

  it('[U02][AC01][TC02] each rating\'s Update opens its own rating', fakeAsync(() => {
    open([withComments[0], withComments[1]]);
    expect(cells(rows()[1])[0]).toBe('second@example.com');
    buttonsOf(rows()[1]).find(b => b.textContent!.trim() === 'Update')!.click();
    expect(navigate).toHaveBeenCalledOnceWith(['update-rating', 8]);
  }));

  it('[U06][AC02] Details opens the rating details of its own rating', fakeAsync(() => {
    open([withComments[0], withComments[1]]);
    buttonsOf(rows()[1]).find(b => b.textContent!.trim() === 'Details')!.click();
    expect(navigate).toHaveBeenCalledOnceWith(['rating-details', 8]);
  }));

  it('[U06][AC01][TC01] offers each rating only Update and Details', fakeAsync(() => {
    open([withComments[0]]);
    expect(buttonTexts(rows()[0])).toEqual(['Update', 'Details']);
    expect(root().textContent).not.toContain('Delete');
  }));

  it('[U06][AC01][TC02] offers the list only New Rating, no Delete All and no removal', fakeAsync(() => {
    open([...withComments, ...withComments].slice(0, 5).map((r, i) => ({ ...r, id: 20 + i })));
    expect(buttonTexts(root()).filter(t => t !== 'Update' && t !== 'Details')).toEqual([]);
    expect(buttonTexts(root()).length).toBe(10);
    const links = Array.from(root().querySelectorAll('a'));
    expect(links.map(a => (a.textContent ?? '').trim())).toEqual(['New Rating']);
    expect(root().textContent).not.toContain('Delete All');
  }));

  it('[U06][AC02][TC02] an empty Ratings list sends no removal and shows no row', fakeAsync(() => {
    open([]);
    expect(rows().length).toBe(0);
    expect(buttonTexts(root())).toEqual([]);
    // the afterEach hook asserts that no DELETE request was made
  }));

  it('[U02][AC01] leaving the list before it answers cancels the load', fakeAsync(() => {
    fixture = TestBed.createComponent(RatingListComponent);
    fixture.detectChanges();
    tick();
    const req: TestRequest = httpMock.expectOne(isList);
    fixture.destroy();
    expect(req.cancelled).toBeTrue();
  }));
});
