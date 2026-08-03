import { describe, expect, it } from 'vitest';
import { normalizeApiPrice, normalizeMenuSections } from './menu.js';

describe('menu API normalization', () => {
  it('keeps prices numeric and deep-copies menu items', () => {
    const payload = [{
      id: 'bevande',
      title: 'Bevande',
      items: [
        { id: 'acqua', name: 'Acqua', notes: ['50 cl'], price: '2,50 €' },
        { id: 'caffe', name: 'Caffè', notes: [], price: 2 }
      ]
    }];

    const normalized = normalizeMenuSections(payload);

    expect(normalized[0].items[0].price).toBe(2.5);
    expect(normalized[0].items[1].price).toBe(2);
    expect(typeof normalized[0].items[0].price).toBe('number');
    expect(normalized).not.toBe(payload);
    expect(normalized[0].items).not.toBe(payload[0].items);
    expect(normalized[0].items[0].notes).not.toBe(payload[0].items[0].notes);
    expect(payload[0].items[0].price).toBe('2,50 €');
  });

  it('maps legacy empty markers to no price and rejects corrupt values', () => {
    expect(normalizeApiPrice(null)).toBeNull();
    expect(normalizeApiPrice('')).toBeNull();
    expect(normalizeApiPrice('-')).toBeNull();
    expect(() => normalizeApiPrice('NaN')).toThrow(/Prezzo API non valido/);
    expect(() => normalizeApiPrice('5 € / 20 €')).toThrow(/Prezzo API non valido/);
  });
});
