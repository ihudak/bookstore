import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { Router, provideRouter } from '@angular/router';

import { CreateRatingComponent } from './create-rating.component';
import { StarChoiceComponent } from '../star-choice/star-choice.component';
import { environment } from '../../../environments/environment';

// an answer body of any shape, as the ratings service may send it
type FlushBody = Parameters<TestRequest['flush']>[0];

const BUYERS_ONLY = 'Only customers who have bought this book can rate it.';
const UNAVAILABLE = "We can't check your purchase right now. Nothing was saved. Please try again in a few minutes.";
const CANNOT_SAVE = "We can't save your rating right now. Please try again in a few minutes.";
const GONE = 'This rating no longer exists. Nothing was saved.';
const COMMENT_NOTE = 'Only Quillmark sees your comment. It is not shown to other shoppers.';
const KEEP_NOTE = 'Leave this empty to keep any comment you gave before.';

describe('CreateRatingComponent', () => {
  const ratingsUrl = `${environment.ratingsSrvUrl}/api/v1/ratings`;
  let fixture: ComponentFixture<CreateRatingComponent>;
  let component: CreateRatingComponent;
  let httpMock: HttpTestingController;
  let navigate: jasmine.Spy;

  const isPost = (r: HttpRequest<unknown>) => r.method === 'POST' && r.url === ratingsUrl;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CreateRatingComponent, StarChoiceComponent],
      imports: [FormsModule],
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
  function open(): void {
    fixture = TestBed.createComponent(CreateRatingComponent);
    component = fixture.componentInstance;
    settle();
  }
  function settle(): void {
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }
  const root = () => fixture.nativeElement as HTMLElement;
  const field = (id: string) => root().querySelector(`#${id}`) as HTMLInputElement;
  const radios = () => Array.from(root().querySelectorAll<HTMLInputElement>('input[type="radio"]'));
  const submitButton = () => root().querySelector('button[type="submit"]') as HTMLButtonElement;
  const alertEl = () => root().querySelector('[role="alert"]') as HTMLElement;
  const alertText = () => alertEl().textContent;
  const checkedStars = () => radios().filter(r => r.checked).map(r => r.value);

  function type(id: string, value: string): void {
    const input = field(id);
    input.value = value;
    input.dispatchEvent(new Event('input'));
    settle();
  }
  function choose(stars: number): void {
    (root().querySelector(`#give-stars-${stars}`) as HTMLInputElement).click();
    settle();
  }
  function fill(email: string, isbn: string, stars?: number, comment?: string): void {
    type('email', email);
    type('isbn', isbn);
    if (stars !== undefined) {
      choose(stars);
    }
    if (comment !== undefined) {
      type('comment', comment);
    }
  }
  function submit(): void {
    submitButton().click();
    settle();
  }
  function takePost(): TestRequest {
    return httpMock.expectOne(isPost);
  }
  function answer(req: TestRequest, status: number, statusText: string, body: unknown): void {
    req.flush(body as FlushBody, { status, statusText });
    settle();
  }
  function expectNoPost(): void {
    expect(httpMock.match(isPost)).toEqual([]);
  }
  function expectEntries(email: string, isbn: string, stars: number, comment: string): void {
    expect(field('email').value).toBe(email);
    expect(field('isbn').value).toBe(isbn);
    expect(checkedStars()).toEqual([String(stars)]);
    expect(field('comment').value).toBe(comment);
  }

  describe('the stars', () => {
    it('[U01][AC01][TC01] opens with exactly the five whole stars 1 to 5, none chosen', fakeAsync(() => {
      open();
      expect(radios().map(r => r.value)).toEqual(['1', '2', '3', '4', '5']);
      expect(checkedStars()).toEqual([]);
    }));

    it('[U01][AC01][TC02] offers no way to enter 0, 6, -1 or 3.5', fakeAsync(() => {
      open();
      expect(root().querySelectorAll('input[type="number"]').length).toBe(0);
      expect(root().querySelector('#rating')).toBeNull();
      expect(radios().length).toBe(5);
    }));

    it('[U01][AC13][TC01] the stars are native radios in one fieldset with a legend', fakeAsync(() => {
      open();
      const fieldset = root().querySelector('fieldset')!;
      expect(fieldset.querySelectorAll('input[type="radio"]').length).toBe(5);
      expect(fieldset.querySelector('legend')!.textContent!.trim()).toBe('Rating');
      expect(new Set(radios().map(r => r.name))).toEqual(new Set(['give-stars']));
    }));

    it('[U01][AC13][TC01] choosing a radio gives that many stars in the give', fakeAsync(() => {
      open();
      fill('reader@example.com', '9780000000017', 4);
      expect(checkedStars()).toEqual(['4']);
      submit();
      const req = takePost();
      expect((req.request.body as { rating: number }).rating).toBe(4);
      answer(req, 200, 'OK', {});
    }));

    it('[U01][AC13][TC02] the radios reach 1 and 5 and offer nothing beyond', fakeAsync(() => {
      open();
      choose(1);
      expect(checkedStars()).toEqual(['1']);
      choose(5);
      expect(checkedStars()).toEqual(['5']);
      expect(radios().map(r => r.value)).toEqual(['1', '2', '3', '4', '5']);
    }));
  });

  describe('the gate', () => {
    it('[U01][AC02][TC01] a form just opened has Submit disabled', fakeAsync(() => {
      open();
      expect(submitButton().disabled).toBeTrue();
    }));

    it('[U01][AC02][TC02] with no star chosen Submit is disabled and nothing is sent', fakeAsync(() => {
      open();
      fill('reader@example.com', '9780000000017');
      expect(submitButton().disabled).toBeTrue();
      submit();
      component.onSubmit();
      settle();
      expectNoPost();
    }));

    it('[U01][AC02][TC03] with the e-mail or the ISBN empty Submit is disabled and nothing is sent', fakeAsync(() => {
      open();
      fill('', '9780000000017', 4);
      expect(submitButton().disabled).toBeTrue();
      submit();
      component.onSubmit();
      type('email', 'reader@example.com');
      type('isbn', '');
      expect(submitButton().disabled).toBeTrue();
      submit();
      component.onSubmit();
      settle();
      expectNoPost();
    }));

    it('[U01][AC02][TC04] a field of only spaces counts as empty', fakeAsync(() => {
      open();
      fill('   ', '9780000000017', 4);
      expect(submitButton().disabled).toBeTrue();
      component.onSubmit();
      type('email', 'reader@example.com');
      type('isbn', '   ');
      expect(submitButton().disabled).toBeTrue();
      component.onSubmit();
      settle();
      expectNoPost();
    }));

    it('[U01][AC14][TC01] a complete form enables Submit and sends one POST', fakeAsync(() => {
      open();
      fill('reader@example.com', '9780000000017');
      expect(submitButton().disabled).toBeTrue();
      choose(4);
      expect(submitButton().disabled).toBeFalse();
      submit();
      answer(takePost(), 200, 'OK', { id: 1 });
      expectNoPost();
    }));

    it('[U01][AC14][TC02] Submit is enabled again after an outcome', fakeAsync(() => {
      open();
      fill('reader5@example.com', '9780000000017', 4);
      submit();
      answer(takePost(), 503, 'Service Unavailable', null);
      expect(alertText()).toBe(UNAVAILABLE);
      expect(submitButton().disabled).toBeFalse();
    }));

    it('[U01][AC14] an e-mail or ISBN holding only a tab or a non-breaking space enables Submit and the reason is shown verbatim', fakeAsync(() => {
      open();
      fill('\t', '\u00a0', 2);
      expect(submitButton().disabled).toBeFalse();
      submit();
      const req = takePost();
      expect(req.request.body).toEqual({ email: '\t', isbn: '\u00a0', rating: 2, comment: '' });
      answer(req, 400, 'Bad Request', { message: 'Invalid email address' });
      expect(alertText()).toBe('Invalid email address');
    }));

    it('[U01][AC02] a submit event on the form with the gate closed sends no POST', fakeAsync(() => {
      open();
      fill('reader@example.com', '9780000000017');
      root().querySelector('form')!.dispatchEvent(new Event('submit'));
      settle();
      expectNoPost();
      expect(alertText()).toBe('');
    }));
  });

  describe('the give', () => {
    it('[U01][AC03][TC01] carries what was entered', fakeAsync(() => {
      open();
      fill('reader@example.com', '9780000000017', 4, 'Great plot');
      submit();
      const req = takePost();
      expect(req.request.body).toEqual({ email: 'reader@example.com', isbn: '9780000000017', rating: 4, comment: 'Great plot' });
      answer(req, 200, 'OK', { id: 1 });
    }));

    it('[U01][AC03][TC02] sends the e-mail and the ISBN exactly as typed', fakeAsync(() => {
      open();
      fill('Reader@Example.com', ' 9780000000017', 1);
      submit();
      const req = takePost();
      expect(req.request.body).toEqual({ email: 'Reader@Example.com', isbn: ' 9780000000017', rating: 1, comment: '' });
      answer(req, 400, 'Bad Request', { message: 'ISBN must be a 13-digits value' });
    }));

    it('[U01][AC03][TC03] a give with no comment carries an empty or absent comment', fakeAsync(() => {
      open();
      fill('reader@example.com', '9780000000017', 5);
      submit();
      const req = takePost();
      const body = req.request.body as { rating: number; comment?: string };
      expect(body.rating).toBe(5);
      expect(body.comment ?? '').toBe('');
      answer(req, 200, 'OK', {});
    }));

    it('[U01][AC03] sends no other field than the four entered ones', fakeAsync(() => {
      open();
      fill('reader@example.com', '9780000000017', 3, 'x');
      submit();
      const req = takePost();
      expect(Object.keys(req.request.body as object).sort()).toEqual(['comment', 'email', 'isbn', 'rating']);
      answer(req, 200, 'OK', {});
    }));

    it('[U01][AC04][TC01] a stored give opens the Ratings list', fakeAsync(() => {
      open();
      fill('reader@example.com', '9780000000017', 4);
      submit();
      answer(takePost(), 200, 'OK', { id: 1 });
      expect(navigate).toHaveBeenCalledOnceWith(['/ratings']);
    }));

    it('[U01][AC04][TC02] a refused give does not open the Ratings list', fakeAsync(() => {
      open();
      fill('browser@example.com', '9780000000017', 4);
      submit();
      answer(takePost(), 403, 'Forbidden', null);
      expect(navigate).not.toHaveBeenCalled();
    }));

    it('[U01][AC04] a 200 whose body fails parsing is still stored and opens the Ratings list', fakeAsync(() => {
      open();
      fill('reader@example.com', '9780000000017', 4);
      submit();
      takePost().error(new ProgressEvent('error'), { status: 200, statusText: 'OK' });
      settle();
      expect(navigate).toHaveBeenCalledOnceWith(['/ratings']);
      expect(alertText()).toBe('');
    }));
  });

  describe('the outcome texts', () => {
    function give(status: number, statusText: string, body: unknown): void {
      open();
      fill('browser@example.com', '9780000000017', 3);
      submit();
      answer(takePost(), status, statusText, body);
    }

    it('[U01][AC05][TC01] 403 shows the buyers-only rule', fakeAsync(() => {
      give(403, 'Forbidden', { message: BUYERS_ONLY });
      expect(alertText()).toBe(BUYERS_ONLY);
    }));

    it('[U01][AC05][TC02] 403 with an empty body shows the same rule', fakeAsync(() => {
      give(403, 'Forbidden', '');
      expect(alertText()).toBe(BUYERS_ONLY);
    }));

    it('[U01][AC05] 403 with another message still shows the rule, not the message', fakeAsync(() => {
      give(403, 'Forbidden', { message: 'Access denied' });
      expect(alertText()).toBe(BUYERS_ONLY);
    }));

    it('[U01][AC06][TC01] 400 shows a malformed e-mail\'s reason', fakeAsync(() => {
      give(400, 'Bad Request', { message: 'Invalid email address' });
      expect(alertText()).toBe('Invalid email address');
    }));

    it('[U01][AC06][TC02] 400 shows a short ISBN\'s reason, not the buyers-only text', fakeAsync(() => {
      give(400, 'Bad Request', { message: 'ISBN must be a 13-digits value' });
      expect(alertText()).toBe('ISBN must be a 13-digits value');
      expect(alertText()).not.toContain('bought');
    }));

    it('[U01][AC06][TC03] 400 shows a reason the form has no text of its own for', fakeAsync(() => {
      give(400, 'Bad Request', { message: 'Invalid rating. Must be a whole number from 1 to 5.' });
      expect(alertText()).toBe('Invalid rating. Must be a whole number from 1 to 5.');
    }));

    it('[U01][AC06][TC04] a reason holding markup is shown as plain text', fakeAsync(() => {
      const markup = '<B>Invalid</B><img src=x onerror=alert(1)>';
      give(400, 'Bad Request', { message: markup });
      expect(alertText()).toBe(markup);
      expect(root().querySelector('img')).toBeNull();
      expect(root().querySelector('b')).toBeNull();
      expect(alertEl().children.length).toBe(0);
    }));

    it('[U01][AC07][TC01] 503 says the purchase check is unavailable', fakeAsync(() => {
      give(503, 'Service Unavailable', null);
      expect(alertText()).toBe(UNAVAILABLE);
    }));

    it('[U01][AC07][TC02] 503 shows the text, not the body\'s own words', fakeAsync(() => {
      give(503, 'Service Unavailable', { message: 'Service Unavailable' });
      expect(alertText()).toBe(UNAVAILABLE);
      expect(alertText()).not.toContain('Service Unavailable');
    }));

    it('[U01][AC08][TC01] an unreachable service cannot be saved', fakeAsync(() => {
      open();
      fill('reader5@example.com', '9780000000017', 4);
      submit();
      takePost().error(new ProgressEvent('error'));
      settle();
      expect(alertText()).toBe(CANNOT_SAVE);
    }));

    it('[U01][AC08][TC02] 500 is not shown as a refusal', fakeAsync(() => {
      give(500, 'Server Error', null);
      expect(alertText()).toBe(CANNOT_SAVE);
    }));

    it('[U01][AC08][TC03] 400 with an empty body cannot be saved, with no empty or "undefined" text', fakeAsync(() => {
      give(400, 'Bad Request', '');
      expect(alertText()).toBe(CANNOT_SAVE);
      expect(alertText()).not.toContain('undefined');
    }));

    it('[U01][AC08][TC03] 400 with a blank reason cannot be saved', fakeAsync(() => {
      give(400, 'Bad Request', { message: '   ' });
      expect(alertText()).toBe(CANNOT_SAVE);
    }));

    it('[U01][AC08][TC04] 404 to a give is not "no longer exists"', fakeAsync(() => {
      give(404, 'Not Found', null);
      expect(alertText()).toBe(CANNOT_SAVE);
      expect(alertText()).not.toBe(GONE);
    }));
  });

  describe('the bounded time', () => {
    it('[U01][AC08][TC05] a give that is never answered shows cannot-save at 8 seconds', fakeAsync(() => {
      open();
      fill('reader5@example.com', '9780000000017', 4);
      submit();
      const req = takePost();
      tick(7999);
      fixture.detectChanges();
      expect(alertText()).toBe('');
      expect(submitButton().disabled).toBeTrue();
      tick(1);
      fixture.detectChanges();
      expect(req.cancelled).toBeTrue();
      expect(alertText()).toBe(CANNOT_SAVE);
      expect(submitButton().disabled).toBeFalse();
    }));

    it('[U01][AC08][TC06] a late 200 reaches nothing: the text stays and the list is not opened', fakeAsync(() => {
      open();
      fill('reader5@example.com', '9780000000017', 4);
      submit();
      const req = takePost();
      tick(8000);
      fixture.detectChanges();
      expect(req.cancelled).toBeTrue();
      expect(alertText()).toBe(CANNOT_SAVE);
      tick(7000);
      fixture.detectChanges();
      expect(alertText()).toBe(CANNOT_SAVE);
      expect(navigate).not.toHaveBeenCalled();
    }));
  });

  describe('the entries are kept', () => {
    it('[U01][AC09][TC01] a buyers-only refusal keeps the entries', fakeAsync(() => {
      open();
      fill('browser@example.com', '9780000000017', 3, 'Lovely cover');
      submit();
      answer(takePost(), 403, 'Forbidden', null);
      expectEntries('browser@example.com', '9780000000017', 3, 'Lovely cover');
      expect(navigate).not.toHaveBeenCalled();
    }));

    it('[U01][AC09][TC02] an unavailable refusal keeps the entries', fakeAsync(() => {
      open();
      fill('reader5@example.com', '9780000000017', 5, 'Loved it');
      submit();
      answer(takePost(), 503, 'Service Unavailable', null);
      expectEntries('reader5@example.com', '9780000000017', 5, 'Loved it');
      expect(navigate).not.toHaveBeenCalled();
    }));

    it('[U01][AC09][TC03] an unreachable service keeps the entries', fakeAsync(() => {
      open();
      fill('reader5@example.com', '9780000000017', 2, 'On reflection');
      submit();
      takePost().error(new ProgressEvent('error'));
      settle();
      expectEntries('reader5@example.com', '9780000000017', 2, 'On reflection');
    }));

    it('[U01][AC09][TC04] an invalid give keeps the entries', fakeAsync(() => {
      open();
      fill('reader-at-example', '9780000000017', 1, 'Typo');
      submit();
      answer(takePost(), 400, 'Bad Request', { message: 'Invalid email address' });
      expectEntries('reader-at-example', '9780000000017', 1, 'Typo');
    }));

    it('[U01][AC09] a timed-out give keeps the entries', fakeAsync(() => {
      open();
      fill('reader5@example.com', '9780000000017', 4, 'Slow');
      submit();
      takePost();
      tick(8000);
      fixture.detectChanges();
      expectEntries('reader5@example.com', '9780000000017', 4, 'Slow');
    }));
  });

  describe('while a give is in flight', () => {
    it('[U01][AC10][TC01] Submit is disabled and a second Submit sends nothing', fakeAsync(() => {
      open();
      fill('reader@example.com', '9780000000017', 4);
      submit();
      expect(submitButton().disabled).toBeTrue();
      submit();
      component.onSubmit();
      component.onSubmit();
      settle();
      const posts = httpMock.match(isPost);
      expect(posts.length).toBe(1);
      expect(submitButton().disabled).toBeTrue();
      answer(posts[0], 200, 'OK', {});
    }));

    it('[U01][AC10][TC02] Submit sends again once the give has its outcome', fakeAsync(() => {
      open();
      fill('reader5@example.com', '9780000000017', 4);
      submit();
      answer(takePost(), 503, 'Service Unavailable', null);
      expect(alertText()).toBe(UNAVAILABLE);
      submit();
      answer(takePost(), 503, 'Service Unavailable', null);
      expect(alertText()).toBe(UNAVAILABLE);
    }));

    it('[U01][AC11][TC01] a new Submit clears the earlier text until the next outcome', fakeAsync(() => {
      open();
      fill('reader5@example.com', '9780000000017', 4);
      submit();
      answer(takePost(), 503, 'Service Unavailable', null);
      expect(alertText()).toBe(UNAVAILABLE);
      submit();
      expect(alertText()).toBe('');
      const second = takePost();
      tick(3000);
      fixture.detectChanges();
      expect(alertText()).toBe('');
      answer(second, 200, 'OK', {});
      expect(navigate).toHaveBeenCalledOnceWith(['/ratings']);
    }));

    it('[U01][AC11][TC02] the next outcome replaces the earlier text', fakeAsync(() => {
      open();
      fill('browser@example.com', '9780000000017', 4);
      submit();
      answer(takePost(), 503, 'Service Unavailable', null);
      submit();
      answer(takePost(), 403, 'Forbidden', null);
      expect(alertText()).toBe(BUYERS_ONLY);
    }));

    it('[U01][AC10] leaving the form with a give in flight cancels it and never opens the list', fakeAsync(() => {
      open();
      fill('reader@example.com', '9780000000017', 4);
      submit();
      const req = takePost();
      fixture.destroy();
      expect(req.cancelled).toBeTrue();
      expect(navigate).not.toHaveBeenCalled();
    }));
  });

  describe('the alert', () => {
    it('[U01][AC12][TC01] a persistent role="alert" element is there before any outcome, empty', fakeAsync(() => {
      open();
      expect(root().querySelectorAll('[role="alert"]').length).toBe(1);
      expect(alertText()).toBe('');
    }));

    it('[U01][AC12][TC01] a refusal is put in the alert element beside Submit', fakeAsync(() => {
      open();
      fill('browser@example.com', '9780000000017', 3);
      submit();
      const before = alertEl();
      answer(takePost(), 403, 'Forbidden', null);
      expect(alertEl()).toBe(before);
      expect(alertText()).toBe(BUYERS_ONLY);
      expect(submitButton().nextElementSibling).toBe(alertEl());
    }));

    it('[U01][AC12][TC02] the same element is emptied and refilled by a later outcome', fakeAsync(() => {
      open();
      fill('browser@example.com', '9780000000017', 3);
      submit();
      const element = alertEl();
      answer(takePost(), 503, 'Service Unavailable', null);
      expect(alertText()).toBe(UNAVAILABLE);
      submit();
      expect(alertEl()).toBe(element);
      expect(alertText()).toBe('');
      answer(takePost(), 403, 'Forbidden', null);
      expect(alertEl()).toBe(element);
      expect(alertText()).toBe(BUYERS_ONLY);
    }));
  });

  describe('the comment notes', () => {
    function notesAtComment(): string[] {
      const notes: string[] = [];
      let next = field('comment').nextElementSibling;
      while (next && next.tagName === 'SMALL') {
        notes.push(next.textContent ?? '');
        next = next.nextElementSibling;
      }
      return notes;
    }

    it('[U05][AC01][TC01] the form shows the comment note and the keep-comment text at the comment field', fakeAsync(() => {
      open();
      expect(notesAtComment()).toEqual([COMMENT_NOTE, KEEP_NOTE]);
    }));

    it('[U05][AC01][TC03] the notes stay while a refusal keeps the form open', fakeAsync(() => {
      open();
      fill('browser@example.com', '9780000000017', 3);
      submit();
      answer(takePost(), 403, 'Forbidden', null);
      expect(alertText()).toBe(BUYERS_ONLY);
      expect(notesAtComment()).toEqual([COMMENT_NOTE, KEEP_NOTE]);
    }));

    it('[U05][AC02][TC01] the keep-comment text is on the form', fakeAsync(() => {
      open();
      expect(notesAtComment()).toContain(KEEP_NOTE);
    }));
  });

  describe('no removal', () => {
    it('[U06][AC02][TC01] giving a rating sends no DELETE', fakeAsync(() => {
      open();
      fill('reader@example.com', '9780000000017', 5);
      submit();
      answer(takePost(), 200, 'OK', {});
      // the afterEach hook asserts that no DELETE request was made
      expect(navigate).toHaveBeenCalledTimes(1);
    }));
  });
});
