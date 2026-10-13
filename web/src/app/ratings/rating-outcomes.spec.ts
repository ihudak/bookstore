import {
  RATINGS_BOUNDED_TIME_MS,
  classifyLoad,
  classifyWrite,
  readSummaries,
  readSummary,
  readableReason
} from './rating-outcomes';

const BUYERS_ONLY = 'Only customers who have bought this book can rate it.';
const PURCHASE_CHECK_UNAVAILABLE = "We can't check your purchase right now. Nothing was saved. Please try again in a few minutes.";
const CANNOT_SAVE = "We can't save your rating right now. Please try again in a few minutes.";
const GONE_ON_CHANGE = 'This rating no longer exists. Nothing was saved.';
const GONE_ON_LOAD = 'This rating no longer exists.';
const CANNOT_LOAD = "We can't load this rating right now. Please try again in a few minutes.";

const OPS: ('give' | 'change')[] = ['give', 'change'];

describe('rating-outcomes', () => {

  it('[U01][AC08] the bounded time is 8 seconds, within the specification\'s 10', () => {
    expect(RATINGS_BOUNDED_TIME_MS).toBe(8000);
  });

  describe('classifyWrite', () => {
    const bodies: [string, unknown][] = [
      ['a reason', { message: 'Invalid email address' }],
      ['a blank reason', { message: '' }],
      ['an all-space reason', { message: '   ' }],
      ['a non-string message', { message: 42 }],
      ['no message', { error: 'x' }],
      ['null', null],
      ['a string', 'Bad Request']
    ];

    for (const op of OPS) {
      for (const [label, body] of bodies) {
        it(`[U01][AC04] ${op}: 200 is stored whatever the body (${label})`, () => {
          expect(classifyWrite(200, body, op)).toEqual({ stored: true });
        });
        it(`[U01][AC05] ${op}: 403 is the buyers-only rule whatever the body (${label})`, () => {
          expect(classifyWrite(403, body, op)).toEqual({ stored: false, kind: 'buyers-only', text: BUYERS_ONLY });
        });
        it(`[U01][AC07] ${op}: 503 is the purchase-check text whatever the body (${label})`, () => {
          expect(classifyWrite(503, body, op)).toEqual({ stored: false, kind: 'unavailable', text: PURCHASE_CHECK_UNAVAILABLE });
        });
      }

      it(`[U01][AC06] ${op}: 400 with a reason shows the reason as given`, () => {
        expect(classifyWrite(400, { message: 'ISBN must be a 13-digits value' }, op))
          .toEqual({ stored: false, kind: 'invalid', text: 'ISBN must be a 13-digits value' });
      });

      it(`[U01][AC06] ${op}: 400 keeps the reason's spaces, untrimmed`, () => {
        expect(classifyWrite(400, { message: '  Invalid  ' }, op))
          .toEqual({ stored: false, kind: 'invalid', text: '  Invalid  ' });
      });

      it(`[U01][AC06][TC04] ${op}: 400 keeps markup in the reason as plain characters`, () => {
        const markup = '<B>Invalid</B><img src=x onerror=alert(1)>';
        expect(classifyWrite(400, { message: markup }, op)).toEqual({ stored: false, kind: 'invalid', text: markup });
      });

      for (const [label, body] of bodies.slice(1)) {
        it(`[U01][AC08][TC03] ${op}: 400 with no readable reason (${label}) cannot be saved`, () => {
          expect(classifyWrite(400, body, op)).toEqual({ stored: false, kind: 'cannot-save', text: CANNOT_SAVE });
        });
      }

      for (const status of [201, 204, 500, 0]) {
        it(`[U01][AC08] ${op}: status ${status} cannot be saved, whatever the body`, () => {
          expect(classifyWrite(status, { message: 'Service Unavailable' }, op))
            .toEqual({ stored: false, kind: 'cannot-save', text: CANNOT_SAVE });
          expect(classifyWrite(status, null, op))
            .toEqual({ stored: false, kind: 'cannot-save', text: CANNOT_SAVE });
        });
      }
    }

    it('[U02][AC08] change: 404 says the rating no longer exists, not the body\'s words', () => {
      expect(classifyWrite(404, { message: 'Rating not found' }, 'change'))
        .toEqual({ stored: false, kind: 'gone', text: GONE_ON_CHANGE });
    });

    it('[U01][AC08][TC04] give: 404 cannot be saved, it is not "no longer exists"', () => {
      expect(classifyWrite(404, null, 'give'))
        .toEqual({ stored: false, kind: 'cannot-save', text: CANNOT_SAVE });
    });
  });

  describe('readableReason', () => {
    it('[U01][AC06] returns a message as given', () => {
      expect(readableReason({ message: 'Invalid email address' })).toBe('Invalid email address');
    });

    it('[U01][AC06] returns a message untrimmed', () => {
      expect(readableReason({ message: ' a ' })).toBe(' a ');
    });

    it('[U01][AC14] a message holding only a tab or a non-breaking space is readable', () => {
      expect(readableReason({ message: '\t' })).toBe('\t');
      expect(readableReason({ message: '\u00a0' })).toBe('\u00a0');
    });

    const unreadable: [string, unknown][] = [
      ['an empty message', { message: '' }],
      ['an all-space message', { message: '   ' }],
      ['a numeric message', { message: 5 }],
      ['a null message', { message: null }],
      ['no message', {}],
      ['a null body', null],
      ['a string body', 'Bad Request'],
      ['an array body', [{ message: 'x' }]]
    ];
    for (const [label, body] of unreadable) {
      it(`[U01][AC08] has no reason for ${label}`, () => {
        expect(readableReason(body)).toBeNull();
      });
    }
  });

  describe('classifyLoad', () => {
    const readable = { id: 7, email: 'reader@example.com', isbn: '9780000000017', rating: 3 };

    it('[U02][AC02] a readable rating is loaded with its id, e-mail, ISBN and stars', () => {
      expect(classifyLoad(200, readable, 7))
        .toEqual({ kind: 'loaded', rating: { id: 7, email: 'reader@example.com', isbn: '9780000000017', stars: 3 } });
    });

    it('[U05][AC03][TC01] the comment of a read is not copied', () => {
      const outcome = classifyLoad(200, { ...readable, comment: 'Great plot' }, 7);
      expect(outcome).toEqual({ kind: 'loaded', rating: { id: 7, email: 'reader@example.com', isbn: '9780000000017', stars: 3 } });
      expect(JSON.stringify(outcome)).not.toContain('Great plot');
    });

    it('[U02][AC02] the e-mail is kept as loaded, capitals included', () => {
      const outcome = classifyLoad(200, { ...readable, email: 'Reader@Example.com' }, 7);
      expect(outcome).toEqual({ kind: 'loaded', rating: { id: 7, email: 'Reader@Example.com', isbn: '9780000000017', stars: 3 } });
    });

    it('[U02][AC02] the stars 1 and 5 are readable', () => {
      expect(classifyLoad(200, { ...readable, rating: 1 }, 7).kind).toBe('loaded');
      expect(classifyLoad(200, { ...readable, rating: 5 }, 7).kind).toBe('loaded');
    });

    it('[U02][AC13] 404 is gone, whatever the body', () => {
      expect(classifyLoad(404, null, 7)).toEqual({ kind: 'gone', text: GONE_ON_LOAD });
      expect(classifyLoad(404, readable, 7)).toEqual({ kind: 'gone', text: GONE_ON_LOAD });
    });

    const unreadable: [string, number, unknown][] = [
      ['an id other than the one requested', 200, { ...readable, id: 8 }],
      ['a string id', 200, { ...readable, id: '7' }],
      ['0 stars', 200, { ...readable, rating: 0 }],
      ['6 stars', 200, { ...readable, rating: 6 }],
      ['3.5 stars', 200, { ...readable, rating: 3.5 }],
      ['string stars', 200, { ...readable, rating: '3' }],
      ['an empty e-mail', 200, { ...readable, email: '' }],
      ['no e-mail', 200, { id: 7, isbn: '9780000000017', rating: 3 }],
      ['an empty ISBN', 200, { ...readable, isbn: '' }],
      ['a null body', 200, null],
      ['an empty string body', 200, ''],
      ['an array body', 200, [readable]],
      ['a 500', 500, readable],
      ['a 201', 201, readable],
      ['status 0', 0, null]
    ];
    for (const [label, status, body] of unreadable) {
      it(`[U02][AC14] ${label} cannot be loaded, it is not "no longer exists"`, () => {
        expect(classifyLoad(status, body, 7)).toEqual({ kind: 'cannot-load', text: CANNOT_LOAD });
      });
    }
  });

  describe('readSummary', () => {
    it('[U03][AC01] a count above 0 with a one-decimal average is readable', () => {
      expect(readSummary({ isbn: '9780000000062', count: 4, average: 4.3 }))
        .toEqual({ isbn: '9780000000062', count: 4, average: 4.3 });
    });

    it('[U03][AC01][TC02] a whole average is readable', () => {
      expect(readSummary({ isbn: '9780000000079', count: 3, average: 4 }))
        .toEqual({ isbn: '9780000000079', count: 3, average: 4 });
    });

    it('[U03][AC03] a count of 0 with a null average is readable as unrated', () => {
      expect(readSummary({ isbn: '9780000000123', count: 0, average: null }))
        .toEqual({ isbn: '9780000000123', count: 0, average: null });
    });

    it('[U03][AC03] a count of 0 is unrated whatever an average says', () => {
      expect(readSummary({ isbn: '9780000000123', count: 0, average: 7 }))
        .toEqual({ isbn: '9780000000123', count: 0, average: null });
    });

    it('[U03][AC03] a count of 0 with no average at all is readable as unrated', () => {
      expect(readSummary({ count: 0 })).toEqual({ isbn: '', count: 0, average: null });
    });

    it('[U03][AC01] the isbn is the body\'s string isbn, else empty', () => {
      expect(readSummary({ count: 2, average: 3.5 })).toEqual({ isbn: '', count: 2, average: 3.5 });
      expect(readSummary({ isbn: 5, count: 2, average: 3.5 })).toEqual({ isbn: '', count: 2, average: 3.5 });
    });

    const unreadable: [string, unknown][] = [
      ['a count above 0 and no average', { isbn: 'x', count: 4 }],
      ['a count above 0 and a null average', { isbn: 'x', count: 4, average: null }],
      ['a negative count', { isbn: 'x', count: -1, average: 4 }],
      ['a fractional count', { isbn: 'x', count: 1.5, average: 4 }],
      ['a string count', { isbn: 'x', count: '4', average: 4 }],
      ['no count', { isbn: 'x', average: 4 }],
      ['an average with two decimals', { isbn: 'x', count: 4, average: 4.35 }],
      ['a string average', { isbn: 'x', count: 4, average: '4.3' }],
      ['an infinite average', { isbn: 'x', count: 4, average: Infinity }],
      ['a null body', null],
      ['a string body', 'summary'],
      ['an array body', [{ count: 4, average: 4.3 }]]
    ];
    for (const [label, body] of unreadable) {
      it(`[U03][AC04] ${label} is not readable`, () => {
        expect(readSummary(body)).toBeNull();
      });
    }
  });

  describe('readSummaries', () => {
    it('[U04][AC01] readable entries become a map keyed by ISBN', () => {
      const map = readSummaries([
        { isbn: '9780000000062', count: 4, average: 4.3 },
        { isbn: '9780000000109', count: 1, average: 5 }
      ]);
      expect(map).not.toBeNull();
      expect(map!.size).toBe(2);
      expect(map!.get('9780000000062')).toEqual({ isbn: '9780000000062', count: 4, average: 4.3 });
      expect(map!.get('9780000000109')).toEqual({ isbn: '9780000000109', count: 1, average: 5 });
    });

    it('[U04][AC02] an empty list is an empty map, not unreadable', () => {
      const map = readSummaries([]);
      expect(map).not.toBeNull();
      expect(map!.size).toBe(0);
    });

    it('[U04][AC02][TC03] an entry with a count of 0 is kept, unrated', () => {
      const map = readSummaries([{ isbn: '9780000000130', count: 0, average: null }]);
      expect(map!.get('9780000000130')).toEqual({ isbn: '9780000000130', count: 0, average: null });
    });

    it('[U04][AC04][TC06] one unreadable entry makes the whole response unreadable', () => {
      expect(readSummaries([
        { isbn: '9780000000062', count: 4, average: 4.3 },
        { isbn: '9780000000123', count: 2 }
      ])).toBeNull();
    });

    it('[U04][AC04] a repeated ISBN makes the response unreadable', () => {
      expect(readSummaries([
        { isbn: '9780000000062', count: 4, average: 4.3 },
        { isbn: '9780000000062', count: 5, average: 4 }
      ])).toBeNull();
    });

    it('[U04][AC04] an entry without an ISBN makes the response unreadable', () => {
      expect(readSummaries([{ count: 4, average: 4.3 }])).toBeNull();
      expect(readSummaries([{ isbn: '', count: 4, average: 4.3 }])).toBeNull();
    });

    it('[U04][AC04][TC03] a body that is not a list is unreadable', () => {
      expect(readSummaries({ isbn: '9780000000062', count: 4, average: 4.3 })).toBeNull();
      expect(readSummaries(null)).toBeNull();
      expect(readSummaries('summaries')).toBeNull();
    });
  });
});
