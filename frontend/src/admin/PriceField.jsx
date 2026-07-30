import { useEffect, useRef, useState } from 'react';

const PRICE_PATTERN = /^\d+(?:[.,]\d{1,2})?$/;

export function PriceField({ value, isDirty, itemName, subcategory, sectionTitle, onChange }) {
  const initial = value == null ? '' : String(value);
  const [isEditing, setIsEditing] = useState(false);
  const [draft, setDraft] = useState(initial);
  const [error, setError] = useState(null);
  const inputRef = useRef(null);
  const context = [sectionTitle, subcategory].filter(Boolean).join(' · ');
  useEffect(() => { if (isEditing) { inputRef.current?.focus(); inputRef.current?.select(); } }, [isEditing]);
  function start() { setDraft(value == null ? '' : String(value)); setError(null); setIsEditing(true); }
  function commit() {
    const numeric = draft.trim().replace(',', '.');
    if (!PRICE_PATTERN.test(draft.trim())) { setError('Inserisci solo un importo numerico, con al massimo due decimali.'); return; }
    onChange(numeric); setIsEditing(false); setError(null);
  }
  if (!isEditing) return <button type="button" className={`admin-price ${isDirty ? 'admin-price--dirty' : ''}`} onClick={start} aria-label={`Modifica il prezzo di ${itemName}`}><span className="admin-price__value">{value == null ? '—' : `${value} €`}</span><span aria-hidden="true">✎</span></button>;
  return <div className="admin-price-editor"><p className="admin-price-editor__context">{context && `${context} · `}{itemName}</p><div className="admin-price-editor__row"><input ref={inputRef} className={`admin-price-editor__input ${error ? 'admin-price-editor__input--invalid' : ''}`} type="text" inputMode="decimal" value={draft} aria-label={`Prezzo di ${itemName}`} aria-invalid={Boolean(error)} onChange={(event) => { setDraft(event.target.value); setError(null); }} onKeyDown={(event) => { if (event.key === 'Enter') commit(); if (event.key === 'Escape') setIsEditing(false); }} /><span className="admin-price-editor__currency" aria-hidden="true">€</span><button type="button" className="admin-icon-button admin-icon-button--confirm" onClick={commit} aria-label={`Salva il prezzo di ${itemName}`}>✓</button><button type="button" className="admin-icon-button" onClick={() => setIsEditing(false)} aria-label="Annulla">×</button></div>{error && <p className="admin-price-editor__error" role="alert">{error}</p>}</div>;
}
