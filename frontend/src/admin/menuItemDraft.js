export function createMenuItemDraft(item, initialSectionId) {
  return {
    sectionId: initialSectionId,
    name: item?.name ?? '',
    subtitle: item?.subtitle ?? '',
    description: item?.description ?? '',
    price: item?.price == null ? '' : String(item.price),
    notesText: Array.isArray(item?.notes) ? item.notes.join('\n') : '',
    translations: Object.fromEntries(['en', 'de'].map((language) => {
      const value = item?.translations?.[language] ?? {};
      return [language, {
        name: value.name ?? '',
        subtitle: value.subtitle ?? '',
        description: value.description ?? '',
        notesText: Array.isArray(value.notes) ? value.notes.join('\n') : ''
      }];
    })),
    autoTranslate: !item
  };
}

export function menuItemPayload(form) {
  const notes = form.notesText.split('\n').map((note) => note.trim()).filter(Boolean);
  const payload = {
    sectionId: form.sectionId,
    name: form.name.trim(),
    subtitle: form.subtitle.trim(),
    description: form.description.trim(),
    notes,
    autoTranslate: form.autoTranslate
  };
  if (!form.autoTranslate) {
    payload.translations = Object.fromEntries(['en', 'de'].map((language) => {
      const translated = form.translations[language];
      const lines = translated.notesText.split('\n');
      return [language, {
        name: translated.name.trim(),
        subtitle: translated.subtitle.trim(),
        description: translated.description.trim(),
        notes: notes.length === 0 ? [] : Array.from({ length: notes.length }, (_, index) =>
          (lines[index] ?? '').trim())
      }];
    }));
  }
  return payload;
}
