export function createMenuItemDraft(item, initialSectionId) {
  return {
    sectionId: initialSectionId,
    name: item?.name ?? '',
    subtitle: item?.subtitle ?? '',
    description: item?.description ?? '',
    price: item?.price == null ? '' : String(item.price),
    notesText: Array.isArray(item?.notes) ? item.notes.join('\n') : ''
  };
}
