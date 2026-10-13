import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { StarChoiceComponent } from './star-choice.component';

describe('StarChoiceComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [StarChoiceComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // the required input is set in the same tick as the component is created
  function create(stars?: number): ComponentFixture<StarChoiceComponent> {
    const fixture = TestBed.createComponent(StarChoiceComponent);
    fixture.componentRef.setInput('name', 'stars-under-test');
    if (stars !== undefined) {
      fixture.componentRef.setInput('stars', stars);
    }
    fixture.detectChanges();
    return fixture;
  }

  const radios = (fixture: ComponentFixture<StarChoiceComponent>) =>
    Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLInputElement>('input[type="radio"]'));

  it('[U01][AC01][TC01] offers exactly the five whole stars 1 to 5, none chosen', () => {
    const fixture = create();
    expect(radios(fixture).map(r => r.value)).toEqual(['1', '2', '3', '4', '5']);
    expect(radios(fixture).map(r => r.checked)).toEqual([false, false, false, false, false]);
  });

  it('[U01][AC01][TC02] offers nothing but the five radios, so no other value can be entered', () => {
    const root = create().nativeElement as HTMLElement;
    expect(root.querySelectorAll('input').length).toBe(5);
    expect(root.querySelectorAll('input[type="number"], input[type="text"], input[type="range"]').length).toBe(0);
  });

  it('[U01][AC13][TC01] the radios sit in one fieldset with the legend "Rating"', () => {
    const root = create().nativeElement as HTMLElement;
    const fieldsets = root.querySelectorAll('fieldset');
    expect(fieldsets.length).toBe(1);
    expect(fieldsets[0].querySelectorAll('input[type="radio"]').length).toBe(5);
    expect(fieldsets[0].querySelector('legend')!.textContent!.trim()).toBe('Rating');
  });

  it('[U01][AC13] all five radios share the group name so the arrow keys move within them', () => {
    const fixture = create();
    expect(radios(fixture).map(r => r.name)).toEqual(Array(5).fill('stars-under-test'));
  });

  it('[U01][AC13] each radio has a label for it showing its number', () => {
    const fixture = create();
    const root = fixture.nativeElement as HTMLElement;
    for (const n of [1, 2, 3, 4, 5]) {
      const radio = root.querySelector(`#stars-under-test-${n}`) as HTMLInputElement;
      expect(radio).not.toBeNull();
      expect(radio.type).toBe('radio');
      const label = root.querySelector(`label[for="stars-under-test-${n}"]`) as HTMLLabelElement;
      expect(label.textContent!.trim()).toBe(String(n));
    }
  });

  it('[U02][AC02] with 3 stars given, only the radio 3 is checked', () => {
    const fixture = create(3);
    expect(radios(fixture).map(r => r.checked)).toEqual([false, false, true, false, false]);
  });

  it('[U02][AC02] changing the stars input moves the checked radio', () => {
    const fixture = create(3);
    fixture.componentRef.setInput('stars', 5);
    fixture.detectChanges();
    expect(radios(fixture).map(r => r.checked)).toEqual([false, false, false, false, true]);
  });

  it('[U01][AC13] choosing radio 4 emits 4 once', () => {
    const fixture = create();
    const emitted: number[] = [];
    fixture.componentInstance.starsChange.subscribe((n: number) => emitted.push(n));
    radios(fixture)[3].click();
    fixture.detectChanges();
    expect(emitted).toEqual([4]);
    expect(radios(fixture).map(r => r.checked)).toEqual([false, false, false, true, false]);
  });

  it('[U01][AC13][TC02] choosing 1 and then 5 emits 1 then 5', () => {
    const fixture = create();
    const emitted: number[] = [];
    fixture.componentInstance.starsChange.subscribe((n: number) => emitted.push(n));
    radios(fixture)[0].click();
    radios(fixture)[4].click();
    expect(emitted).toEqual([1, 5]);
  });
});
