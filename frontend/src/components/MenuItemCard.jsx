import { useTranslation } from 'react-i18next';
import { formatEuro } from '../utils/price.js';
import { resolveMenuItemText } from '../utils/localizedMenu.js';

export function MenuItemCard({ item }) {
  const { t, i18n } = useTranslation();

  const { name, subtitle, description, notes } = resolveMenuItemText(item, { t, i18n });
  const price = formatEuro(item.price);

  return (
    <article className="menu-item-editorial">
      {subtitle && (
        <p className="menu-item-editorial__subtitle">{subtitle}</p>
      )}
      
      <div className="menu-item-editorial__header">
        <h4>{name}</h4>
        <span className="menu-item-editorial__price">{price}</span>
      </div>

      {description && (
        <p className="menu-item-editorial__description">{description}</p>
      )}

      {notes && notes.length > 0 && (
        <ul className="note-list-editorial" aria-label={t('menu.aria.notes', { name })}>
          {notes.map((note) => (
            <li key={note}>{note}</li>
          ))}
        </ul>
      )}
    </article>
  );
}
