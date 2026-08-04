import { parsePriceInput } from './price.js';

export function normalizeApiPrice(value) {
  if (value === null || value === undefined) return null;

  let price;
  if (typeof value === 'number') {
    price = value;
  } else if (typeof value === 'string') {
    const legacyValue = value.trim();
    if (legacyValue === '' || legacyValue === '-') return null;
    price = parsePriceInput(legacyValue);
  } else {
    price = Number.NaN;
  }

  if (!Number.isFinite(price) || price < 0) {
    throw new TypeError(`Prezzo API non valido: ${String(value)}`);
  }
  return price;
}

export function cloneMenuItem(item) {
  return {
    ...item,
    notes: Array.isArray(item?.notes) ? [...item.notes] : [],
    translations: normalizeTranslations(item?.translations)
  };
}

function normalizeTranslations(value) {
  if (value == null) return {};
  if (typeof value !== 'object' || Array.isArray(value)) throw new TypeError('Traduzioni menu non valide.');
  return Object.fromEntries(['en', 'de'].filter((language) => value[language] != null).map((language) => {
    const translated = value[language];
    if (typeof translated !== 'object' || !Array.isArray(translated.notes ?? [])) {
      throw new TypeError(`Traduzione ${language} non valida.`);
    }
    return [language, {
      name: String(translated.name ?? ''),
      subtitle: String(translated.subtitle ?? ''),
      description: String(translated.description ?? ''),
      notes: (translated.notes ?? []).map((note) => String(note))
    }];
  }));
}

export function normalizeMenuSections(payload) {
  if (!Array.isArray(payload)) {
    throw new TypeError('La risposta del menù deve essere un elenco di sezioni.');
  }

  return payload.map((section) => {
    if (!section || !Array.isArray(section.items)) {
      throw new TypeError('Sezione del menù non valida.');
    }
    return {
      ...section,
      items: section.items.map((item) => ({
        ...cloneMenuItem(item),
        price: normalizeApiPrice(item?.price)
      }))
    };
  });
}
