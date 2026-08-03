import { useState } from 'react';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from '../i18n.js';
import { MenuItemCard } from '../components/MenuItemCard.jsx';
import { MenuItemForm } from './MenuItemForm.jsx';
import { createMenuItemDraft } from './menuItemDraft.js';

const sections = [{ id: 'antipasti', title: 'Antipasti', items: [] }];
const originalItem = {
  id: 'piatto-test',
  name: 'Piatto test',
  subtitle: '',
  description: '',
  notes: ['Nota originale'],
  price: 25
};

function EditingHarness({ onSave }) {
  const [item, setItem] = useState(originalItem);
  const [isEditing, setIsEditing] = useState(false);

  async function save(payload) {
    onSave(payload);
    setItem((previous) => ({ ...previous, ...payload }));
    setIsEditing(false);
  }

  return (
    <>
      <MenuItemCard item={item} />
      <button type="button" onClick={() => setIsEditing(true)}>Modifica piatto</button>
      {isEditing && (
        <MenuItemForm
          sections={sections}
          initialSectionId="antipasti"
          item={item}
          onCancel={() => setIsEditing(false)}
          onSubmit={save}
          isSaving={false}
        />
      )}
    </>
  );
}

beforeAll(async () => {
  await i18n.changeLanguage('it');
});

afterEach(cleanup);

describe('menu item editing flow', () => {
  it('builds a draft independent from the original menu item', () => {
    const source = { ...originalItem, notes: [...originalItem.notes] };
    const snapshot = structuredClone(source);
    const draft = createMenuItemDraft(source, 'antipasti');

    draft.price = '99';
    draft.notesText = 'Nota cambiata';

    expect(source).toEqual(snapshot);
    expect(draft.price).toBe('99');
  });

  it('leaves the public menu untouched after opening and cancelling', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<EditingHarness onSave={onSave} />);

    expect(screen.getByText('25 €')).toBeTruthy();
    await user.click(screen.getByRole('button', { name: 'Modifica piatto' }));
    expect(screen.getByRole('textbox', { name: 'Prezzo di Piatto test' }).value).toBe('25');
    await user.click(screen.getByRole('button', { name: 'Annulla' }));

    expect(onSave).not.toHaveBeenCalled();
    expect(screen.getByText('25 €')).toBeTruthy();
    expect(document.body.textContent).not.toContain('NaN');
  });

  it('discards a temporary decimal and a temporarily empty value', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<EditingHarness onSave={onSave} />);

    await user.click(screen.getByRole('button', { name: 'Modifica piatto' }));
    let input = screen.getByRole('textbox', { name: 'Prezzo di Piatto test' });
    await user.clear(input);
    await user.type(input, '99,99');
    await user.click(screen.getByRole('button', { name: 'Annulla' }));

    await user.click(screen.getByRole('button', { name: 'Modifica piatto' }));
    input = screen.getByRole('textbox', { name: 'Prezzo di Piatto test' });
    expect(input.value).toBe('25');
    await user.clear(input);
    expect(input.value).toBe('');
    await user.click(screen.getByRole('button', { name: 'Annulla' }));

    expect(onSave).not.toHaveBeenCalled();
    expect(screen.getByText('25 €')).toBeTruthy();
    expect(document.body.textContent).not.toContain('NaN');
  });

  it.each([
    ['12', 12, '12 €'],
    ['12,50', 12.5, '12,5 €']
  ])('submits %s as a number and renders it publicly', async (typed, numeric, displayed) => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<EditingHarness onSave={onSave} />);

    await user.click(screen.getByRole('button', { name: 'Modifica piatto' }));
    const input = screen.getByRole('textbox', { name: 'Prezzo di Piatto test' });
    await user.clear(input);
    await user.type(input, typed);
    await user.click(screen.getByRole('button', { name: 'Salva piatto' }));

    await waitFor(() => expect(onSave).toHaveBeenCalledOnce());
    expect(onSave.mock.calls[0][0].price).toBe(numeric);
    expect(typeof onSave.mock.calls[0][0].price).toBe('number');
    expect(screen.getByText(displayed)).toBeTruthy();
  });

  it('saves a valid price after an earlier edit was cancelled', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<EditingHarness onSave={onSave} />);

    await user.click(screen.getByRole('button', { name: 'Modifica piatto' }));
    let input = screen.getByRole('textbox', { name: 'Prezzo di Piatto test' });
    await user.clear(input);
    await user.type(input, '99');
    await user.click(screen.getByRole('button', { name: 'Annulla' }));

    await user.click(screen.getByRole('button', { name: 'Modifica piatto' }));
    input = screen.getByRole('textbox', { name: 'Prezzo di Piatto test' });
    expect(input.value).toBe('25');
    await user.clear(input);
    await user.type(input, '18,50');
    await user.click(screen.getByRole('button', { name: 'Salva piatto' }));

    await waitFor(() => expect(onSave).toHaveBeenCalledOnce());
    expect(onSave.mock.calls[0][0].price).toBe(18.5);
    expect(screen.getByText('18,5 €')).toBeTruthy();
    expect(document.body.textContent).not.toContain('NaN');
  });
});
