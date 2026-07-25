import { useTranslation } from 'react-i18next';

export function MenuItemCard({ item }) {
  const { t, i18n } = useTranslation();

  // Helper to translate known terms in descriptions (especially for cocktails)
  const translateDescription = (description, itemId) => {
    const translatedDesc = t(`menu.items.${itemId}.description`, { defaultValue: description });
    
    // If it's a cocktail and we are using the default value, try to translate ingredients
    if (translatedDesc === description && (description.startsWith('(') || description.includes(','))) {
      let result = description;
      const ingredients = i18n.getResourceBundle(i18n.language, 'translation')?.menu?.items?.cocktail_ingredients;
      
      if (ingredients) {
        // Sort keys by length descending to avoid partial replacements (e.g., 'arancia' vs 'succo d'arancia')
        const sortedKeys = Object.keys(ingredients).sort((a, b) => b.length - a.length);
        
        for (const key of sortedKeys) {
          const italianTerm = i18n.getResourceBundle('it', 'translation')?.menu?.items?.cocktail_ingredients?.[key];
          if (italianTerm && result.toLowerCase().includes(italianTerm.toLowerCase())) {
            const regex = new RegExp(italianTerm, 'gi');
            result = result.replace(regex, ingredients[key]);
          }
        }
      }
      return result;
    }
    
    return translatedDesc;
  };

  // Helper to translate subtitles (especially for wines)
  const translateSubtitle = (subtitle) => {
    if (!subtitle) return '';
    
    // Check specific labels first
    const viniLabelKey = subtitle.toLowerCase().replace(/ /g, '_');
    const viniTranslated = t(`menu.items.vini_labels.${viniLabelKey}`, { defaultValue: '' });
    if (viniTranslated) return viniTranslated;

    const spiritsLabelKey = subtitle.toLowerCase()
      .replace(/ & /g, '_')
      .replace(/, /g, '_')
      .replace(/ /g, '_');
    const spiritsTranslated = t(`menu.items.distillati_labels.${spiritsLabelKey}`, { defaultValue: '' });
    if (spiritsTranslated) return spiritsTranslated;

    const bevandeLabelKey = subtitle.toLowerCase().replace(/ /g, '_');
    const bevandeTranslated = t(`menu.items.bevande_labels.${bevandeLabelKey}`, { defaultValue: '' });
    if (bevandeTranslated) return bevandeTranslated;

    const cocktailLabelKey = subtitle.toLowerCase().replace(/ \/ /g, '_').replace(/ /g, '_');
    const cocktailTranslated = t(`menu.items.cocktails_labels.${cocktailLabelKey}`, { defaultValue: '' });
    if (cocktailTranslated) return cocktailTranslated;

    return t(`menu.items.${item.id}.subtitle`, { defaultValue: subtitle });
  };

  const name = t(`menu.items.${item.id}.name`, { defaultValue: item.name });
  const subtitle = translateSubtitle(item.subtitle);
  const description = translateDescription(item.description, item.id);
  const notes = item.notes.map(note => {
    const noteKey = note.toLowerCase().replace(/ /g, '_');
    return t(`menu.items.${item.id}.notes.${noteKey}`, { 
      defaultValue: t(`menu.items.common_notes.${noteKey}`, { defaultValue: note }) 
    });
  });

  return (
    <article className="menu-item-editorial">
      {subtitle && (
        <p className="menu-item-editorial__subtitle">{subtitle}</p>
      )}
      
      <div className="menu-item-editorial__header">
        <h4>{name}</h4>
        <span className="menu-item-editorial__price">{item.price}</span>
      </div>

      {description && (
        <p className="menu-item-editorial__description">{description}</p>
      )}

      {notes && notes.length > 0 && (
        <ul className="note-list-editorial" aria-label={`Note di ${name}`}>
          {notes.map((note) => (
            <li key={note}>{note}</li>
          ))}
        </ul>
      )}
    </article>
  );
}
