import { describe, expect, it } from 'vitest';
import { normalizeMenuLanguage, resolveMenuItemText } from './localizedMenu.js';

const item = {
  id: 'piatto', name: 'Piatto', subtitle: 'Specialità', description: 'Descrizione italiana', notes: ['Caldo'],
  translations: {
    en: { name: 'Dynamic dish', subtitle: 'Special', description: 'Dynamic description', notes: ['Hot'] },
    de: { name: 'Dynamisches Gericht', subtitle: 'Spezialität', description: 'Dynamische Beschreibung', notes: ['Heiß'] }
  }
};
const legacy = {
  'menu.items.piatto.name': 'Legacy dish',
  'menu.items.piatto.description': 'Legacy description',
  'menu.items.piatto.subtitle': 'Legacy special',
  'menu.items.piatto.notes.caldo': 'Legacy hot'
};
const t = (key, options) => legacy[key] ?? options?.defaultValue ?? '';
const i18n = { language: 'it', getResourceBundle: () => ({}) };

describe('dynamic menu localization', () => {
  it.each(['it', 'it-IT'])('renders API Italian for %s', (language) => {
    expect(resolveMenuItemText(item, { language, t, i18n })).toEqual({
      name: 'Piatto', subtitle: 'Specialità', description: 'Descrizione italiana', notes: ['Caldo']
    });
  });

  it.each([
    ['en', 'Dynamic dish'], ['en-US', 'Dynamic dish'], ['en-GB', 'Dynamic dish'],
    ['de', 'Dynamisches Gericht'], ['de-DE', 'Dynamisches Gericht']
  ])('prefers dynamic content for %s', (language, name) => {
    expect(resolveMenuItemText(item, { language, t, i18n }).name).toBe(name);
  });

  it('falls back to legacy and then Italian field by field', () => {
    const partial = { ...item, translations: { en: { name: '', notes: [] } } };
    expect(resolveMenuItemText(partial, { language: 'en', t, i18n })).toEqual({
      name: 'Legacy dish', subtitle: 'Legacy special', description: 'Legacy description', notes: ['Legacy hot']
    });
    const fallback = resolveMenuItemText({ ...partial, id: 'unknown' }, {
      language: 'en-US', t: (_key, options) => options?.defaultValue ?? '', i18n
    });
    expect(fallback.name).toBe('Piatto');
    expect(fallback.description).toBe('Descrizione italiana');
  });

  it('normalizes regional tags', () => {
    expect(['en-US', 'en-GB', 'de-DE', 'it-IT'].map(normalizeMenuLanguage)).toEqual(['en', 'en', 'de', 'it']);
  });
});
