import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { BookListComponent } from './book-list.component';
import { environment } from '../../../environments/environment';

// an answer body of any shape, as the ratings service may send it
type FlushBody = Parameters<TestRequest['flush']>[0];

const STAR = '★';
const RATING_CELL = 6;

describe('BookListComponent', () => {
  const booksUrl = `${environment.booksSrvUrl}/api/v1/books`;
  const summariesUrl = `${environment.ratingsSrvUrl}/api/v1/ratings/summaries`;
  const summaryUrl = `${environment.ratingsSrvUrl}/api/v1/ratings/summary`;
  let fixture: ComponentFixture<BookListComponent>;
  let httpMock: HttpTestingController;

  const isBooks = (r: HttpRequest<unknown>) => r.method === 'GET' && r.url === booksUrl;
  const isSummaries = (r: HttpRequest<unknown>) => r.method === 'GET' && r.url === summariesUrl;
  const isSummary = (r: HttpRequest<unknown>) => r.method === 'GET' && r.url === summaryUrl;
  const book = (isbn: string, id = 1) => ({ id, isbn, title: `Title ${id}`, author: `Author ${id}`, price: 10 + id, language: 'en', published: false });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [BookListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // must run inside the fakeAsync body (the TestBed is zoneless with auto-detect)
  function open(): { books: TestRequest; summaries: TestRequest } {
    fixture = TestBed.createComponent(BookListComponent);
    fixture.detectChanges();
    tick();
    return { books: httpMock.expectOne(isBooks), summaries: httpMock.expectOne(isSummaries) };
  }
  function flush(req: TestRequest, status: number, statusText: string, body: unknown): void {
    req.flush(body as FlushBody, { status, statusText });
    fixture.detectChanges();
  }
  const ok = (req: TestRequest, body: unknown) => flush(req, 200, 'OK', body);
  const root = () => fixture.nativeElement as HTMLElement;
  const rows = () => Array.from(root().querySelectorAll('tbody tr'));
  const ratingCell = (row: Element) => row.querySelectorAll('td')[RATING_CELL].textContent;
  const ratingCells = () => rows().map(ratingCell);

  /** opens the list with the given books and summaries answered */
  function openWith(books: unknown[], summaries: unknown): void {
    const requests = open();
    ok(requests.books, books);
    ok(requests.summaries, summaries);
  }

  describe('the Rating column', () => {
    it('[U04][AC01] has the headings ISBN, Title, Author, Price, Lang, OnVend, Rating, Actions in that order', fakeAsync(() => {
      openWith([book('9780000000062')], []);
      const headings = Array.from(root().querySelectorAll('thead th')).map(th => th.textContent);
      expect(headings).toEqual(['ISBN', 'Title', 'Author', 'Price', 'Lang', 'OnVend', 'Rating', 'Actions']);
    }));

    it('[U04][AC01][TC01] a rated book shows its average on its row', fakeAsync(() => {
      openWith([book('9780000000062')], [{ isbn: '9780000000062', count: 4, average: 4.3 }]);
      expect(rows().length).toBe(1);
      expect(rows()[0].querySelectorAll('td')[0].textContent).toBe('9780000000062');
      expect(ratingCell(rows()[0])).toBe(`${STAR} 4.3`);
    }));

    it('[U04][AC01][TC02] a whole average shows one decimal', fakeAsync(() => {
      openWith([book('9780000000109')], [{ isbn: '9780000000109', count: 1, average: 5 }]);
      expect(ratingCells()).toEqual([`${STAR} 5.0`]);
    }));

    it('[U04][AC01][TC03] the list shows the average without a count', fakeAsync(() => {
      openWith([book('9780000000093')], [{ isbn: '9780000000093', count: 37, average: 4 }]);
      expect(ratingCells()).toEqual([`${STAR} 4.0`]);
      expect(ratingCells()[0]).not.toContain('37');
    }));

    it('[U04][AC02][TC01] a book the summaries do not list is not yet rated', fakeAsync(() => {
      openWith([book('9780000000123')], []);
      expect(ratingCells()).toEqual(['Not yet rated']);
    }));

    it('[U04][AC02][TC02] rated and unrated books side by side', fakeAsync(() => {
      openWith([book('9780000000062', 1), book('9780000000123', 2)], [{ isbn: '9780000000062', count: 4, average: 4.3 }]);
      expect(ratingCells()).toEqual([`${STAR} 4.3`, 'Not yet rated']);
    }));

    it('[U04][AC02][TC03] a book listed with a count of 0 is not yet rated, with no star', fakeAsync(() => {
      openWith([book('9780000000130')], [{ isbn: '9780000000130', count: 0, average: null }]);
      expect(ratingCells()).toEqual(['Not yet rated']);
    }));

    it('[U04][AC03][TC01] the row matches the book page for a rated book', fakeAsync(() => {
      openWith([book('9780000000062')], [{ isbn: '9780000000062', count: 4, average: 4.3 }]);
      // the book page shows "★ 4.3 (4 ratings)": the same average in the same format
      expect(ratingCells()[0]).toBe(`${STAR} 4.3`);
    }));

    it('[U04][AC03][TC02] the row matches the book page for an unrated book', fakeAsync(() => {
      openWith([book('9780000000123')], []);
      expect(ratingCells()[0]).toBe('Not yet rated');
    }));
  });

  describe('ratings that cannot be loaded', () => {
    const twoBooks = [book('9780000000062', 1), book('9780000000123', 2)];

    it('[U04][AC04][TC01] an unreachable ratings service shows "Rating unavailable" on every row, books as usual', fakeAsync(() => {
      const { books, summaries } = open();
      ok(books, twoBooks);
      summaries.error(new ProgressEvent('error'));
      fixture.detectChanges();
      expect(rows().length).toBe(2);
      expect(rows().map(r => r.querySelectorAll('td')[1].textContent)).toEqual(['Title 1', 'Title 2']);
      expect(ratingCells()).toEqual(['Rating unavailable', 'Rating unavailable']);
    }));

    it('[U04][AC04][TC02] a 500 is not shown as "Not yet rated"', fakeAsync(() => {
      const { books, summaries } = open();
      ok(books, [book('9780000000123')]);
      flush(summaries, 500, 'Server Error', null);
      expect(ratingCells()).toEqual(['Rating unavailable']);
    }));

    it('[U04][AC04][TC03] summaries that are not a list show "Rating unavailable"', fakeAsync(() => {
      openWith([book('9780000000062')], { isbn: '9780000000062', count: 4, average: 4.3 });
      expect(ratingCells()).toEqual(['Rating unavailable']);
    }));

    it('[U04][AC04] a 200 whose body fails parsing shows "Rating unavailable" on every row', fakeAsync(() => {
      const { books, summaries } = open();
      ok(books, twoBooks);
      summaries.error(new ProgressEvent('error'), { status: 200, statusText: 'OK' });
      fixture.detectChanges();
      expect(ratingCells()).toEqual(['Rating unavailable', 'Rating unavailable']);
    }));

    it('[U04][AC04][TC04] summaries never answered show "Rating unavailable" at 8 seconds', fakeAsync(() => {
      const { books, summaries } = open();
      ok(books, [book('9780000000062')]);
      tick(7999);
      fixture.detectChanges();
      expect(ratingCells()).toEqual(['']);
      tick(1);
      fixture.detectChanges();
      expect(summaries.cancelled).toBeTrue();
      expect(rows().length).toBe(1);
      expect(ratingCells()).toEqual(['Rating unavailable']);
    }));

    it('[U04][AC04][TC05] summaries answered after the bounded time change nothing', fakeAsync(() => {
      const { books, summaries } = open();
      ok(books, [book('9780000000062')]);
      tick(8000);
      fixture.detectChanges();
      expect(summaries.cancelled).toBeTrue();
      expect(ratingCells()).toEqual(['Rating unavailable']);
      tick(7000);
      fixture.detectChanges();
      expect(ratingCells()).toEqual(['Rating unavailable']);
    }));

    it('[U04][AC04][TC06] one unreadable entry makes both rows "Rating unavailable"', fakeAsync(() => {
      openWith(twoBooks, [
        { isbn: '9780000000062', count: 4, average: 4.3 },
        { isbn: '9780000000123', count: 2 }
      ]);
      expect(ratingCells()).toEqual(['Rating unavailable', 'Rating unavailable']);
      expect(root().textContent).not.toContain(STAR);
      expect(root().textContent).not.toContain('Not yet rated');
    }));
  });

  describe('while the ratings load', () => {
    it('[U04][AC05][TC01] the Rating cell is empty before the summaries answer', fakeAsync(() => {
      const { books, summaries } = open();
      ok(books, [book('9780000000062')]);
      tick(3000);
      fixture.detectChanges();
      expect(rows().length).toBe(1);
      expect(ratingCells()).toEqual(['']);
      expect(root().textContent).not.toContain(STAR);
      expect(root().textContent).not.toContain('Not yet rated');
      expect(root().textContent).not.toContain('Rating unavailable');
      ok(summaries, [{ isbn: '9780000000062', count: 4, average: 4.3 }]);
      expect(ratingCells()).toEqual([`${STAR} 4.3`]);
    }));

    it('[U04][AC05][TC02] an unrated book is not called unrated before the summaries answer', fakeAsync(() => {
      const { books, summaries } = open();
      ok(books, [book('9780000000123')]);
      tick(3000);
      fixture.detectChanges();
      expect(ratingCells()).toEqual(['']);
      ok(summaries, []);
      expect(ratingCells()).toEqual(['Not yet rated']);
    }));

    it('[U04][AC05] summaries answering before the books list show each row its line as it renders', fakeAsync(() => {
      const { books, summaries } = open();
      ok(summaries, [{ isbn: '9780000000062', count: 4, average: 4.3 }]);
      expect(rows().length).toBe(0);
      ok(books, [book('9780000000062', 1), book('9780000000123', 2)]);
      expect(ratingCells()).toEqual([`${STAR} 4.3`, 'Not yet rated']);
    }));
  });

  describe('one request per load', () => {
    it('[U04][AC06][TC01] three books take one summaries request and no per-book summary', fakeAsync(() => {
      openWith([book('9780000000062', 1), book('9780000000123', 2), book('9780000000130', 3)], [{ isbn: '9780000000062', count: 4, average: 4.3 }]);
      expect(httpMock.match(isSummaries)).toEqual([]);
      expect(httpMock.match(isSummary)).toEqual([]);
      expect(ratingCells()).toEqual([`${STAR} 4.3`, 'Not yet rated', 'Not yet rated']);
    }));

    it('[U04][AC06][TC02] a second opening asks again and shows the new answer', fakeAsync(() => {
      openWith([book('9780000000123')], []);
      expect(ratingCells()).toEqual(['Not yet rated']);
      fixture.destroy();

      openWith([book('9780000000123')], [{ isbn: '9780000000123', count: 1, average: 4 }]);
      expect(ratingCells()).toEqual([`${STAR} 4.0`]);
    }));

    it('[U04][AC06][TC03] fifty books still take one summaries request', fakeAsync(() => {
      const fifty = Array.from({ length: 50 }, (_, i) => book(String(9780000001000 + i), i + 1));
      const entries = fifty.map(b => ({ isbn: b.isbn, count: 2, average: 3.5 }));
      openWith(fifty, entries);
      expect(rows().length).toBe(50);
      expect(ratingCells().every(c => c === `${STAR} 3.5`)).toBeTrue();
      expect(httpMock.match(isSummaries)).toEqual([]);
      expect(httpMock.match(isSummary)).toEqual([]);
    }));

    it('[U04][AC06] a Vend click refetches the books only, sends no new summaries request, and keeps the lines', fakeAsync(() => {
      openWith([book('9780000000062', 1)], [{ isbn: '9780000000062', count: 4, average: 4.3 }]);
      const vend = Array.from(rows()[0].querySelectorAll('button')).find(b => b.textContent!.trim() === 'NotVended')!;
      vend.click();
      const post = httpMock.expectOne(r => r.method === 'POST' && r.url === `${booksUrl}/vend?isbn=9780000000062`);
      ok(post, {});
      // the refresh adds a row the screen-open answer does not list
      ok(httpMock.expectOne(isBooks), [{ ...book('9780000000062', 1), published: true }, book('9780000000123', 2)]);
      expect(httpMock.match(isSummaries)).toEqual([]);
      expect(ratingCells()).toEqual([`${STAR} 4.3`, 'Not yet rated']);
    }));

    it('[U04][AC04] a refresh adding a row after failed summaries shows "Rating unavailable", never blank', fakeAsync(() => {
      const { books, summaries } = open();
      ok(books, [book('9780000000062', 1)]);
      flush(summaries, 500, 'Server Error', null);
      Array.from(rows()[0].querySelectorAll('button')).find(b => b.textContent!.trim() === 'NotVended')!.click();
      ok(httpMock.expectOne(r => r.method === 'POST' && r.url === `${booksUrl}/vend?isbn=9780000000062`), {});
      ok(httpMock.expectOne(isBooks), [book('9780000000062', 1), book('9780000000123', 2)]);
      expect(ratingCells()).toEqual(['Rating unavailable', 'Rating unavailable']);
    }));
  });

  describe('leaving the list', () => {
    it('[U04][AC06] leaving before the summaries answer cancels them, writes nothing and logs no error', fakeAsync(() => {
      const logged = spyOn(console, 'error');
      const { books, summaries } = open();
      ok(books, [book('9780000000062')]);
      fixture.destroy();
      expect(summaries.cancelled).toBeTrue();
      tick(8000);
      expect(logged).not.toHaveBeenCalled();
    }));
  });
});
