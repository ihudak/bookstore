import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { Observable } from 'rxjs';

import { RatingService } from './rating.service';
import { LoadOutcome, RatingLine, RatingToEdit, WriteOutcome } from './rating-outcomes';
import { environment } from '../../environments/environment';

// an answer body of any shape, as the ratings service may send it
type FlushBody = Parameters<TestRequest['flush']>[0];

const BUYERS_ONLY = 'Only customers who have bought this book can rate it.';
const CANNOT_SAVE = "We can't save your rating right now. Please try again in a few minutes.";
const CANNOT_LOAD = "We can't load this rating right now. Please try again in a few minutes.";
const GONE_ON_CHANGE = 'This rating no longer exists. Nothing was saved.';
const GONE_ON_LOAD = 'This rating no longer exists.';

interface Probe<T> { values: T[]; completed: boolean; failed: boolean; }

function probe<T>(source: Observable<T>): Probe<T> {
  const p: Probe<T> = { values: [], completed: false, failed: false };
  source.subscribe({
    next: v => p.values.push(v),
    error: () => { p.failed = true; },
    complete: () => { p.completed = true; }
  });
  return p;
}

describe('RatingService', () => {
  const ratingsUrl = `${environment.ratingsSrvUrl}/api/v1/ratings`;
  const loaded: RatingToEdit = { id: 7, email: 'Reader@Example.com', isbn: '9780000000017', stars: 3 };
  let service: RatingService;
  let httpMock: HttpTestingController;

  const is = (method: string, url: string) => (r: HttpRequest<unknown>) => r.method === method && r.url === url;
  const cannotSave: WriteOutcome = { stored: false, kind: 'cannot-save', text: CANNOT_SAVE };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(RatingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    expect(httpMock.match(r => r.method === 'DELETE')).toEqual([]);
    httpMock.verify();
  });

  it('[U06][AC02] the removal and old write methods are gone', () => {
    const bag = service as unknown as Record<string, unknown>;
    expect(bag['deleteRating']).toBeUndefined();
    expect(bag['deleteAllRatings']).toBeUndefined();
    expect(bag['createRating']).toBeUndefined();
    expect(bag['updateRating']).toBeUndefined();
  });

  describe('give', () => {
    const entry = { email: ' Reader@Example.com', isbn: ' 9780000000017', rating: 4, comment: ' Great plot ' };

    it('[U01][AC03][TC01] posts exactly the entered e-mail, ISBN, stars and comment, untrimmed', () => {
      const p = probe(service.give(entry));
      const req = httpMock.expectOne(is('POST', ratingsUrl));
      expect(req.request.body).toEqual({ email: ' Reader@Example.com', isbn: ' 9780000000017', rating: 4, comment: ' Great plot ' });
      req.flush({ id: 1 }, { status: 200, statusText: 'OK' });
      expect(p.values).toEqual([{ stored: true }]);
      expect(p.completed).toBeTrue();
      expect(p.failed).toBeFalse();
    });

    it('[U01][AC03] sends only the four entered fields even if the entry carries more', () => {
      const wide = { ...entry, id: 99, createdAt: 'x', updatedAt: 'y' };
      probe(service.give(wide));
      const req = httpMock.expectOne(is('POST', ratingsUrl));
      expect(Object.keys(req.request.body as object).sort()).toEqual(['comment', 'email', 'isbn', 'rating']);
      req.flush({}, { status: 200, statusText: 'OK' });
    });

    const answers: [string, number, string, unknown, WriteOutcome][] = [
      ['[U01][AC05] 403', 403, 'Forbidden', null, { stored: false, kind: 'buyers-only', text: BUYERS_ONLY }],
      ['[U01][AC06] 400 with a reason', 400, 'Bad Request', { message: 'Invalid email address' }, { stored: false, kind: 'invalid', text: 'Invalid email address' }],
      ['[U01][AC07] 503', 503, 'Service Unavailable', { message: 'Service Unavailable' },
        { stored: false, kind: 'unavailable', text: "We can't check your purchase right now. Nothing was saved. Please try again in a few minutes." }],
      ['[U01][AC08] 500', 500, 'Server Error', null, cannotSave],
      ['[U01][AC08] 404', 404, 'Not Found', { message: 'Rating not found' }, cannotSave],
      ['[U01][AC08] 400 with an empty body', 400, 'Bad Request', '', cannotSave]
    ];
    for (const [label, status, statusText, body, expected] of answers) {
      it(`${label} gives its outcome`, () => {
        const p = probe(service.give(entry));
        httpMock.expectOne(is('POST', ratingsUrl)).flush(body as FlushBody, { status, statusText });
        expect(p.values).toEqual([expected]);
        expect(p.completed).toBeTrue();
      });
    }

    it('[U01][AC08][TC01] an unreachable service cannot be saved', () => {
      const p = probe(service.give(entry));
      httpMock.expectOne(is('POST', ratingsUrl)).error(new ProgressEvent('error'));
      expect(p.values).toEqual([cannotSave]);
      expect(p.completed).toBeTrue();
      expect(p.failed).toBeFalse();
    });

    it('[U01][AC04] a 200 whose body fails parsing is still stored', () => {
      const p = probe(service.give(entry));
      httpMock.expectOne(is('POST', ratingsUrl)).error(new ProgressEvent('error'), { status: 200, statusText: 'OK' });
      expect(p.values).toEqual([{ stored: true }]);
    });

    it('[U01][AC08][TC05] gives up exactly at 8 seconds and cancels the request', fakeAsync(() => {
      const p = probe(service.give(entry));
      const req = httpMock.expectOne(is('POST', ratingsUrl));
      tick(7999);
      expect(p.values).toEqual([]);
      expect(req.cancelled).toBeFalse();
      tick(1);
      expect(p.values).toEqual([cannotSave]);
      expect(p.completed).toBeTrue();
      expect(req.cancelled).toBeTrue();
    }));

    it('[U01][AC08] two subscriptions send two requests', () => {
      const first = probe(service.give(entry));
      const second = probe(service.give(entry));
      const reqs = httpMock.match(is('POST', ratingsUrl));
      expect(reqs.length).toBe(2);
      reqs[0].flush({}, { status: 200, statusText: 'OK' });
      reqs[1].flush(null, { status: 503, statusText: 'Service Unavailable' });
      expect(first.values).toEqual([{ stored: true }]);
      expect(second.values[0].stored).toBeFalse();
    });
  });

  describe('change', () => {
    it('[U02][AC03][TC01] puts the loaded id, e-mail and ISBN with the chosen stars and comment', () => {
      const p = probe(service.change(loaded, 5, 'Changed my mind'));
      const req = httpMock.expectOne(is('PUT', `${ratingsUrl}/7`));
      expect(req.request.body).toEqual({ id: 7, email: 'Reader@Example.com', isbn: '9780000000017', rating: 5, comment: 'Changed my mind' });
      expect(typeof (req.request.body as { id: unknown }).id).toBe('number');
      req.flush({ id: 7 }, { status: 200, statusText: 'OK' });
      expect(p.values).toEqual([{ stored: true }]);
      expect(p.completed).toBeTrue();
    });

    it('[U02][AC03][TC03] an empty comment is sent as empty', () => {
      probe(service.change(loaded, 4, ''));
      const req = httpMock.expectOne(is('PUT', `${ratingsUrl}/7`));
      expect((req.request.body as { comment: string }).comment).toBe('');
      req.flush({}, { status: 200, statusText: 'OK' });
    });

    const answers: [string, number, string, unknown, WriteOutcome][] = [
      ['[U02][AC05] 403', 403, 'Forbidden', '', { stored: false, kind: 'buyers-only', text: BUYERS_ONLY }],
      ['[U02][AC08] 404', 404, 'Not Found', { message: 'Rating not found' }, { stored: false, kind: 'gone', text: GONE_ON_CHANGE }],
      ['[U02][AC09] 500', 500, 'Server Error', null, cannotSave],
      ['[U02][AC06] 400 with a reason', 400, 'Bad Request', { message: 'Invalid rating. Must be a whole number from 1 to 5.' },
        { stored: false, kind: 'invalid', text: 'Invalid rating. Must be a whole number from 1 to 5.' }]
    ];
    for (const [label, status, statusText, body, expected] of answers) {
      it(`${label} gives its outcome`, () => {
        const p = probe(service.change(loaded, 4, ''));
        httpMock.expectOne(is('PUT', `${ratingsUrl}/7`)).flush(body as FlushBody, { status, statusText });
        expect(p.values).toEqual([expected]);
      });
    }

    it('[U02][AC09][TC01] an unreachable service cannot be saved', () => {
      const p = probe(service.change(loaded, 4, ''));
      httpMock.expectOne(is('PUT', `${ratingsUrl}/7`)).error(new ProgressEvent('error'));
      expect(p.values).toEqual([cannotSave]);
    });

    it('[U02][AC04] a 200 whose body fails parsing is still stored', () => {
      const p = probe(service.change(loaded, 4, ''));
      httpMock.expectOne(is('PUT', `${ratingsUrl}/7`)).error(new ProgressEvent('error'), { status: 200, statusText: 'OK' });
      expect(p.values).toEqual([{ stored: true }]);
    });

    it('[U02][AC09][TC04] gives up exactly at 8 seconds and cancels the request', fakeAsync(() => {
      const p = probe(service.change(loaded, 4, ''));
      const req = httpMock.expectOne(is('PUT', `${ratingsUrl}/7`));
      tick(7999);
      expect(p.values).toEqual([]);
      tick(1);
      expect(p.values).toEqual([cannotSave]);
      expect(p.completed).toBeTrue();
      expect(req.cancelled).toBeTrue();
    }));
  });

  describe('loadRating', () => {
    const body = { id: 7, email: 'reader@example.com', isbn: '9780000000017', rating: 3, comment: 'Great plot' };

    it('[U02][AC02] gets the rating by id and loads it without the comment', () => {
      const p = probe(service.loadRating('7'));
      const req = httpMock.expectOne(is('GET', `${ratingsUrl}/7`));
      req.flush(body as FlushBody, { status: 200, statusText: 'OK' });
      expect(p.values).toEqual([{ kind: 'loaded', rating: { id: 7, email: 'reader@example.com', isbn: '9780000000017', stars: 3 } }]);
      expect(p.completed).toBeTrue();
    });

    it('[U02][AC02] a numeric id is requested the same way', () => {
      probe(service.loadRating(7));
      httpMock.expectOne(is('GET', `${ratingsUrl}/7`)).flush(body as FlushBody, { status: 200, statusText: 'OK' });
    });

    it('[U02][AC13] 404 is gone', () => {
      const p = probe(service.loadRating('7'));
      httpMock.expectOne(is('GET', `${ratingsUrl}/7`)).flush(null, { status: 404, statusText: 'Not Found' });
      expect(p.values).toEqual([{ kind: 'gone', text: GONE_ON_LOAD }]);
    });

    it('[U02][AC14][TC02] 500 cannot be loaded', () => {
      const p = probe(service.loadRating('7'));
      httpMock.expectOne(is('GET', `${ratingsUrl}/7`)).flush(null, { status: 500, statusText: 'Server Error' });
      expect(p.values).toEqual([{ kind: 'cannot-load', text: CANNOT_LOAD }]);
    });

    it('[U02][AC14][TC03] a 200 with no body cannot be loaded', () => {
      const p = probe(service.loadRating('7'));
      httpMock.expectOne(is('GET', `${ratingsUrl}/7`)).flush(null, { status: 200, statusText: 'OK' });
      expect(p.values).toEqual([{ kind: 'cannot-load', text: CANNOT_LOAD }]);
    });

    it('[U02][AC14][TC01] an unreachable service cannot be loaded', () => {
      const p = probe(service.loadRating('7'));
      httpMock.expectOne(is('GET', `${ratingsUrl}/7`)).error(new ProgressEvent('error'));
      expect(p.values).toEqual([{ kind: 'cannot-load', text: CANNOT_LOAD }]);
    });

    it('[U02][AC14] a 200 whose body fails parsing cannot be loaded', () => {
      const p = probe(service.loadRating('7'));
      httpMock.expectOne(is('GET', `${ratingsUrl}/7`)).error(new ProgressEvent('error'), { status: 200, statusText: 'OK' });
      expect(p.values).toEqual([{ kind: 'cannot-load', text: CANNOT_LOAD }]);
    });

    it('[U02][AC14][TC04] gives up exactly at 8 seconds and cancels the request', fakeAsync(() => {
      const p = probe(service.loadRating('7'));
      const req = httpMock.expectOne(is('GET', `${ratingsUrl}/7`));
      tick(7999);
      expect(p.values).toEqual([]);
      tick(1);
      expect(p.values).toEqual([{ kind: 'cannot-load', text: CANNOT_LOAD }] as LoadOutcome[]);
      expect(p.completed).toBeTrue();
      expect(req.cancelled).toBeTrue();
    }));

    for (const id of ['abc', '7.5', ' ', '0x7', '1e2', '+7']) {
      it(`[U02][AC14] the id '${id}' cannot be loaded and sends no request`, () => {
        const p = probe(service.loadRating(id));
        expect(p.values).toEqual([{ kind: 'cannot-load', text: CANNOT_LOAD }]);
        expect(p.completed).toBeTrue();
        expect(httpMock.match(() => true)).toEqual([]);
      });
    }
  });

  describe('bookLine', () => {
    const summaryUrl = `${ratingsUrl}/summary`;
    const take = (): TestRequest => httpMock.expectOne(r => r.method === 'GET' && r.url === summaryUrl);

    it('[U03][AC06][TC01] gets the summary for the ISBN as a query parameter', () => {
      const p = probe(service.bookLine('9780000000062'));
      const req = take();
      expect(req.request.params.get('isbn')).toBe('9780000000062');
      req.flush({ isbn: '9780000000062', count: 4, average: 4.3 }, { status: 200, statusText: 'OK' });
      expect(p.values).toEqual([{ kind: 'rated', text: '★ 4.3 (4 ratings)' } as RatingLine]);
      expect(p.completed).toBeTrue();
    });

    it('[U03][AC03] a count of 0 is "Not yet rated"', () => {
      const p = probe(service.bookLine('9780000000123'));
      take().flush({ isbn: '9780000000123', count: 0, average: null }, { status: 200, statusText: 'OK' });
      expect(p.values).toEqual([{ kind: 'unrated', text: 'Not yet rated' }]);
    });

    const unavailable: [string, () => void][] = [
      ['a 500', () => take().flush(null, { status: 500, statusText: 'Server Error' })],
      ['a 204', () => take().flush(null, { status: 204, statusText: 'No Content' })],
      ['a 200 with a count and no average', () => take().flush({ isbn: 'x', count: 4 }, { status: 200, statusText: 'OK' })],
      ['an unreachable service', () => take().error(new ProgressEvent('error'))],
      ['a 200 whose body fails parsing', () => take().error(new ProgressEvent('error'), { status: 200, statusText: 'OK' })]
    ];
    for (const [label, answer] of unavailable) {
      it(`[U03][AC04] ${label} is "Rating unavailable"`, () => {
        const p = probe(service.bookLine('9780000000062'));
        answer();
        expect(p.values).toEqual([{ kind: 'unavailable', text: 'Rating unavailable' }]);
        expect(p.completed).toBeTrue();
      });
    }

    it('[U03][AC04][TC04] gives up exactly at 8 seconds and cancels the request', fakeAsync(() => {
      const p = probe(service.bookLine('9780000000062'));
      const req = take();
      tick(7999);
      expect(p.values).toEqual([]);
      tick(1);
      expect(p.values).toEqual([{ kind: 'unavailable', text: 'Rating unavailable' }]);
      expect(req.cancelled).toBeTrue();
    }));

    it('[U03][AC06][TC02] two subscriptions send two requests, nothing is reused', () => {
      const first = probe(service.bookLine('9780000000147'));
      const second = probe(service.bookLine('9780000000147'));
      const reqs = httpMock.match(r => r.method === 'GET' && r.url === summaryUrl);
      expect(reqs.length).toBe(2);
      reqs[0].flush({ isbn: '9780000000147', count: 1, average: 4 }, { status: 200, statusText: 'OK' });
      reqs[1].flush({ isbn: '9780000000147', count: 2, average: 3 }, { status: 200, statusText: 'OK' });
      expect(first.values[0].text).toBe('★ 4.0 (1 rating)');
      expect(second.values[0].text).toBe('★ 3.0 (2 ratings)');
    });
  });

  describe('catalogueLines', () => {
    const summariesUrl = `${ratingsUrl}/summaries`;
    const take = (): TestRequest => httpMock.expectOne(r => r.method === 'GET' && r.url === summariesUrl);

    it('[U04][AC06][TC01] sends one request and answers a line per ISBN', () => {
      const p = probe(service.catalogueLines());
      take().flush([
        { isbn: '9780000000062', count: 4, average: 4.3 },
        { isbn: '9780000000130', count: 0, average: null }
      ], { status: 200, statusText: 'OK' });
      expect(p.values.length).toBe(1);
      expect(p.completed).toBeTrue();
      const lineFor = p.values[0];
      expect(lineFor('9780000000062')).toEqual({ kind: 'rated', text: '★ 4.3' });
      expect(lineFor('9780000000130').text).toBe('Not yet rated');
      expect(lineFor('9780000000999').text).toBe('Not yet rated');
    });

    const unavailable: [string, () => void][] = [
      ['a 500', () => take().flush(null, { status: 500, statusText: 'Server Error' })],
      ['a 200 that is not a list', () => take().flush({ isbn: 'x' }, { status: 200, statusText: 'OK' })],
      ['a 200 with one unreadable entry', () => take().flush([
        { isbn: '9780000000062', count: 4, average: 4.3 },
        { isbn: '9780000000123', count: 2 }
      ], { status: 200, statusText: 'OK' })],
      ['an unreachable service', () => take().error(new ProgressEvent('error'))],
      ['a 200 whose body fails parsing', () => take().error(new ProgressEvent('error'), { status: 200, statusText: 'OK' })],
      ['a 204', () => take().flush(null, { status: 204, statusText: 'No Content' })]
    ];
    for (const [label, answer] of unavailable) {
      it(`[U04][AC04] ${label} is "Rating unavailable" on every row`, () => {
        const p = probe(service.catalogueLines());
        answer();
        const lineFor = p.values[0];
        expect(lineFor('9780000000062')).toEqual({ kind: 'unavailable', text: 'Rating unavailable' });
        expect(lineFor('9780000000123').text).toBe('Rating unavailable');
        expect(lineFor('9780000000999').text).toBe('Rating unavailable');
      });
    }

    it('[U04][AC04][TC04] gives up exactly at 8 seconds and cancels the request', fakeAsync(() => {
      const p = probe(service.catalogueLines());
      const req = take();
      tick(7999);
      expect(p.values.length).toBe(0);
      tick(1);
      expect(p.values.length).toBe(1);
      expect(p.values[0]('9780000000062').text).toBe('Rating unavailable');
      expect(req.cancelled).toBeTrue();
    }));

    it('[U04][AC06][TC02] two subscriptions send two requests, nothing is reused', () => {
      const first = probe(service.catalogueLines());
      const second = probe(service.catalogueLines());
      const reqs = httpMock.match(r => r.method === 'GET' && r.url === summariesUrl);
      expect(reqs.length).toBe(2);
      reqs[0].flush([], { status: 200, statusText: 'OK' });
      reqs[1].flush([{ isbn: '9780000000123', count: 1, average: 4 }], { status: 200, statusText: 'OK' });
      expect(first.values[0]('9780000000123').text).toBe('Not yet rated');
      expect(second.values[0]('9780000000123').text).toBe('★ 4.0');
    });
  });
});
