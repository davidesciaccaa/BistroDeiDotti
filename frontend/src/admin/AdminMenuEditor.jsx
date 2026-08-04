import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  adminLogout,
  backfillAdminMenuTranslations,
  createAdminMenuItem,
  deleteAdminMenuItem,
  fetchAdminMenuSections,
  saveAdminPrices,
  updateAdminMenuItem
} from '../api/adminApi.js';
import { formatEuro } from '../utils/price.js';
import { cloneMenuItem } from '../utils/menu.js';
import { MenuItemForm } from './MenuItemForm.jsx';
import { PriceField } from './PriceField.jsx';

function withEffectiveSubcategories(items) {
  let current = '';
  return items.map((item) => {
    if (item.subtitle) current = item.subtitle;
    return { ...item, effectiveSubtitle: current };
  });
}

export function AdminMenuEditor({ onSignedOut }) {
  const [sections, setSections] = useState([]);
  const [loadState, setLoadState] = useState('loading');
  const [loadError, setLoadError] = useState(null);
  const [drafts, setDrafts] = useState({});
  const [isSavingPrices, setIsSavingPrices] = useState(false);
  const [editor, setEditor] = useState(null);
  const [isSavingItem, setIsSavingItem] = useState(false);
  const [isBackfilling, setIsBackfilling] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [feedback, setFeedback] = useState(null);

  const savedPrices = useMemo(() => {
    const prices = {};
    sections.forEach((section) => section.items.forEach((item) => {
      prices[item.id] = item.price;
    }));
    return prices;
  }, [sections]);

  const pendingCount = Object.keys(drafts).length;

  const loadMenu = useCallback(async () => {
    setLoadState('loading');
    setLoadError(null);
    try {
      setSections(await fetchAdminMenuSections());
      setLoadState('ready');
    } catch (error) {
      if (error.status === 401) {
        onSignedOut();
        return;
      }
      setLoadError(error.message);
      setLoadState('error');
    }
  }, [onSignedOut]);

  useEffect(() => {
    loadMenu();
  }, [loadMenu]);

  useEffect(() => {
    if (pendingCount === 0) return undefined;
    const warn = (event) => {
      event.preventDefault();
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [pendingCount]);

  function handleApiError(error, prefix) {
    if (error.status === 401) {
      onSignedOut();
      return;
    }
    setFeedback({ type: 'error', message: `${prefix}: ${error.message}` });
  }

  function changePrice(itemId, price) {
    setFeedback(null);
    setDrafts((previous) => {
      const next = { ...previous };
      if (Number(price) === Number(savedPrices[itemId])) delete next[itemId];
      else next[itemId] = price;
      return next;
    });
  }

  async function handleSavePrices() {
    if (pendingCount === 0 || isSavingPrices) return;
    setIsSavingPrices(true);
    setFeedback(null);
    try {
      setSections(await saveAdminPrices(drafts));
      setDrafts({});
      setFeedback({
        type: 'success',
        message: `${pendingCount === 1 ? 'Prezzo salvato' : `${pendingCount} prezzi salvati`}. Il menù pubblico è aggiornato.`
      });
    } catch (error) {
      handleApiError(error, 'Salvataggio non riuscito');
    } finally {
      setIsSavingPrices(false);
    }
  }

  async function handleItemSubmit(payload) {
    setIsSavingItem(true);
    setFeedback(null);
    try {
      const updated = editor.item
        ? await updateAdminMenuItem(editor.item.id, payload)
        : await createAdminMenuItem(payload);
      setSections(updated);
      setDrafts({});
      setEditor(null);
      setFeedback({
        type: 'success',
        message: payload.autoTranslate
          ? 'Traduzione completata. Il menù pubblico è aggiornato.'
          : 'Traduzione manuale salvata. Il menù pubblico è aggiornato.'
      });
    } catch (error) {
      handleApiError(error, 'Operazione non riuscita');
    } finally {
      setIsSavingItem(false);
    }
  }

  async function handleDelete(item) {
    if (!window.confirm(`Eliminare definitivamente “${item.name}”?`)) return;
    setDeletingId(item.id);
    setFeedback(null);
    try {
      setSections(await deleteAdminMenuItem(item.id));
      setDrafts((previous) => {
        const next = { ...previous };
        delete next[item.id];
        return next;
      });
      setFeedback({ type: 'success', message: 'Piatto eliminato correttamente.' });
    } catch (error) {
      handleApiError(error, 'Eliminazione non riuscita');
    } finally {
      setDeletingId(null);
    }
  }

  async function handleBackfill() {
    if (isBackfilling || !window.confirm('Generare soltanto le traduzioni mancanti senza sovrascrivere quelle esistenti?')) return;
    setIsBackfilling(true);
    setFeedback(null);
    try {
      const result = await backfillAdminMenuTranslations();
      setSections(result.sections);
      setFeedback({
        type: 'success',
        message: `Traduzioni generate per ${result.updatedItems} voci; ${result.completeItems} erano già complete.`
      });
    } catch (error) {
      handleApiError(error, 'Generazione non riuscita');
    } finally {
      setIsBackfilling(false);
    }
  }

  async function handleLogout() {
    if (pendingCount > 0 && !window.confirm('Ci sono prezzi non salvati. Uscire comunque?')) return;
    await adminLogout();
    onSignedOut();
  }

  return (
    <main className="admin-shell">
      <div className="admin-topbar-wrapper">
        <header className="admin-topbar">
          <div className="admin-topbar__identity">
            <p className="admin-eyebrow">Il Bistr&ograve; dei Dotti</p>
            <h1 className="admin-topbar__title">Gestione menù</h1>
          </div>

          <div className="admin-topbar__actions">
            <button type="button" className="admin-button admin-button--ghost"
              onClick={handleBackfill} disabled={isBackfilling || isSavingItem}>
              {isBackfilling ? 'Generazione in corso…' : 'Genera traduzioni mancanti'}
            </button>
            <span className={`admin-pending ${pendingCount > 0 ? 'admin-pending--active' : ''}`}>
              {pendingCount === 0 ? 'Tutto salvato' : `${pendingCount} ${pendingCount === 1 ? 'prezzo' : 'prezzi'} da salvare`}
            </span>
            <button type="button" className="admin-button admin-button--ghost" onClick={() => setDrafts({})} disabled={pendingCount === 0 || isSavingPrices}>Annulla prezzi</button>
            <button type="button" className="admin-button admin-button--primary" onClick={handleSavePrices} disabled={pendingCount === 0 || isSavingPrices}>{isSavingPrices ? 'Salvataggio…' : 'Salva prezzi'}</button>
            <button type="button" className="admin-button admin-button--quiet" onClick={handleLogout}>Esci</button>
          </div>
        </header>
        {feedback && <p className={`admin-feedback admin-feedback--${feedback.type}`} role="status">{feedback.message}</p>}
      </div>

      {loadState === 'loading' && <p className="admin-boot">Caricamento del menù…</p>}
      {loadState === 'error' && (
        <div className="admin-empty">
          <p className="admin-feedback admin-feedback--error" role="alert">{loadError}</p>
          <button type="button" className="admin-button admin-button--ghost" onClick={loadMenu}>Riprova</button>
        </div>
      )}

      {loadState === 'ready' && (
        <div className="admin-sections">
          {sections.map((section) => (
            <section className="admin-section" key={section.id}>
              <div className="admin-section__header admin-section__header--actions">
                <div>
                  <p className="admin-eyebrow">{section.title}</p>
                  <h2 className="admin-section__title">{section.title}</h2>
                  {section.description && <p className="admin-section__description">{section.description}</p>}
                </div>
                <button type="button" className="admin-button admin-button--ghost" onClick={() => setEditor({ sectionId: section.id, item: null })}>Aggiungi piatto</button>
              </div>

              <div className="admin-item-list">
                {withEffectiveSubcategories(section.items).map((item) => {
                  const isDirty = Object.prototype.hasOwnProperty.call(drafts, item.id);
                  const value = isDirty ? drafts[item.id] : item.price;
                  return (
                    <article className={`admin-item ${isDirty ? 'admin-item--dirty' : ''}`} key={item.id}>
                      {item.subtitle && <p className="admin-item__subtitle">{item.subtitle}</p>}
                      <div className="admin-item__header">
                        <div>
                          <h3 className="admin-item__name">{item.name}</h3>
                          {item.description && <p className="admin-item__description">{item.description}</p>}
                        </div>
                        <div className="admin-item__controls">
                          <PriceField value={value} isDirty={isDirty} itemName={item.name} subcategory={item.effectiveSubtitle} sectionTitle={section.title} onChange={(price) => changePrice(item.id, price)} />
                          <button type="button" className="admin-button admin-button--ghost admin-button--compact" onClick={() => setEditor({ sectionId: section.id, item: cloneMenuItem(item) })}>Modifica</button>
                          <button type="button" className="admin-button admin-button--quiet admin-button--compact" disabled={deletingId === item.id} onClick={() => handleDelete(item)}>{deletingId === item.id ? 'Eliminazione…' : 'Elimina'}</button>
                        </div>
                      </div>
                      {item.notes?.length > 0 && <ul className="admin-item__notes">{item.notes.map((note) => <li key={note}>{note}</li>)}</ul>}
                      {isDirty && <p className="admin-item__dirty-note">Da <strong>{formatEuro(item.price)}</strong> a <strong>{formatEuro(value)}</strong> · in attesa di salvataggio</p>}
                    </article>
                  );
                })}
                {section.items.length === 0 && <p className="admin-section__empty">Nessun piatto in questa categoria.</p>}
              </div>
            </section>
          ))}
        </div>
      )}

      {editor && (
        <MenuItemForm
          sections={sections}
          initialSectionId={editor.sectionId}
          item={editor.item}
          onCancel={() => setEditor(null)}
          onSubmit={handleItemSubmit}
          isSaving={isSavingItem}
        />
      )}
    </main>
  );
}
