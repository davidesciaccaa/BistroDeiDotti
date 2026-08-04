export function normalizeMenuLanguage(language) {
  const normalized = String(language || 'it').toLowerCase().split('-')[0];
  return ['it', 'en', 'de'].includes(normalized) ? normalized : 'it';
}

function staticSubtitle(item, t) {
  const subtitle = item.subtitle || '';
  if (!subtitle) return '';
  const candidates = [
    `menu.items.vini_labels.${subtitle.toLowerCase().replace(/ /g, '_')}`,
    `menu.items.distillati_labels.${subtitle.toLowerCase().replace(/ & /g, '_').replace(/, /g, '_').replace(/ /g, '_')}`,
    `menu.items.bevande_labels.${subtitle.toLowerCase().replace(/ /g, '_')}`,
    `menu.items.cocktails_labels.${subtitle.toLowerCase().replace(/ \/ /g, '_').replace(/ /g, '_')}`,
    `menu.items.${item.id}.subtitle`
  ];
  for (const key of candidates) {
    const translated = t(key, { defaultValue: '' });
    if (translated) return translated;
  }
  return subtitle;
}

function staticDescription(item, t, i18n, language) {
  const translated = t(`menu.items.${item.id}.description`, { defaultValue: '' });
  if (translated) return translated;
  let result = item.description || '';
  if (!(result.startsWith('(') || result.includes(','))) return result;
  const ingredients = i18n.getResourceBundle(language, 'translation')?.menu?.items?.cocktail_ingredients;
  const italian = i18n.getResourceBundle('it', 'translation')?.menu?.items?.cocktail_ingredients;
  if (!ingredients || !italian) return result;
  Object.keys(ingredients).sort((a, b) => b.length - a.length).forEach((key) => {
    const source = italian[key];
    if (source && result.toLowerCase().includes(source.toLowerCase())) {
      result = result.replace(new RegExp(source, 'gi'), ingredients[key]);
    }
  });
  return result;
}

function staticNote(item, note, t) {
  const noteKey = note.toLowerCase().replace(/ /g, '_');
  return t(`menu.items.${item.id}.notes.${noteKey}`, {
    defaultValue: t(`menu.items.common_notes.${noteKey}`, { defaultValue: note })
  });
}

export function resolveMenuItemText(item, { language, t, i18n }) {
  const resolved = normalizeMenuLanguage(language || i18n.resolvedLanguage || i18n.language || 'it');
  if (resolved === 'it') {
    return { name: item.name, subtitle: item.subtitle || '', description: item.description || '', notes: item.notes || [] };
  }
  const dynamic = item.translations?.[resolved];
  const italianNotes = Array.isArray(item.notes) ? item.notes : [];
  return {
    name: dynamic?.name || t(`menu.items.${item.id}.name`, { defaultValue: item.name }),
    subtitle: dynamic?.subtitle || staticSubtitle(item, t),
    description: dynamic?.description || staticDescription(item, t, i18n, resolved),
    notes: italianNotes.map((note, index) => dynamic?.notes?.[index] || staticNote(item, note, t))
  };
}
