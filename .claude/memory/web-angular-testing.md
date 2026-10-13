# Web (Angular) testing gotchas

### Angular 21 TestBed is zoneless; timer specs must create the fixture inside fakeAsync
- **Context**: Karma specs for components with timers (8 s bounded ratings calls) failed to advance with tick().
- **Insight**: Angular 21.1.x TestBed is zoneless with fixture auto-detect on by default, while web/src/main.ts bootstraps with provideZoneChangeDetection(). Create the fixture inside the fakeAsync body, otherwise ngOnInit's timer is outside tick()'s reach.
- **When it applies**: Any component spec that tests timeouts/timers via fakeAsync + tick.
- **Date**: 2026-10-13
- **Ref**: Storefront ratings (Epic BOOK-2-01-02), web/src/app/ratings/rating-list/rating-list.component.spec.ts

### HttpTestingController behaviours
- **Context**: Testing HttpClient calls with observe:'response' + rxjs timeout.
- **Insight**: TestRequest.flush throws on a cancelled request; flush(null) with no status is a 204; verify() counts cancelled-but-unmatched requests. flush's body type rejects `unknown`, so use `type FlushBody = Parameters<TestRequest['flush']>[0]` and cast under strict TS.
- **When it applies**: Specs asserting timeout/cancel behaviour or flushing arbitrary bodies.
- **Date**: 2026-10-13
- **Ref**: Storefront ratings (Epic BOOK-2-01-02), web/src/app/books/book-details/book-details.component.spec.ts

### DOM assertion pitfalls in component specs
- **Context**: Two spec assertions failed unexpectedly.
- **Insight**: Angular strips whitespace between sibling elements (preserveWhitespaces false), so host textContent joins blocks with no space ("Update RatingThis..."). The TestBed host is a <div>, so 'div > div' also matches the template's outer div; use ':scope > div > div'.
- **When it applies**: Asserting on textContent or structural selectors.
- **Date**: 2026-10-13
- **Ref**: Storefront ratings (Epic BOOK-2-01-02)

### Bootstrap 5.3 form-check and legend
- **Context**: Star-choice radio styling.
- **Insight**: .form-check-input spacing applies only inside .form-check; reboot sizes <legend> at heading size, so override it.
- **When it applies**: Custom radio/checkbox groups using fieldset/legend.
- **Date**: 2026-10-13
- **Ref**: Storefront ratings (Epic BOOK-2-01-02), web/src/app/ratings/star-choice/

### `ng test` noise from untouched smoke specs
- **Context**: Karma output shows XHR errors and `ALERT: undefined`.
- **Insight**: Smoke specs for clients, ingest, storage, update-book make real XHRs to kubernetes.docker.internal; this is pre-existing noise, not a failure.
- **When it applies**: Reading `ng test` output; do not chase these.
- **Date**: 2026-10-13
- **Ref**: Storefront ratings (Epic BOOK-2-01-02)
