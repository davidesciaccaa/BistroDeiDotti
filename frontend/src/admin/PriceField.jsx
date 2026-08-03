import { useEffect, useRef, useState } from 'react';
import { formatEuro, parsePriceInput } from '../utils/price.js';

export function PriceInput({ value, onChange, itemName, required = false }) {
  const parsed = parsePriceInput(value);
  const invalid = Number.isNaN(parsed) || (required && parsed === null);

  return (
    <div className={`admin-currency-input ${invalid ? 'admin-currency-input--invalid' : ''}`}>
      <input
        className="admin-field__input admin-currency-input__control"
        type="text"
        inputMode="decimal"
        autoComplete="off"
        value={value ?? ''}
        aria-label={itemName ? `Prezzo di ${itemName}` : 'Prezzo'}
        aria-invalid={invalid}
        placeholder="0,00"
        onChange={(event) => onChange(event.target.value.replace(/[^\d.,]/g, ''))}
      />
      <span className="admin-currency-input__suffix" aria-hidden="true">€</span>
    </div>
  );
}

export function PriceField({ value, isDirty, itemName, subcategory, sectionTitle, onChange }) {
  const [isEditing, setIsEditing] = useState(false);
  const [draft, setDraft] = useState(value == null ? '' : String(value));
  const [error, setError] = useState(null);
  const inputRef = useRef(null);
  const context = [sectionTitle, subcategory].filter(Boolean).join(' · ');

  useEffect(() => {
    if (isEditing) {
      inputRef.current?.focus();
      inputRef.current?.select();
    }
  }, [isEditing]);

  function commit() {
    const numeric = parsePriceInput(draft);
    if (Number.isNaN(numeric) || numeric === null) {
      setError('Inserisci un importo numerico con al massimo due decimali.');
      return;
    }
    onChange(numeric);
    setIsEditing(false);
    setError(null);
  }

  function cancel() {
    setDraft(value == null ? '' : String(value));
    setError(null);
    setIsEditing(false);
  }

  if (!isEditing) {
    return (
      <button
        type="button"
        className={`admin-price ${isDirty ? 'admin-price--dirty' : ''}`}
        onClick={() => {
          setDraft(value == null ? '' : String(value));
          setError(null);
          setIsEditing(true);
        }}
        aria-label={`Modifica il prezzo di ${itemName}`}
      >
        <span className="admin-price__value">{formatEuro(value) || '—'}</span>
        <span aria-hidden="true">✎</span>
      </button>
    );
  }

  return (
    <div className="admin-price-editor">
      <p className="admin-price-editor__context">{context && `${context} · `}{itemName}</p>
      <div className="admin-price-editor__row">
        <input
          ref={inputRef}
          className={`admin-price-editor__input ${error ? 'admin-price-editor__input--invalid' : ''}`}
          type="text"
          inputMode="decimal"
          value={draft}
          aria-label={`Prezzo di ${itemName}`}
          aria-invalid={Boolean(error)}
          onChange={(event) => {
            setDraft(event.target.value.replace(/[^\d.,]/g, ''));
            setError(null);
          }}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault();
              commit();
            }
            if (event.key === 'Escape') {
              event.preventDefault();
              cancel();
            }
          }}
        />
        <span className="admin-price-editor__currency" aria-hidden="true">€</span>
        <button type="button" className="admin-icon-button admin-icon-button--confirm" onClick={commit} aria-label={`Salva il prezzo di ${itemName}`}>✓</button>
        <button type="button" className="admin-icon-button" onClick={cancel} aria-label="Annulla">×</button>
      </div>
      {error && <p className="admin-price-editor__error" role="alert">{error}</p>}
    </div>
  );
}
