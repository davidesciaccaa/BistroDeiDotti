import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AdminMenuEditor } from './AdminMenuEditor.jsx';

const api = vi.hoisted(() => ({
  adminLogout: vi.fn(),
  backfillAdminMenuTranslations: vi.fn(),
  createAdminMenuItem: vi.fn(),
  deleteAdminMenuItem: vi.fn(),
  fetchAdminMenuSections: vi.fn(),
  saveAdminPrices: vi.fn(),
  updateAdminMenuItem: vi.fn()
}));

vi.mock('../api/adminApi.js', () => api);

const initialSections = [{
  id: 'antipasti',
  title: 'Antipasti',
  description: '',
  items: [{
    id: 'piatto-test',
    name: 'Piatto test',
    subtitle: '',
    description: '',
    notes: ['Nota'],
    price: 25
  }]
}];

beforeEach(() => {
  Object.values(api).forEach((mock) => mock.mockReset());
  api.fetchAdminMenuSections.mockResolvedValue(structuredClone(initialSections));
  api.updateAdminMenuItem.mockImplementation(async (id, payload) => [{
    ...initialSections[0],
    items: [{ ...initialSections[0].items[0], id, ...payload }]
  }]);
  api.createAdminMenuItem.mockResolvedValue(structuredClone(initialSections));
  api.backfillAdminMenuTranslations.mockResolvedValue({
    updatedItems: 1,
    completeItems: 0,
    sections: structuredClone(initialSections)
  });
});

afterEach(cleanup);

describe('AdminMenuEditor edit requests', () => {
  it('does not call a mutation API on cancel and can save numerically afterwards', async () => {
    const user = userEvent.setup();
    const onSignedOut = vi.fn();
    render(<AdminMenuEditor onSignedOut={onSignedOut} />);

    const heading = await screen.findByRole('heading', { name: 'Piatto test' });
    const article = heading.closest('article');
    expect(article).toBeTruthy();
    const editButton = within(article).getByRole('button', { name: 'Modifica' });

    await user.click(editButton);
    let input = screen.getByRole('textbox', { name: 'Prezzo di Piatto test' });
    await user.clear(input);
    await user.type(input, '99,99');
    await user.click(screen.getByRole('button', { name: 'Annulla' }));

    expect(api.updateAdminMenuItem).not.toHaveBeenCalled();
    expect(api.createAdminMenuItem).not.toHaveBeenCalled();
    expect(api.saveAdminPrices).not.toHaveBeenCalled();
    expect(api.deleteAdminMenuItem).not.toHaveBeenCalled();
    expect(within(article).getByRole('button', { name: 'Modifica il prezzo di Piatto test' }).textContent)
      .toContain('25 €');

    await user.click(editButton);
    input = screen.getByRole('textbox', { name: 'Prezzo di Piatto test' });
    expect(input.value).toBe('25');
    await user.clear(input);
    await user.type(input, '18,50');
    await user.click(screen.getByRole('button', { name: 'Salva piatto' }));

    await waitFor(() => expect(api.updateAdminMenuItem).toHaveBeenCalledOnce());
    expect(api.updateAdminMenuItem).toHaveBeenCalledWith(
      'piatto-test',
      expect.objectContaining({ price: 18.5 })
    );
    expect(typeof api.updateAdminMenuItem.mock.calls[0][1].price).toBe('number');
    expect(await screen.findByText('18,5 €')).toBeTruthy();
  });
});

describe('AdminMenuEditor translations', () => {
  it('defaults new items to autoTranslate and preserves manual translations while editing', async () => {
    const user = userEvent.setup();
    api.fetchAdminMenuSections.mockResolvedValueOnce(structuredClone(initialSections));
    render(<AdminMenuEditor onSignedOut={vi.fn()} />);
    await screen.findByRole('heading', { name: 'Piatto test' });
    await user.click(screen.getAllByRole('button', { name: 'Aggiungi piatto' })[0]);
    expect(screen.getByRole('checkbox', { name: /Traduci automaticamente/ }).checked).toBe(true);
    await user.type(screen.getAllByRole('textbox', { name: 'Nome *' })[0], 'Nuovo');
    await user.click(screen.getByRole('button', { name: 'Salva piatto' }));
    await waitFor(() => expect(api.createAdminMenuItem).toHaveBeenCalledOnce());
    expect(api.createAdminMenuItem.mock.calls[0][0]).toEqual(expect.objectContaining({ autoTranslate: true }));
  });

  it('confirms backfill and reports counts', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    api.fetchAdminMenuSections.mockResolvedValueOnce(structuredClone(initialSections));
    render(<AdminMenuEditor onSignedOut={vi.fn()} />);
    await user.click(await screen.findByRole('button', { name: 'Genera traduzioni mancanti' }));
    expect(await screen.findByText(/1 voci; 0 erano già complete/)).toBeTruthy();
    expect(api.backfillAdminMenuTranslations).toHaveBeenCalledOnce();
  });
});
