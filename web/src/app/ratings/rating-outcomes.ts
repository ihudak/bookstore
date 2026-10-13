import {
  BUYERS_ONLY,
  CANNOT_LOAD,
  CANNOT_SAVE,
  GONE_ON_CHANGE,
  GONE_ON_LOAD,
  PURCHASE_CHECK_UNAVAILABLE
} from "./rating-texts";

// The bounded time of every request the storefront sends to the ratings service
export const RATINGS_BOUNDED_TIME_MS = 8000;

export interface EnteredRating { email: string; isbn: string; rating: number; comment: string; }
export interface RatingToEdit { id: number; email: string; isbn: string; stars: number; }
export interface Summary { isbn: string; count: number; average: number | null; }

export type WriteKind = 'buyers-only' | 'invalid' | 'unavailable' | 'gone' | 'cannot-save';
export type WriteOutcome = { stored: true } | { stored: false; kind: WriteKind; text: string };
export type LoadOutcome =
  | { kind: 'loaded'; rating: RatingToEdit }
  | { kind: 'gone' | 'cannot-load'; text: string };
export interface RatingLine { kind: 'rated' | 'unrated' | 'unavailable'; text: string; }

function asObject(body: unknown): Record<string, unknown> | null {
  return typeof body === 'object' && body !== null && !Array.isArray(body) ? body as Record<string, unknown> : null;
}

function isNonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && value.length > 0;
}

// the outcome of a give or a change, from the ratings service's status and body
export function classifyWrite(status: number, body: unknown, op: 'give' | 'change'): WriteOutcome {
  if (status === 200) {
    return { stored: true };
  }
  if (status === 403) {
    return { stored: false, kind: 'buyers-only', text: BUYERS_ONLY };
  }
  if (status === 400) {
    const reason = readableReason(body);
    if (reason !== null) {
      return { stored: false, kind: 'invalid', text: reason };
    }
  }
  if (status === 503) {
    return { stored: false, kind: 'unavailable', text: PURCHASE_CHECK_UNAVAILABLE };
  }
  if (status === 404 && op === 'change') {
    return { stored: false, kind: 'gone', text: GONE_ON_CHANGE };
  }
  return { stored: false, kind: 'cannot-save', text: CANNOT_SAVE };
}

// a message holding a character other than a space, returned untrimmed
export function readableReason(body: unknown): string | null {
  const o = asObject(body);
  const message = o ? o['message'] : undefined;
  return typeof message === 'string' && /[^ ]/.test(message) ? message : null;
}

// the outcome of loading the rating to change; a comment in the read is never copied
export function classifyLoad(status: number, body: unknown, requestedId: number): LoadOutcome {
  if (status === 404) {
    return { kind: 'gone', text: GONE_ON_LOAD };
  }
  const o = asObject(body);
  if (status === 200 && o) {
    const id = o['id'];
    const email = o['email'];
    const isbn = o['isbn'];
    const stars = o['rating'];
    if (Number.isInteger(id) && id === requestedId && isNonEmptyString(email) && isNonEmptyString(isbn)
      && Number.isInteger(stars) && (stars as number) >= 1 && (stars as number) <= 5) {
      return { kind: 'loaded', rating: { id: id as number, email, isbn, stars: stars as number } };
    }
  }
  return { kind: 'cannot-load', text: CANNOT_LOAD };
}

// a book's summary: an integer count and, above 0, an average with at most one decimal place
export function readSummary(body: unknown): Summary | null {
  const o = asObject(body);
  if (!o) {
    return null;
  }
  const count = o['count'];
  if (!Number.isInteger(count) || (count as number) < 0) {
    return null;
  }
  const isbn = typeof o['isbn'] === 'string' ? o['isbn'] as string : '';
  if (count === 0) {
    return { isbn, count: 0, average: null };
  }
  const average = o['average'];
  if (typeof average !== 'number' || !Number.isFinite(average)
    || Math.abs(average * 10 - Math.round(average * 10)) >= 1e-9) {
    return null;
  }
  return { isbn, count: count as number, average };
}

// every rated book's summary by ISBN; one unreadable entry makes the whole answer unreadable
export function readSummaries(body: unknown): ReadonlyMap<string, Summary> | null {
  if (!Array.isArray(body)) {
    return null;
  }
  const summaries = new Map<string, Summary>();
  for (const entry of body) {
    const summary = readSummary(entry);
    if (summary === null || !isNonEmptyString(summary.isbn) || summaries.has(summary.isbn)) {
      return null;
    }
    summaries.set(summary.isbn, summary);
  }
  return summaries;
}
