import type {RatingLine, Summary} from "./rating-outcomes";

// The specification's texts, verbatim
export const BUYERS_ONLY = 'Only customers who have bought this book can rate it.';
export const PURCHASE_CHECK_UNAVAILABLE = "We can't check your purchase right now. Nothing was saved. Please try again in a few minutes.";
export const CANNOT_SAVE = "We can't save your rating right now. Please try again in a few minutes.";
export const GONE_ON_CHANGE = 'This rating no longer exists. Nothing was saved.';
export const GONE_ON_LOAD = 'This rating no longer exists.';
export const CANNOT_LOAD = "We can't load this rating right now. Please try again in a few minutes.";
export const NOT_YET_RATED = 'Not yet rated';
export const RATING_UNAVAILABLE = 'Rating unavailable';
export const COMMENT_NOTE = 'Only Quillmark sees your comment. It is not shown to other shoppers.';
export const KEEP_COMMENT_NOTE = 'Leave this empty to keep any comment you gave before.';

// the book page's line: the average with the number of ratings
export function bookLineFor(summary: Summary | null): RatingLine {
  if (summary === null) {
    return { kind: 'unavailable', text: RATING_UNAVAILABLE };
  }
  if (summary.count === 0 || summary.average === null) {
    return { kind: 'unrated', text: NOT_YET_RATED };
  }
  const ratings = summary.count === 1 ? ' (1 rating)' : ` (${summary.count} ratings)`;
  return { kind: 'rated', text: '★ ' + summary.average.toFixed(1) + ratings };
}

// the catalogue's line: the average alone; a book the summaries do not list is not yet rated
export function catalogueLineFor(summaries: ReadonlyMap<string, Summary> | null, isbn: string): RatingLine {
  if (summaries === null) {
    return { kind: 'unavailable', text: RATING_UNAVAILABLE };
  }
  const summary = summaries.get(isbn);
  if (summary === undefined || summary.count === 0 || summary.average === null) {
    return { kind: 'unrated', text: NOT_YET_RATED };
  }
  return { kind: 'rated', text: '★ ' + summary.average.toFixed(1) };
}
