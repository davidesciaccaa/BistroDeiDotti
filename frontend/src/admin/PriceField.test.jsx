import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PriceField } from './PriceField.jsx';

afterEach(cleanup);

describe('PriceField cancellation', () => {
  it('does not publish a temporary price when editing is cancelled', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<PriceField value={25} itemName="Piatto test" onChange={onChange} />);

    await user.click(screen.getByRole('button', { name: 'Modifica il prezzo di Piatto test' }));
    const input = screen.getByRole('textbox', { name: 'Prezzo di Piatto test' });
    await user.clear(input);
    await user.type(input, '99,99');
    await user.click(screen.getByRole('button', { name: 'Annulla' }));

    expect(onChange).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Modifica il prezzo di Piatto test' }).textContent)
      .toContain('25 €');
  });

  it('can be temporarily empty and still restores the original price on Escape', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<PriceField value={2.5} itemName="Acqua" onChange={onChange} />);

    await user.click(screen.getByRole('button', { name: 'Modifica il prezzo di Acqua' }));
    const input = screen.getByRole('textbox', { name: 'Prezzo di Acqua' });
    await user.clear(input);
    expect(input.value).toBe('');
    await user.keyboard('{Escape}');

    expect(onChange).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Modifica il prezzo di Acqua' }).textContent)
      .toContain('2,5 €');
  });
});
