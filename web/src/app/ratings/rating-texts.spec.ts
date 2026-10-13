import { Summary } from './rating-outcomes';
import {
  BUYERS_ONLY,
  CANNOT_LOAD,
  CANNOT_SAVE,
  COMMENT_NOTE,
  GONE_ON_CHANGE,
  GONE_ON_LOAD,
  KEEP_COMMENT_NOTE,
  NOT_YET_RATED,
  PURCHASE_CHECK_UNAVAILABLE,
  RATING_UNAVAILABLE,
  bookLineFor,
  catalogueLineFor
} from './rating-texts';

// U+2605 followed by one U+0020
const STAR = '★ ';

function summaries(...entries: Summary[]): ReadonlyMap<string, Summary> {
  return new Map(entries.map(e => [e.isbn, e] as [string, Summary]));
}

describe('rating-texts', () => {

  describe('the specification\'s texts, verbatim', () => {
    it('[U01][AC05] buyers only', () => {
      expect(BUYERS_ONLY).toBe('Only customers who have bought this book can rate it.');
    });
    it('[U01][AC07] purchase check unavailable', () => {
      expect(PURCHASE_CHECK_UNAVAILABLE)
        .toBe("We can't check your purchase right now. Nothing was saved. Please try again in a few minutes.");
    });
    it('[U01][AC08] cannot save', () => {
      expect(CANNOT_SAVE).toBe("We can't save your rating right now. Please try again in a few minutes.");
    });
    it('[U02][AC08] gone on change', () => {
      expect(GONE_ON_CHANGE).toBe('This rating no longer exists. Nothing was saved.');
    });
    it('[U02][AC13] gone on load', () => {
      expect(GONE_ON_LOAD).toBe('This rating no longer exists.');
    });
    it('[U02][AC14] cannot load', () => {
      expect(CANNOT_LOAD).toBe("We can't load this rating right now. Please try again in a few minutes.");
    });
    it('[U03][AC03] not yet rated', () => {
      expect(NOT_YET_RATED).toBe('Not yet rated');
    });
    it('[U03][AC04] rating unavailable', () => {
      expect(RATING_UNAVAILABLE).toBe('Rating unavailable');
    });
    it('[U05][AC01] comment note', () => {
      expect(COMMENT_NOTE).toBe('Only Quillmark sees your comment. It is not shown to other shoppers.');
    });
    it('[U05][AC02] keep-comment note', () => {
      expect(KEEP_COMMENT_NOTE).toBe('Leave this empty to keep any comment you gave before.');
    });
  });

  describe('bookLineFor', () => {
    it('[U03][AC02][TC01] 37 ratings of average 4 show the plural count', () => {
      expect(bookLineFor({ isbn: 'a', count: 37, average: 4 }))
        .toEqual({ kind: 'rated', text: STAR + '4.0 (37 ratings)' });
    });

    it('[U03][AC02][TC02] one rating is worded in the singular', () => {
      expect(bookLineFor({ isbn: 'a', count: 1, average: 5 }))
        .toEqual({ kind: 'rated', text: STAR + '5.0 (1 rating)' });
    });

    it('[U03][AC02][TC03] two ratings are worded in the plural', () => {
      expect(bookLineFor({ isbn: 'a', count: 2, average: 3.5 }))
        .toEqual({ kind: 'rated', text: STAR + '3.5 (2 ratings)' });
    });

    it('[U03][AC01][TC01] an average of 4.3 shows one decimal', () => {
      expect(bookLineFor({ isbn: 'a', count: 4, average: 4.3 }).text).toBe(STAR + '4.3 (4 ratings)');
    });

    it('[U03][AC03][TC02] a count of 0 is "Not yet rated" with no star and no count', () => {
      const line = bookLineFor({ isbn: 'a', count: 0, average: null });
      expect(line).toEqual({ kind: 'unrated', text: 'Not yet rated' });
      expect(line.text).not.toContain('★');
      expect(line.text).not.toContain('(0');
    });

    it('[U03][AC04] no readable summary is "Rating unavailable"', () => {
      expect(bookLineFor(null)).toEqual({ kind: 'unavailable', text: 'Rating unavailable' });
    });
  });

  describe('catalogueLineFor', () => {
    it('[U04][AC01][TC01] a rated book shows the average alone', () => {
      expect(catalogueLineFor(summaries({ isbn: 'a', count: 4, average: 4.3 }), 'a'))
        .toEqual({ kind: 'rated', text: STAR + '4.3' });
    });

    it('[U04][AC01][TC02] a whole average shows one decimal', () => {
      expect(catalogueLineFor(summaries({ isbn: 'a', count: 1, average: 5 }), 'a').text).toBe(STAR + '5.0');
    });

    it('[U04][AC01][TC03] the count is not shown', () => {
      const text = catalogueLineFor(summaries({ isbn: 'a', count: 37, average: 4 }), 'a').text;
      expect(text).toBe(STAR + '4.0');
      expect(text).not.toContain('37');
      expect(text).not.toContain('(');
    });

    it('[U04][AC02][TC01] an ISBN the summaries do not list is not yet rated', () => {
      expect(catalogueLineFor(summaries({ isbn: 'a', count: 4, average: 4.3 }), 'b'))
        .toEqual({ kind: 'unrated', text: 'Not yet rated' });
    });

    it('[U04][AC02] an empty map leaves every book not yet rated', () => {
      expect(catalogueLineFor(summaries(), 'a').text).toBe('Not yet rated');
    });

    it('[U04][AC02][TC03] a listed count of 0 is not yet rated, with no star', () => {
      const line = catalogueLineFor(summaries({ isbn: 'a', count: 0, average: null }), 'a');
      expect(line).toEqual({ kind: 'unrated', text: 'Not yet rated' });
    });

    it('[U04][AC04] no readable summaries is "Rating unavailable" for every ISBN', () => {
      expect(catalogueLineFor(null, 'a')).toEqual({ kind: 'unavailable', text: 'Rating unavailable' });
      expect(catalogueLineFor(null, 'b')).toEqual({ kind: 'unavailable', text: 'Rating unavailable' });
    });
  });

  describe('the list and the book page show one answer alike', () => {
    it('[U04][AC03][TC01] the same average gives the same "★ 4.3"', () => {
      const summary: Summary = { isbn: 'a', count: 4, average: 4.3 };
      expect(catalogueLineFor(summaries(summary), 'a').text).toBe(STAR + '4.3');
      expect(bookLineFor(summary).text.startsWith(STAR + '4.3')).toBeTrue();
    });

    it('[U04][AC03][TC02] no rating gives the same "Not yet rated"', () => {
      const fromCatalogue = catalogueLineFor(summaries(), 'a').text;
      const fromBookPage = bookLineFor({ isbn: 'a', count: 0, average: null }).text;
      expect(fromCatalogue).toBe('Not yet rated');
      expect(fromBookPage).toBe('Not yet rated');
    });
  });
});
