import { useEffect, useState } from 'react';
import { parsePriceInput } from '../utils/price.js';
import { createMenuItemDraft, menuItemPayload } from './menuItemDraft.js';
import { PriceInput } from './PriceField.jsx';

export function MenuItemForm({ sections, initialSectionId, item, onCancel, onSubmit, isSaving }) {
  const [form, setForm] = useState(() => createMenuItemDraft(item, initialSectionId));
  const [error, setError] = useState(null);

  useEffect(() => {
    const closeOnEscape = (event) => {
      if (event.key === 'Escape' && !isSaving) onCancel();
    };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, [isSaving, onCancel]);

  function change(field, value) {
    setForm((previous) => ({ ...previous, [field]: value }));
    setError(null);
  }

  function changeTranslation(language, field, value) {
    setForm((previous) => ({
      ...previous,
      translations: {
        ...previous.translations,
        [language]: { ...previous.translations[language], [field]: value }
      }
    }));
    setError(null);
  }

  async function submit(event) {
    event.preventDefault();
    const price = parsePriceInput(form.price);
    if (!form.name.trim()) {
      setError('Il nome del piatto è obbligatorio.');
      return;
    }
    if (Number.isNaN(price)) {
      setError('Il prezzo deve essere un numero con al massimo due decimali.');
      return;
    }
    await onSubmit({ ...menuItemPayload(form), price });
  }

  return (
    <div className="admin-modal-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget && !isSaving) onCancel();
    }}>
      <section className="admin-modal" role="dialog" aria-modal="true" aria-labelledby="menu-item-form-title">
        <div className="admin-modal__header">
          <div>
            <p className="admin-eyebrow">{item ? 'Modifica' : 'Nuovo piatto'}</p>
            <h2 id="menu-item-form-title" className="admin-modal__title">{item ? item.name : 'Aggiungi al menù'}</h2>
          </div>
          <button type="button" className="admin-icon-button" onClick={onCancel} disabled={isSaving} aria-label="Chiudi">×</button>
        </div>

        <form className="admin-menu-form" onSubmit={submit}>
          <fieldset className="admin-language-fields admin-field--full">
            <legend>Italiano</legend>
          <label className="admin-field">
            <span className="admin-field__label">Nome *</span>
            <input className="admin-field__input" value={form.name} maxLength={160} autoFocus onChange={(event) => change('name', event.target.value)} />
          </label>

          <label className="admin-field">
            <span className="admin-field__label">Categoria *</span>
            <select className="admin-field__input" value={form.sectionId} onChange={(event) => change('sectionId', event.target.value)}>
              {sections.map((section) => <option key={section.id} value={section.id}>{section.title}</option>)}
            </select>
          </label>

          <label className="admin-field">
            <span className="admin-field__label">Sottotitolo</span>
            <input className="admin-field__input" value={form.subtitle} maxLength={120} onChange={(event) => change('subtitle', event.target.value)} />
          </label>

          <label className="admin-field admin-field--full">
            <span className="admin-field__label">Descrizione</span>
            <textarea className="admin-field__input admin-field__textarea" value={form.description} maxLength={1000} onChange={(event) => change('description', event.target.value)} />
          </label>
          <label className="admin-field admin-field--full">
            <span className="admin-field__label">Note</span>
            <textarea className="admin-field__input admin-field__textarea" value={form.notesText} placeholder="Una nota per riga" onChange={(event) => change('notesText', event.target.value)} />
          </label>
          </fieldset>

          <label className="admin-translation-toggle admin-field--full">
            <input type="checkbox" checked={form.autoTranslate}
              onChange={(event) => change('autoTranslate', event.target.checked)} />
            <span>Traduci automaticamente dall&apos;italiano</span>
          </label>
          {form.autoTranslate && <p className="admin-field__help admin-field--full">English e Deutsch saranno generati al salvataggio.</p>}

          {['en', 'de'].map((language) => (
            <fieldset className="admin-language-fields admin-field--full" disabled={form.autoTranslate} key={language}>
              <legend>{language === 'en' ? 'English' : 'Deutsch'}</legend>
              <label className="admin-field"><span className="admin-field__label">Nome</span>
                <input className="admin-field__input" maxLength={160} value={form.translations[language].name}
                  onChange={(event) => changeTranslation(language, 'name', event.target.value)} /></label>
              <label className="admin-field"><span className="admin-field__label">Sottotitolo</span>
                <input className="admin-field__input" maxLength={120} value={form.translations[language].subtitle}
                  onChange={(event) => changeTranslation(language, 'subtitle', event.target.value)} /></label>
              <label className="admin-field admin-field--full"><span className="admin-field__label">Descrizione</span>
                <textarea className="admin-field__input admin-field__textarea" maxLength={1000} value={form.translations[language].description}
                  onChange={(event) => changeTranslation(language, 'description', event.target.value)} /></label>
              <label className="admin-field admin-field--full"><span className="admin-field__label">Note (una per ogni nota italiana)</span>
                <textarea className="admin-field__input admin-field__textarea" value={form.translations[language].notesText}
                  onChange={(event) => changeTranslation(language, 'notesText', event.target.value)} /></label>
            </fieldset>
          ))}

          <label className="admin-field">
            <span className="admin-field__label">Prezzo</span>
            <PriceInput value={form.price} onChange={(value) => change('price', value)} itemName={form.name} />
            <span className="admin-field__help">Inserisci solo il numero; il simbolo € viene aggiunto automaticamente.</span>
          </label>

          {error && <p className="admin-feedback admin-feedback--error admin-field--full" role="alert">{error}</p>}

          <div className="admin-modal__actions admin-field--full">
            <button type="button" className="admin-button admin-button--ghost" onClick={onCancel} disabled={isSaving}>Annulla</button>
            <button type="submit" className="admin-button admin-button--primary" disabled={isSaving}>{isSaving ? (form.autoTranslate ? 'Traduzione in corso…' : 'Salvataggio…') : 'Salva piatto'}</button>
          </div>
        </form>
      </section>
    </div>
  );
}
