import { describe, expect, it } from 'vitest';
import { formatEuro, parsePriceInput } from './price.js';

describe('price utilities', () => {
  it.each([
    ['12', 12],
    ['12.50', 12.5],
    ['12,50', 12.5],
    ['12,50 €', 12.5]
  ])('parses %s as a numeric price', (input, expected) => {
    expect(parsePriceInput(input)).toBe(expected);
  });

  it('keeps an empty temporary form value distinct from NaN', () => {
    expect(parsePriceInput('')).toBeNull();
    expect(parsePriceInput('  ')).toBeNull();
    expect(Number.isNaN(parsePriceInput('12,5,0'))).toBe(true);
  });

  it('formats integers and decimals without ever spelling NaN', () => {
    expect(formatEuro(12)).toBe('12 €');
    expect(formatEuro(12.5)).toBe('12,5 €');
    expect(formatEuro('12,50 €')).toBe('12,5 €');
    expect(formatEuro(Number.NaN)).toBe('');
    expect(formatEuro(Number.NaN)).not.toContain('NaN');
  });
});
