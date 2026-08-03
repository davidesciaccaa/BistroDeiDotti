export function parsePriceInput(value) {
  const normalized = String(value ?? '').trim().replace(/[€\s]/g, '').replace(',', '.');
  if (normalized === '') return null;
  if (!/^\d+(?:\.\d{1,2})?$/.test(normalized)) return Number.NaN;
  return Number(normalized);
}

export function formatEuro(value) {
  const parsed = typeof value === 'number' ? value : parsePriceInput(value);
  if (parsed === null || !Number.isFinite(parsed) || parsed < 0) return '';
  return `${parsed.toLocaleString('it-IT', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  })} €`;
}
