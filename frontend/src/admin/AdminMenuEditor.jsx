import { useCallback, useEffect, useMemo, useState } from 'react';
import { adminLogout, createAdminMenuItem, deleteAdminMenuItem, fetchAdminMenuSections, saveAdminPrices, updateAdminMenuItem } from '../api/adminApi.js';
import { PriceField } from './PriceField.jsx';

/**
 * The public menu leaves the subtitle blank on items that continue the previous subcategory
 * (e.g. the wines after "Bianchi"). Carry it forward so every row can name its own group.
 */
function withEffectiveSubcategories(items) {
  let current = '';
  return items.map((item) => {
    if (item.subtitle) {
      current = item.subtitle;
    }
    return { ...item, effectiveSubtitle: current };
  });
}

export function AdminMenuEditor({ onSignedOut }) {
  const [sections, setSections] = useState([]);
  const [loadState, setLoadState] = useState('loading');
  const [loadError, setLoadError] = useState(null);
  const [drafts, setDrafts] = useState({});
  const [isSaving, setIsSaving] = useState(false);
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
      const loaded = await fetchAdminMenuSections();
      setSections(loaded);
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
    if (pendingCount === 0) {
      return undefined;
    }

    const warn = (event) => {
      event.preventDefault();
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [pendingCount]);

  const changePrice = useCallback((itemId, price) => {
    setFeedback(null);
    setDrafts((previous) => {
      const next = { ...previous };
      if (price === savedPrices[itemId]) {
        delete next[itemId];
      } else {
        next[itemId] = price;
      }
      return next;
    });
  }, [savedPrices]);

  async function handleSaveAll() {
    if (pendingCount === 0 || isSaving) {
      return;
    }

    setIsSaving(true);
    setFeedback(null);

    try {
      const updated = await saveAdminPrices(drafts);
      setSections(updated);
      setDrafts({});
      setFeedback({
        type: 'success',
        message: `${pendingCount === 1 ? 'Prezzo salvato' : `${pendingCount} prezzi salvati`}. Il menù pubblico è aggiornato.`
      });
    } catch (error) {
      if (error.status === 401) {
        onSignedOut();
        return;
      }
      setFeedback({ type: 'error', message: `Salvataggio non riuscito: ${error.message}` });
    } finally {
      setIsSaving(false);
    }
  }

  async function handleLogout() {
    if (pendingCount > 0 && !window.confirm('Ci sono modifiche non salvate. Uscire comunque?')) {
      return;
    }
    await adminLogout();
    onSignedOut();
  }

  async function editItem(item, sectionId) {
    const name = window.prompt('Nome del piatto', item.name); if (name === null) return;
    const subtitle = window.prompt('Sottotitolo (facoltativo)', item.subtitle || ''); if (subtitle === null) return;
    const description = window.prompt('Descrizione (facoltativa)', item.description || ''); if (description === null) return;
    const notes = window.prompt('Note, separate da virgole (facoltative)', (item.notes || []).join(', ')); if (notes === null) return;
    const price = window.prompt('Prezzo numerico (es. 12.50)', item.price ?? ''); if (price === null) return;
    const payload = { sectionId, name, subtitle, description, notes: notes.split(',').map((note) => note.trim()).filter(Boolean), price: price === '' ? null : Number(price.replace(',', '.')) };
    try { setSections(item.id === '__new__' ? await createAdminMenuItem(payload) : await updateAdminMenuItem(item.id, payload)); setFeedback({ type: 'success', message: item.id === '__new__' ? 'Piatto aggiunto.' : 'Piatto aggiornato.' }); } catch (error) { setFeedback({ type: 'error', message: error.message }); }
  }

  async function addItem(sectionId) {
    const name = window.prompt('Nome del nuovo piatto'); if (!name) return;
    await editItem({ id: '__new__', name, subtitle: '', description: '', notes: [], price: '' }, sectionId).catch(() => {});
  }

  return (
    <main className="admin-shell">
      {/* Sticky together, so the save confirmation is visible wherever the page is scrolled. */}
      <div className="admin-topbar-wrapper">
        <header className="admin-topbar">
          <div className="admin-topbar__identity">
            <p className="admin-eyebrow">Il Bistr&ograve; dei Dotti</p>
            <h1 className="admin-topbar__title">Gestione prezzi</h1>
          </div>

          <div className="admin-topbar__actions">
            <span className={`admin-pending ${pendingCount > 0 ? 'admin-pending--active' : ''}`}>
              {pendingCount === 0
                ? 'Nessuna modifica'
                : `${pendingCount} ${pendingCount === 1 ? 'modifica' : 'modifiche'} da salvare`}
            </span>
            <button
              type="button"
              className="admin-button admin-button--ghost"
              onClick={() => setDrafts({})}
              disabled={pendingCount === 0 || isSaving}
            >
              Annulla modifiche
            </button>
            <button
              type="button"
              className="admin-button admin-button--primary"
              onClick={handleSaveAll}
              disabled={pendingCount === 0 || isSaving}
            >
              {isSaving ? 'Salvataggio…' : 'Salva tutto'}
            </button>
            <button type="button" className="admin-button admin-button--quiet" onClick={handleLogout}>
              Esci
            </button>
          </div>
        </header>

        {feedback && (
          <p className={`admin-feedback admin-feedback--${feedback.type}`} role="status">
            {feedback.message}
          </p>
        )}
      </div>

      {loadState === 'loading' && <p className="admin-boot">Caricamento del menù…</p>}

      {loadState === 'error' && (
        <div className="admin-empty">
          <p className="admin-feedback admin-feedback--error" role="alert">{loadError}</p>
          <button type="button" className="admin-button admin-button--ghost" onClick={loadMenu}>
            Riprova
          </button>
        </div>
      )}

      {loadState === 'ready' && (
        <div className="admin-sections">
          {sections.map((section) => (
            <section className="admin-section" key={section.id}>
              <div className="admin-section__header">
                <p className="admin-eyebrow">{section.title}</p>
                <h2 className="admin-section__title">{section.title}</h2>
                {section.description && <p className="admin-section__description">{section.description}</p>}
                <button type="button" className="admin-button admin-button--ghost" onClick={() => addItem(section.id)}>Aggiungi piatto</button>
              </div>

              <div className="admin-item-list">
                {withEffectiveSubcategories(section.items).map((item) => {
                  const isDirty = Object.prototype.hasOwnProperty.call(drafts, item.id);
                  const value = isDirty ? drafts[item.id] : item.price;

                  return (
                    <article className={`admin-item ${isDirty ? 'admin-item--dirty' : ''}`} key={item.id}>
                      {item.subtitle && <p className="admin-item__subtitle">{item.subtitle}</p>}

                      <div className="admin-item__header">
                        <h3 className="admin-item__name">{item.name}</h3>
                        <div className="admin-item__actions"><button type="button" className="admin-button admin-button--ghost" onClick={() => editItem(item, section.id)}>Modifica</button><button type="button" className="admin-button admin-button--quiet" onClick={async () => { if (window.confirm(`Eliminare ${item.name}?`)) setSections(await deleteAdminMenuItem(item.id)); }}>Elimina</button></div>
                        <PriceField
                          value={value}
                          isDirty={isDirty}
                          itemName={item.name}
                          subcategory={item.effectiveSubtitle}
                          sectionTitle={section.title}
                          onChange={(price) => changePrice(item.id, price)}
                        />
                      </div>

                      {item.description && <p className="admin-item__description">{item.description}</p>}

                      {item.notes?.length > 0 && (
                        <ul className="admin-item__notes">
                          {item.notes.map((note) => <li key={note}>{note}</li>)}
                        </ul>
                      )}

                      {isDirty && (
                        <p className="admin-item__dirty-note">
                          Da <strong>{item.price}</strong> a <strong>{value}</strong> · in attesa di salvataggio
                        </p>
                      )}
                    </article>
                  );
                })}
              </div>
            </section>
          ))}
        </div>
      )}
    </main>
  );
}
