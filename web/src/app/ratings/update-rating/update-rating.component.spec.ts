import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';

import { UpdateRatingComponent } from './update-rating.component';
import { StarChoiceComponent } from '../star-choice/star-choice.component';
import { environment } from '../../../environments/environment';

// an answer body of any shape, as the ratings service may send it
type FlushBody = Parameters<TestRequest['flush']>[0];

const BUYERS_ONLY = 'Only customers who have bought this book can rate it.';
const UNAVAILABLE = "We can't check your purchase right now. Nothing was saved. Please try again in a few minutes.";
const CANNOT_SAVE = "We can't save your rating right now. Please try again in a few minutes.";
const GONE_ON_CHANGE = 'This rating no longer exists. Nothing was saved.';
const GONE_ON_LOAD = 'This rating no longer exists.';
const CANNOT_LOAD = "We can't load this rating right now. Please try again in a few minutes.";
const COMMENT_NOTE = 'Only Quillmark sees your comment. It is not shown to other shoppers.';
const KEEP_NOTE = 'Leave this empty to keep any comment you gave before.';

describe('UpdateRatingComponent', () => {
  const ratingsUrl = `${environment.ratingsSrvUrl}/api/v1/ratings`;
  const readable = { id: 7, email: 'reader@example.com', isbn: '9780000000017', rating: 3 };
  let routeParams: { [key: string]: string };
  let fixture: ComponentFixture<UpdateRatingComponent>;
  let component: UpdateRatingComponent;
  let httpMock: HttpTestingController;
  let navigate: jasmine.Spy;

  const isLoad = (r: HttpRequest<unknown>) => r.method === 'GET' && r.url === `${ratingsUrl}/7`;
  const isPut = (r: HttpRequest<unknown>) => r.method === 'PUT' && r.url === `${ratingsUrl}/7`;

  beforeEach(async () => {
    routeParams = { id: '7' };
    await TestBed.configureTestingModule({
      declarations: [UpdateRatingComponent, StarChoiceComponent],
      imports: [FormsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { params: routeParams } } }
      ]
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
  });

  afterEach(() => {
    expect(httpMock.match(r => r.method === 'DELETE')).toEqual([]);
    httpMock.verify();
  });

  // must run inside the fakeAsync body (the TestBed is zoneless with auto-detect)
  function openPending(): TestRequest {
    fixture = TestBed.createComponent(UpdateRatingComponent);
    component = fixture.componentInstance;
    settle();
    return httpMock.expectOne(isLoad);
  }
  function settle(): void {
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }
  function openLoaded(body: unknown = readable): void {
    const req = openPending();
    req.flush(body as FlushBody, { status: 200, statusText: 'OK' });
    settle();
  }
  const root = () => fixture.nativeElement as HTMLElement;
  const squash = (s: string | null) => (s ?? '').replace(/\s+/g, ' ').trim();
  const radios = () => Array.from(root().querySelectorAll<HTMLInputElement>('input[type="radio"]'));
  const checkedStars = () => radios().filter(r => r.checked).map(r => r.value);
  const commentField = () => root().querySelector('#comment') as HTMLInputElement;
  const submitButton = () => root().querySelector('button[type="submit"]') as HTMLButtonElement;
  const alertEl = () => root().querySelector('[role="alert"]') as HTMLElement;
  const alertText = () => alertEl().textContent;

  function choose(stars: number): void {
    (root().querySelector(`#change-stars-${stars}`) as HTMLInputElement).click();
    settle();
  }
  function typeComment(value: string): void {
    const input = commentField();
    input.value = value;
    input.dispatchEvent(new Event('input'));
    settle();
  }
  function submit(): void {
    submitButton().click();
    settle();
  }
  function takePut(): TestRequest {
    return httpMock.expectOne(isPut);
  }
  function answer(req: TestRequest, status: number, statusText: string, body: unknown): void {
    req.flush(body as FlushBody, { status, statusText });
    settle();
  }
  function change(stars: number, comment?: string): TestRequest {
    choose(stars);
    if (comment !== undefined) {
      typeComment(comment);
    }
    submit();
    return takePut();
  }
  function expectLoadFailureOnly(text: string): void {
    // the heading and the text are the screen's only two elements
    expect(Array.from(root().querySelector('.col-md-6')!.children).map(e => e.textContent)).toEqual(['Update Rating', text]);
    expect(root().querySelector('form')).toBeNull();
    expect(root().querySelector('input')).toBeNull();
    expect(root().querySelector('button')).toBeNull();
    expect(root().querySelector('[role="alert"]')).toBeNull();
    expect(root().querySelector('p')!.textContent).toBe(text);
  }

  describe('while the rating loads', () => {
    it('[U02][AC15][TC01] shows only the heading, with no field, text or Submit', fakeAsync(() => {
      const req = openPending();
      tick(3000);
      fixture.detectChanges();
      expect(squash(root().textContent)).toBe('Update Rating');
      expect(root().querySelector('h3')!.textContent).toBe('Update Rating');
      expect(root().querySelector('input')).toBeNull();
      expect(root().querySelector('button')).toBeNull();
      expect(root().querySelector('app-star-choice')).toBeNull();
      expect(root().querySelector('p')).toBeNull();
      expect(root().querySelector('[role="alert"]')).toBeNull();
      req.flush(readable, { status: 200, statusText: 'OK' });
      settle();
    }));

    it('[U02][AC15][TC02] the fields and Submit appear once loaded, and a change sends the new stars', fakeAsync(() => {
      const req = openPending();
      tick(3000);
      req.flush(readable, { status: 200, statusText: 'OK' });
      settle();
      expect(root().querySelector('#email')!.textContent).toBe('reader@example.com');
      expect(root().querySelector('#isbn')!.textContent).toBe('9780000000017');
      expect(checkedStars()).toEqual(['3']);
      expect(submitButton()).not.toBeNull();
      const put = change(4);
      expect((put.request.body as { rating: number }).rating).toBe(4);
      answer(put, 200, 'OK', {});
    }));

    it('[U02][AC02] sends no PUT until the rating has loaded', fakeAsync(() => {
      const req = openPending();
      component.onSubmit();
      settle();
      expect(httpMock.match(isPut)).toEqual([]);
      req.flush(readable, { status: 200, statusText: 'OK' });
      settle();
    }));
  });

  describe('the loaded rating', () => {
    it('[U02][AC02][TC01] shows the heading, the e-mail, the ISBN and the stars with 3 chosen', fakeAsync(() => {
      openLoaded();
      expect(root().querySelector('h3')!.textContent).toBe('Update Rating');
      expect(root().querySelector('#email')!.textContent).toBe('reader@example.com');
      expect(root().querySelector('#isbn')!.textContent).toBe('9780000000017');
      expect(radios().map(r => r.value)).toEqual(['1', '2', '3', '4', '5']);
      expect(checkedStars()).toEqual(['3']);
    }));

    it('[U02][AC02][TC02] offers no way to change the e-mail or the ISBN', fakeAsync(() => {
      openLoaded();
      expect(root().querySelector('input#email')).toBeNull();
      expect(root().querySelector('input#isbn')).toBeNull();
      const textInputs = root().querySelectorAll('input[type="text"]');
      expect(textInputs.length).toBe(1);
      expect((textInputs[0] as HTMLInputElement).id).toBe('comment');
    }));

    it('[U02][AC02][TC03] offers no value outside 1 to 5', fakeAsync(() => {
      openLoaded();
      expect(root().querySelectorAll('input[type="number"]').length).toBe(0);
      expect(radios().length).toBe(5);
    }));

    it('[U02][AC17][TC01] the stars are native radios in one fieldset with a legend', fakeAsync(() => {
      openLoaded();
      const fieldset = root().querySelector('fieldset')!;
      expect(fieldset.querySelectorAll('input[type="radio"]').length).toBe(5);
      expect(fieldset.querySelector('legend')!.textContent!.trim()).toBe('Rating');
      expect(new Set(radios().map(r => r.name))).toEqual(new Set(['change-stars']));
    }));

    it('[U02][AC17][TC02] the radios reach 1 and 5 and nothing beyond', fakeAsync(() => {
      openLoaded();
      choose(1);
      expect(checkedStars()).toEqual(['1']);
      choose(5);
      expect(checkedStars()).toEqual(['5']);
    }));

    it('[U05][AC03][TC01] a read carrying a comment opens with an empty comment field and shows it nowhere', fakeAsync(() => {
      openLoaded({ ...readable, comment: 'Great plot' });
      expect(commentField().value).toBe('');
      expect(root().textContent).not.toContain('Great plot');
      for (const input of Array.from(root().querySelectorAll('input'))) {
        expect(input.value).not.toContain('Great plot');
      }
    }));

    it('[U05][AC03][TC02] a read with no comment opens with an empty comment field', fakeAsync(() => {
      openLoaded();
      expect(commentField().value).toBe('');
    }));

    it('[U05][AC01][TC02] shows the comment note at the comment field', fakeAsync(() => {
      openLoaded();
      const note = commentField().nextElementSibling!;
      expect(note.tagName).toBe('SMALL');
      expect(note.textContent).toBe(COMMENT_NOTE);
    }));

    it('[U05][AC02][TC02] shows the keep-comment text at the comment field', fakeAsync(() => {
      openLoaded();
      const note = commentField().nextElementSibling!.nextElementSibling!;
      expect(note.tagName).toBe('SMALL');
      expect(note.textContent).toBe(KEEP_NOTE);
    }));

    it('[U02][AC16] the persistent alert element is empty once loaded', fakeAsync(() => {
      openLoaded();
      expect(root().querySelectorAll('[role="alert"]').length).toBe(1);
      expect(alertText()).toBe('');
    }));
  });

  describe('the load outcomes', () => {
    it('[U02][AC13][TC01] 404 says the rating no longer exists, in place of the fields and Submit', fakeAsync(() => {
      openPending().flush(null, { status: 404, statusText: 'Not Found' });
      settle();
      expectLoadFailureOnly(GONE_ON_LOAD);
    }));

    it('[U02][AC13][TC02] an existing rating does not say it is gone', fakeAsync(() => {
      openLoaded();
      expect(root().textContent).not.toContain(GONE_ON_LOAD);
      expect(submitButton()).not.toBeNull();
    }));

    it('[U02][AC14][TC01] an unreachable service cannot be loaded', fakeAsync(() => {
      openPending().error(new ProgressEvent('error'));
      settle();
      expectLoadFailureOnly(CANNOT_LOAD);
    }));

    it('[U02][AC14][TC02] 500 is not shown as a removed rating', fakeAsync(() => {
      openPending().flush(null, { status: 500, statusText: 'Server Error' });
      settle();
      expectLoadFailureOnly(CANNOT_LOAD);
      expect(root().textContent).not.toContain('no longer exists');
    }));

    it('[U02][AC14][TC03] a 200 that is not a rating cannot be loaded', fakeAsync(() => {
      openPending().flush(null, { status: 200, statusText: 'OK' });
      settle();
      expectLoadFailureOnly(CANNOT_LOAD);
    }));

    it('[U02][AC14] a 200 with 0 stars cannot be loaded', fakeAsync(() => {
      openPending().flush({ ...readable, rating: 0 }, { status: 200, statusText: 'OK' });
      settle();
      expectLoadFailureOnly(CANNOT_LOAD);
    }));

    it('[U02][AC14] a 200 for another rating id cannot be loaded', fakeAsync(() => {
      openPending().flush({ ...readable, id: 8 }, { status: 200, statusText: 'OK' });
      settle();
      expectLoadFailureOnly(CANNOT_LOAD);
    }));

    it('[U02][AC14] a 200 whose body fails parsing cannot be loaded', fakeAsync(() => {
      openPending().error(new ProgressEvent('error'), { status: 200, statusText: 'OK' });
      settle();
      expectLoadFailureOnly(CANNOT_LOAD);
    }));

    it('[U02][AC14][TC04] a load that is never answered shows cannot-load at 8 seconds', fakeAsync(() => {
      const req = openPending();
      tick(7999);
      fixture.detectChanges();
      expect(squash(root().textContent)).toBe('Update Rating');
      tick(1);
      fixture.detectChanges();
      expect(req.cancelled).toBeTrue();
      expectLoadFailureOnly(CANNOT_LOAD);
    }));

    it('[U02][AC14][TC05] a rating answered after the bounded time changes nothing', fakeAsync(() => {
      const req = openPending();
      tick(8000);
      fixture.detectChanges();
      expect(req.cancelled).toBeTrue();
      expectLoadFailureOnly(CANNOT_LOAD);
      tick(7000);
      fixture.detectChanges();
      expectLoadFailureOnly(CANNOT_LOAD);
    }));

    it('[U02][AC14] a route id that is not a number shows cannot-load and sends no request', fakeAsync(() => {
      routeParams['id'] = 'abc';
      fixture = TestBed.createComponent(UpdateRatingComponent);
      settle();
      expect(httpMock.match(() => true)).toEqual([]);
      expectLoadFailureOnly(CANNOT_LOAD);
    }));

    it('[U02][AC15] leaving the form while loading cancels the load', fakeAsync(() => {
      const req = openPending();
      fixture.destroy();
      expect(req.cancelled).toBeTrue();
    }));
  });

  describe('the change', () => {
    it('[U02][AC03][TC01] carries the loaded rating with the chosen stars and the comment', fakeAsync(() => {
      openLoaded();
      const put = change(5, 'Changed my mind');
      expect(put.request.body).toEqual({ id: 7, email: 'reader@example.com', isbn: '9780000000017', rating: 5, comment: 'Changed my mind' });
      expect(typeof (put.request.body as { id: unknown }).id).toBe('number');
      answer(put, 200, 'OK', {});
    }));

    it('[U02][AC03][TC02] the e-mail is sent as loaded, capitals included', fakeAsync(() => {
      openLoaded({ ...readable, email: 'Reader@Example.com' });
      const put = change(4);
      expect((put.request.body as { email: string }).email).toBe('Reader@Example.com');
      expect((put.request.body as { id: number }).id).toBe(7);
      answer(put, 200, 'OK', {});
    }));

    it('[U02][AC03][TC03] an empty comment field sends an empty or absent comment', fakeAsync(() => {
      openLoaded();
      const put = change(4);
      const body = put.request.body as { id: number; rating: number; comment?: string };
      expect(body.id).toBe(7);
      expect(body.rating).toBe(4);
      expect(body.comment ?? '').toBe('');
      answer(put, 200, 'OK', {});
    }));

    it('[U02][AC03] a comment in the read is never sent back', fakeAsync(() => {
      openLoaded({ ...readable, comment: 'Great plot' });
      const put = change(4);
      expect(JSON.stringify(put.request.body)).not.toContain('Great plot');
      answer(put, 200, 'OK', {});
    }));

    it('[U02][AC18][TC01] Submit is enabled once loaded and sends the loaded stars unchanged', fakeAsync(() => {
      openLoaded();
      expect(submitButton().disabled).toBeFalse();
      submit();
      const put = takePut();
      expect((put.request.body as { rating: number }).rating).toBe(3);
      answer(put, 200, 'OK', {});
    }));

    it('[U02][AC04][TC01] a saved change opens the Ratings list', fakeAsync(() => {
      openLoaded();
      answer(change(4), 200, 'OK', { id: 7 });
      expect(navigate).toHaveBeenCalledOnceWith(['/ratings']);
    }));

    it('[U02][AC04][TC02] a refused change does not open the Ratings list', fakeAsync(() => {
      openLoaded();
      answer(change(4), 403, 'Forbidden', null);
      expect(navigate).not.toHaveBeenCalled();
    }));

    it('[U02][AC04] a 200 whose body fails parsing is still stored and opens the Ratings list', fakeAsync(() => {
      openLoaded();
      change(4).error(new ProgressEvent('error'), { status: 200, statusText: 'OK' });
      settle();
      expect(navigate).toHaveBeenCalledOnceWith(['/ratings']);
    }));
  });

  describe('the outcome texts', () => {
    function changeAnswered(status: number, statusText: string, body: unknown): void {
      openLoaded();
      answer(change(5), status, statusText, body);
    }

    it('[U02][AC05][TC01] 403 shows the buyers-only rule', fakeAsync(() => {
      changeAnswered(403, 'Forbidden', { message: BUYERS_ONLY });
      expect(alertText()).toBe(BUYERS_ONLY);
    }));

    it('[U02][AC05][TC02] 403 with an empty body shows the same rule', fakeAsync(() => {
      changeAnswered(403, 'Forbidden', '');
      expect(alertText()).toBe(BUYERS_ONLY);
    }));

    it('[U02][AC06][TC01] 400 shows a star reason', fakeAsync(() => {
      changeAnswered(400, 'Bad Request', { message: 'Invalid rating. Must be a whole number from 1 to 5.' });
      expect(alertText()).toBe('Invalid rating. Must be a whole number from 1 to 5.');
    }));

    it('[U02][AC06][TC02] 400 shows another reason as given, not the buyers-only text', fakeAsync(() => {
      changeAnswered(400, 'Bad Request', { message: "A rating's e-mail and ISBN cannot be changed." });
      expect(alertText()).toBe("A rating's e-mail and ISBN cannot be changed.");
    }));

    it('[U02][AC06][TC03] a reason holding markup is shown as plain text', fakeAsync(() => {
      const markup = '<B>Invalid</B><img src=x onerror=alert(1)>';
      changeAnswered(400, 'Bad Request', { message: markup });
      expect(alertText()).toBe(markup);
      expect(root().querySelector('img')).toBeNull();
      expect(root().querySelector('b')).toBeNull();
      expect(alertEl().children.length).toBe(0);
    }));

    it('[U02][AC07][TC01] 503 says the purchase check is unavailable', fakeAsync(() => {
      changeAnswered(503, 'Service Unavailable', null);
      expect(alertText()).toBe(UNAVAILABLE);
    }));

    it('[U02][AC07][TC02] 503 shows the text, not the body\'s own words', fakeAsync(() => {
      changeAnswered(503, 'Service Unavailable', { message: 'Service Unavailable' });
      expect(alertText()).toBe(UNAVAILABLE);
      expect(alertText()).not.toContain('Service Unavailable');
    }));

    it('[U02][AC08][TC01] 404 says the rating no longer exists', fakeAsync(() => {
      changeAnswered(404, 'Not Found', null);
      expect(alertText()).toBe(GONE_ON_CHANGE);
    }));

    it('[U02][AC08][TC02] 404 is not shown as a failure to save, nor with the body\'s words', fakeAsync(() => {
      changeAnswered(404, 'Not Found', { message: 'Rating not found' });
      expect(alertText()).toBe(GONE_ON_CHANGE);
      expect(alertText()).not.toContain('Rating not found');
    }));

    it('[U02][AC09][TC01] an unreachable service cannot be saved', fakeAsync(() => {
      openLoaded();
      change(4).error(new ProgressEvent('error'));
      settle();
      expect(alertText()).toBe(CANNOT_SAVE);
    }));

    it('[U02][AC09][TC02] 500 is not shown as a refusal', fakeAsync(() => {
      changeAnswered(500, 'Server Error', null);
      expect(alertText()).toBe(CANNOT_SAVE);
    }));

    it('[U02][AC09][TC03] 400 with an empty body cannot be saved, with no "undefined" text', fakeAsync(() => {
      changeAnswered(400, 'Bad Request', '');
      expect(alertText()).toBe(CANNOT_SAVE);
      expect(alertText()).not.toContain('undefined');
    }));

    it('[U02][AC09][TC04] a change that is never answered shows cannot-save at 8 seconds', fakeAsync(() => {
      openLoaded();
      const put = change(4);
      tick(7999);
      fixture.detectChanges();
      expect(alertText()).toBe('');
      tick(1);
      fixture.detectChanges();
      expect(put.cancelled).toBeTrue();
      expect(alertText()).toBe(CANNOT_SAVE);
      expect(submitButton().disabled).toBeFalse();
    }));

    it('[U02][AC09][TC05] a change answered after the bounded time changes nothing', fakeAsync(() => {
      openLoaded();
      const put = change(4);
      tick(8000);
      fixture.detectChanges();
      expect(put.cancelled).toBeTrue();
      expect(alertText()).toBe(CANNOT_SAVE);
      tick(7000);
      fixture.detectChanges();
      expect(alertText()).toBe(CANNOT_SAVE);
      expect(navigate).not.toHaveBeenCalled();
    }));
  });

  describe('the entries are kept', () => {
    it('[U02][AC10][TC01] a buyers-only refusal keeps the stars and the comment', fakeAsync(() => {
      openLoaded();
      answer(change(5, 'Changed my mind'), 403, 'Forbidden', null);
      expect(checkedStars()).toEqual(['5']);
      expect(commentField().value).toBe('Changed my mind');
    }));

    it('[U02][AC10][TC02] an unavailable refusal keeps the stars and the comment', fakeAsync(() => {
      openLoaded();
      answer(change(1, 'Not for me'), 503, 'Service Unavailable', null);
      expect(checkedStars()).toEqual(['1']);
      expect(commentField().value).toBe('Not for me');
      expect(navigate).not.toHaveBeenCalled();
    }));

    it('[U02][AC10][TC03] a change to a removed rating keeps the stars and the comment', fakeAsync(() => {
      openLoaded();
      answer(change(4, 'Still good'), 404, 'Not Found', null);
      expect(checkedStars()).toEqual(['4']);
      expect(commentField().value).toBe('Still good');
    }));

    it('[U02][AC10][TC04] an unreachable service keeps the stars and the comment', fakeAsync(() => {
      openLoaded();
      change(2, 'On reflection').error(new ProgressEvent('error'));
      settle();
      expect(checkedStars()).toEqual(['2']);
      expect(commentField().value).toBe('On reflection');
    }));

    it('[U02][AC10] the e-mail and the ISBN stay as loaded after a refusal', fakeAsync(() => {
      openLoaded();
      answer(change(4), 503, 'Service Unavailable', null);
      expect(root().querySelector('#email')!.textContent).toBe('reader@example.com');
      expect(root().querySelector('#isbn')!.textContent).toBe('9780000000017');
    }));
  });

  describe('while a change is in flight', () => {
    it('[U02][AC11][TC01] Submit is disabled and a second Submit sends nothing', fakeAsync(() => {
      openLoaded();
      const put = change(4);
      expect(submitButton().disabled).toBeTrue();
      submit();
      component.onSubmit();
      settle();
      expect(httpMock.match(isPut)).toEqual([]);
      expect(submitButton().disabled).toBeTrue();
      answer(put, 200, 'OK', {});
    }));

    it('[U02][AC11][TC02] Submit sends again once the change has its outcome', fakeAsync(() => {
      openLoaded();
      answer(change(4), 503, 'Service Unavailable', null);
      expect(submitButton().disabled).toBeFalse();
      submit();
      answer(takePut(), 503, 'Service Unavailable', null);
      expect(alertText()).toBe(UNAVAILABLE);
    }));

    it('[U02][AC18][TC02] Submit is enabled again after an outcome', fakeAsync(() => {
      openLoaded();
      answer(change(4), 503, 'Service Unavailable', null);
      expect(alertText()).toBe(UNAVAILABLE);
      expect(submitButton().disabled).toBeFalse();
    }));

    it('[U02][AC12][TC01] a new Submit clears the earlier text until the next outcome', fakeAsync(() => {
      openLoaded();
      answer(change(4), 503, 'Service Unavailable', null);
      expect(alertText()).toBe(UNAVAILABLE);
      submit();
      const second = takePut();
      expect(alertText()).toBe('');
      tick(3000);
      fixture.detectChanges();
      expect(alertText()).toBe('');
      answer(second, 200, 'OK', {});
      expect(navigate).toHaveBeenCalledOnceWith(['/ratings']);
    }));

    it('[U02][AC12][TC02] the next outcome replaces the earlier text', fakeAsync(() => {
      openLoaded();
      answer(change(4), 503, 'Service Unavailable', null);
      submit();
      answer(takePut(), 404, 'Not Found', null);
      expect(alertText()).toBe(GONE_ON_CHANGE);
      expect(alertText()).not.toContain('purchase');
    }));

    it('[U02][AC16][TC02] the same alert element is emptied and refilled by each outcome', fakeAsync(() => {
      openLoaded();
      const element = alertEl();
      answer(change(4), 503, 'Service Unavailable', null);
      expect(alertEl()).toBe(element);
      expect(alertText()).toBe(UNAVAILABLE);
      submit();
      expect(alertEl()).toBe(element);
      expect(alertText()).toBe('');
      answer(takePut(), 404, 'Not Found', null);
      expect(alertEl()).toBe(element);
      expect(alertText()).toBe(GONE_ON_CHANGE);
    }));

    it('[U02][AC16][TC01] a refusal is announced in the alert element beside Submit', fakeAsync(() => {
      openLoaded();
      answer(change(5), 403, 'Forbidden', null);
      expect(alertText()).toBe(BUYERS_ONLY);
      expect(submitButton().nextElementSibling).toBe(alertEl());
    }));

    it('[U02][AC12] a repeat Submit after a 404 is enabled, sends a second PUT, empties then refills the alert', fakeAsync(() => {
      openLoaded();
      answer(change(4), 404, 'Not Found', null);
      expect(alertText()).toBe(GONE_ON_CHANGE);
      expect(submitButton().disabled).toBeFalse();
      submit();
      const second = takePut();
      expect(alertText()).toBe('');
      answer(second, 404, 'Not Found', null);
      expect(alertText()).toBe(GONE_ON_CHANGE);
    }));

    it('[U02][AC11] leaving the form with a change in flight cancels it and never opens the list', fakeAsync(() => {
      openLoaded();
      const put = change(4);
      fixture.destroy();
      expect(put.cancelled).toBeTrue();
      expect(navigate).not.toHaveBeenCalled();
    }));

    it('[U05][AC02][TC03] the keep-comment text stays while a refusal keeps the form open', fakeAsync(() => {
      openLoaded();
      answer(change(4), 503, 'Service Unavailable', null);
      expect(alertText()).toBe(UNAVAILABLE);
      expect(commentField().nextElementSibling!.nextElementSibling!.textContent).toBe(KEEP_NOTE);
    }));
  });

  describe('no removal', () => {
    it('[U06][AC02][TC01] changing a rating sends no DELETE', fakeAsync(() => {
      openLoaded();
      answer(change(4), 200, 'OK', {});
      // the afterEach hook asserts that no DELETE request was made
      expect(navigate).toHaveBeenCalledTimes(1);
    }));
  });
});
