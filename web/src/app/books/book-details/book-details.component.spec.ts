import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { BookDetailsComponent } from './book-details.component';
import { environment } from '../../../environments/environment';

// an answer body of any shape, as the ratings service may send it
type FlushBody = Parameters<TestRequest['flush']>[0];

const STAR = '★';

describe('BookDetailsComponent', () => {
  const booksUrl = `${environment.booksSrvUrl}/api/v1/books`;
  const summaryUrl = `${environment.ratingsSrvUrl}/api/v1/ratings/summary`;
  let routeParams: { [key: string]: string };
  let fixture: ComponentFixture<BookDetailsComponent>;
  let httpMock: HttpTestingController;

  const isBook = (isbn: string) => (r: HttpRequest<unknown>) => r.method === 'GET' && r.url === `${booksUrl}/find?isbn=${isbn}`;
  const isSummary = (r: HttpRequest<unknown>) => r.method === 'GET' && r.url === summaryUrl;
  const bookBody = (isbn: string) => ({ id: 1, isbn, title: 'Quiet Harbour', author: 'A. Writer', price: 12.5, language: 'en', published: true });

  beforeEach(async () => {
    routeParams = { isbn: '9780000000062' };
    await TestBed.configureTestingModule({
      declarations: [BookDetailsComponent],
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
    httpMock.verify();
  });

  // must run inside the fakeAsync body (the TestBed is zoneless with auto-detect)
  function load(isbn: string): { book: TestRequest; summary: TestRequest } {
    routeParams['isbn'] = isbn;
    fixture = TestBed.createComponent(BookDetailsComponent);
    fixture.detectChanges();
    tick();
    return { book: httpMock.expectOne(isBook(isbn)), summary: httpMock.expectOne(isSummary) };
  }
  function flush(req: TestRequest, status: number, statusText: string, body: unknown): void {
    req.flush(body as FlushBody, { status, statusText });
    fixture.detectChanges();
  }
  const root = () => fixture.nativeElement as HTMLElement;
  const ratingLine = () => root().querySelector('.rating-line') as HTMLElement | null;
  const text = () => root().textContent ?? '';

  /** a book answered at once and a summary answered with the given body */
  function loadWithSummary(isbn: string, body: unknown): void {
    const { book, summary } = load(isbn);
    flush(book, 200, 'OK', bookBody(isbn));
    flush(summary, 200, 'OK', body);
  }

  describe('the rating line', () => {
    it('[U03][AC01][TC01] shows an average of 4.3 below the book\'s other details', fakeAsync(() => {
      loadWithSummary('9780000000062', { isbn: '9780000000062', count: 4, average: 4.3 });
      expect(ratingLine()!.textContent).toBe(`${STAR} 4.3 (4 ratings)`);
      expect(ratingLine()!.previousElementSibling!.textContent).toContain('Vend:');
      expect(ratingLine()!.nextElementSibling).toBeNull();
    }));

    it('[U03][AC01][TC02] a whole average shows one decimal', fakeAsync(() => {
      loadWithSummary('9780000000079', { isbn: '9780000000079', count: 3, average: 4 });
      expect(ratingLine()!.textContent).toBe(`${STAR} 4.0 (3 ratings)`);
    }));

    it('[U03][AC02][TC01] thirty-seven ratings show their count', fakeAsync(() => {
      loadWithSummary('9780000000093', { isbn: '9780000000093', count: 37, average: 4 });
      expect(ratingLine()!.textContent).toBe(`${STAR} 4.0 (37 ratings)`);
    }));

    it('[U03][AC02][TC02] one rating is worded in the singular', fakeAsync(() => {
      loadWithSummary('9780000000109', { isbn: '9780000000109', count: 1, average: 5 });
      expect(ratingLine()!.textContent).toBe(`${STAR} 5.0 (1 rating)`);
    }));

    it('[U03][AC02][TC03] two ratings are worded in the plural', fakeAsync(() => {
      loadWithSummary('9780000000116', { isbn: '9780000000116', count: 2, average: 3.5 });
      expect(ratingLine()!.textContent).toBe(`${STAR} 3.5 (2 ratings)`);
    }));

    it('[U03][AC03][TC01] a book nobody has rated shows "Not yet rated"', fakeAsync(() => {
      loadWithSummary('9780000000123', { isbn: '9780000000123', count: 0, average: null });
      expect(ratingLine()!.textContent).toBe('Not yet rated');
    }));

    it('[U03][AC03][TC02] an unrated book shows no star and no zero count', fakeAsync(() => {
      loadWithSummary('9780000000130', { isbn: '9780000000130', count: 0, average: null });
      expect(text()).toContain('Not yet rated');
      expect(text()).not.toContain(STAR);
      expect(text()).not.toContain('(0 ratings)');
    }));

    it('[U04][AC03][TC01] the book half: 4 ratings of 4.3 show "★ 4.3"', fakeAsync(() => {
      loadWithSummary('9780000000062', { isbn: '9780000000062', count: 4, average: 4.3 });
      expect(ratingLine()!.textContent!.startsWith(`${STAR} 4.3`)).toBeTrue();
    }));

    it('[U04][AC03][TC02] the book half: a count of 0 shows "Not yet rated"', fakeAsync(() => {
      loadWithSummary('9780000000123', { isbn: '9780000000123', count: 0 });
      expect(ratingLine()!.textContent).toBe('Not yet rated');
    }));
  });

  describe('a rating that cannot be loaded', () => {
    function expectUnavailableWithBookDetails(): void {
      expect(ratingLine()!.textContent).toBe('Rating unavailable');
      expect(text()).not.toContain(STAR);
      expect(text()).not.toContain('Not yet rated');
      for (const shown of ['9780000000062', 'Quiet Harbour', 'A. Writer', '12.5', 'en', 'true']) {
        expect(text()).toContain(shown);
      }
    }

    it('[U03][AC04][TC01] an unreachable ratings service shows "Rating unavailable" with the book as usual', fakeAsync(() => {
      const { book, summary } = load('9780000000062');
      flush(book, 200, 'OK', bookBody('9780000000062'));
      summary.error(new ProgressEvent('error'));
      fixture.detectChanges();
      expectUnavailableWithBookDetails();
    }));

    it('[U03][AC04][TC02] a 500 is not shown as "Not yet rated"', fakeAsync(() => {
      const { book, summary } = load('9780000000123');
      flush(book, 200, 'OK', { ...bookBody('9780000000123') });
      flush(summary, 500, 'Server Error', null);
      expect(ratingLine()!.textContent).toBe('Rating unavailable');
      expect(text()).not.toContain('Not yet rated');
    }));

    it('[U03][AC04][TC03] a count and no average cannot be read: "Rating unavailable", no star', fakeAsync(() => {
      const { book, summary } = load('9780000000062');
      flush(book, 200, 'OK', bookBody('9780000000062'));
      flush(summary, 200, 'OK', { isbn: '9780000000062', count: 4 });
      expectUnavailableWithBookDetails();
    }));

    it('[U03][AC04] a 200 whose body fails parsing shows "Rating unavailable"', fakeAsync(() => {
      const { book, summary } = load('9780000000062');
      flush(book, 200, 'OK', bookBody('9780000000062'));
      summary.error(new ProgressEvent('error'), { status: 200, statusText: 'OK' });
      fixture.detectChanges();
      expectUnavailableWithBookDetails();
    }));

    it('[U03][AC04][TC04] a summary never answered shows "Rating unavailable" at 8 seconds', fakeAsync(() => {
      const { book, summary } = load('9780000000062');
      flush(book, 200, 'OK', bookBody('9780000000062'));
      tick(7999);
      fixture.detectChanges();
      expect(ratingLine()).toBeNull();
      tick(1);
      fixture.detectChanges();
      expect(summary.cancelled).toBeTrue();
      expectUnavailableWithBookDetails();
    }));

    it('[U03][AC04][TC05] a summary answered after the bounded time changes nothing', fakeAsync(() => {
      const { book, summary } = load('9780000000062');
      flush(book, 200, 'OK', bookBody('9780000000062'));
      tick(8000);
      fixture.detectChanges();
      expect(summary.cancelled).toBeTrue();
      expectUnavailableWithBookDetails();
      tick(7000);
      fixture.detectChanges();
      expectUnavailableWithBookDetails();
    }));
  });

  describe('while the rating loads', () => {
    it('[U03][AC05][TC01] nothing stands in for the rating line before the summary answers', fakeAsync(() => {
      const { book, summary } = load('9780000000062');
      flush(book, 200, 'OK', bookBody('9780000000062'));
      tick(3000);
      fixture.detectChanges();
      expect(text()).toContain('Quiet Harbour');
      expect(ratingLine()).toBeNull();
      expect(text()).not.toContain(STAR);
      expect(text()).not.toContain('Not yet rated');
      expect(text()).not.toContain('Rating unavailable');
      flush(summary, 200, 'OK', { isbn: '9780000000062', count: 4, average: 4.3 });
      expect(ratingLine()!.textContent).toBe(`${STAR} 4.3 (4 ratings)`);
    }));

    it('[U03][AC05][TC02] an unrated book is not called unrated before its summary answers', fakeAsync(() => {
      const { book, summary } = load('9780000000123');
      flush(book, 200, 'OK', bookBody('9780000000123'));
      tick(3000);
      fixture.detectChanges();
      expect(ratingLine()).toBeNull();
      flush(summary, 200, 'OK', { isbn: '9780000000123', count: 0, average: null });
      expect(ratingLine()!.textContent).toBe('Not yet rated');
    }));

    it('[U03][AC05] a summary answering before the book waits for the book', fakeAsync(() => {
      const { book, summary } = load('9780000000062');
      flush(summary, 200, 'OK', { isbn: '9780000000062', count: 4, average: 4.3 });
      expect(ratingLine()).toBeNull();
      flush(book, 200, 'OK', bookBody('9780000000062'));
      expect(ratingLine()!.textContent).toBe(`${STAR} 4.3 (4 ratings)`);
    }));

    it('[U03][AC04] a book that cannot be loaded shows no rating line, though the summary was asked for', fakeAsync(() => {
      const { book, summary } = load('9780000000062');
      flush(book, 500, 'Server Error', null);
      flush(summary, 200, 'OK', { isbn: '9780000000062', count: 4, average: 4.3 });
      expect(ratingLine()).toBeNull();
      expect(text()).not.toContain(STAR);
    }));
  });

  describe('every load asks again', () => {
    it('[U03][AC06][TC01] asks for the summary of the book\'s ISBN once', fakeAsync(() => {
      const { book, summary } = load('9780000000062');
      expect(summary.request.params.get('isbn')).toBe('9780000000062');
      flush(book, 200, 'OK', bookBody('9780000000062'));
      flush(summary, 200, 'OK', { isbn: '9780000000062', count: 4, average: 4.3 });
      expect(httpMock.match(isSummary)).toEqual([]);
    }));

    it('[U03][AC06][TC02] a second load asks again and shows the new answer', fakeAsync(() => {
      const first = load('9780000000147');
      flush(first.book, 200, 'OK', bookBody('9780000000147'));
      flush(first.summary, 200, 'OK', { isbn: '9780000000147', count: 1, average: 4 });
      expect(ratingLine()!.textContent).toBe(`${STAR} 4.0 (1 rating)`);
      fixture.destroy();

      const second = load('9780000000147');
      expect(second.summary.request.params.get('isbn')).toBe('9780000000147');
      flush(second.book, 200, 'OK', bookBody('9780000000147'));
      flush(second.summary, 200, 'OK', { isbn: '9780000000147', count: 2, average: 3 });
      expect(ratingLine()!.textContent).toBe(`${STAR} 3.0 (2 ratings)`);
    }));

    it('[U03][AC06][TC03] coming back to the book page asks again for that ISBN', fakeAsync(() => {
      const first = load('9780000000062');
      flush(first.book, 200, 'OK', bookBody('9780000000062'));
      flush(first.summary, 200, 'OK', { isbn: '9780000000062', count: 4, average: 4.3 });
      fixture.destroy();

      const back = load('9780000000062');
      expect(back.summary.request.params.get('isbn')).toBe('9780000000062');
      flush(back.book, 200, 'OK', bookBody('9780000000062'));
      flush(back.summary, 200, 'OK', { isbn: '9780000000062', count: 4, average: 4.3 });
      expect(ratingLine()!.textContent).toBe(`${STAR} 4.3 (4 ratings)`);
    }));
  });

  describe('leaving the page', () => {
    it('[U03][AC04] leaving before the summary answers cancels it, writes nothing and logs no error', fakeAsync(() => {
      const logged = spyOn(console, 'error');
      const { book, summary } = load('9780000000062');
      flush(book, 200, 'OK', bookBody('9780000000062'));
      fixture.destroy();
      expect(summary.cancelled).toBeTrue();
      tick(8000);
      expect(logged).not.toHaveBeenCalled();
    }));
  });
});
