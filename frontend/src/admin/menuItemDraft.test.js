import { describe, expect, it } from 'vitest';
import { createMenuItemDraft, menuItemPayload } from './menuItemDraft.js';

describe('translated menu item drafts', () => {
  it('defaults new items to automatic translation', () => {
    const draft = createMenuItemDraft(null, 'antipasti');
    draft.name = 'Nuovo piatto';
    expect(menuItemPayload(draft)).toEqual(expect.objectContaining({
      name: 'Nuovo piatto', autoTranslate: true
    }));
    expect(menuItemPayload(draft).translations).toBeUndefined();
  });

  it('keeps existing translations and builds a manual payload', () => {
    const draft = createMenuItemDraft({
      name: 'Piatto', subtitle: '', description: '', notes: ['Caldo'], price: 12,
      translations: {
        en: { name: 'Dish', subtitle: '', description: 'Description', notes: ['Hot'] },
        de: { name: 'Gericht', subtitle: '', description: 'Beschreibung', notes: ['Heiß'] }
      }
    }, 'antipasti');
    expect(draft.autoTranslate).toBe(false);
    expect(menuItemPayload(draft)).toEqual(expect.objectContaining({
      autoTranslate: false,
      translations: {
        en: { name: 'Dish', subtitle: '', description: 'Description', notes: ['Hot'] },
        de: { name: 'Gericht', subtitle: '', description: 'Beschreibung', notes: ['Heiß'] }
      }
    }));
  });
});
