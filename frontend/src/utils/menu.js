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
    notes: Array.isArray(item?.notes) ? [...item.notes] : []
  };
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
