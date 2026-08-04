package com.angolodivino.menu;

import com.angolodivino.admin.MenuItemRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MenuService {
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "de");
    private final MenuOverridesStore store;
    private final MenuTranslationService translationService;

    @Autowired
    public MenuService(MenuOverridesStore store, MenuTranslationService translationService) {
        this.store = store;
        this.translationService = translationService;
    }

    MenuService(MenuOverridesStore store) {
        this(store, (texts, language) -> {
            throw new MenuTranslationException("translation_unavailable", "Translation service is not configured");
        });
    }

    public List<MenuSectionResponse> findMenuSections() {
        return store.readMenu();
    }

    public List<MenuSectionResponse> defaultMenuSections() {
        return store.readDefaultMenu();
    }

    public List<MenuItemResponse> findSignatureDrinks() {
        return findMenuSections().stream()
                .filter(section -> "cocktails".equals(section.id()))
                .findFirst()
                .map(MenuSectionResponse::items)
                .orElseGet(List::of);
    }

    public List<MenuSectionResponse> createItem(MenuItemRequest request) {
        validateTranslations(request.translations(), cleanNotes(request.notes()).size());
        return store.updateMenu(sections -> {
            int sectionIndex = sectionIndex(sections, request.sectionId());
            String base = slug(request.name());
            String id = uniqueId(sections, base.isBlank() ? "piatto" : base);
            List<MenuSectionResponse> updated = mutableSections(sections);
            MenuSectionResponse section = updated.get(sectionIndex);
            List<MenuItemResponse> items = new ArrayList<>(section.items());
            items.add(toItem(id, request, Map.of()));
            updated.set(sectionIndex, withItems(section, items));
            return updated;
        });
    }

    public List<MenuSectionResponse> updateItem(String id, MenuItemRequest request) {
        validateTranslations(request.translations(), cleanNotes(request.notes()).size());
        return store.updateMenu(sections -> {
            int sourceSection = -1;
            int sourceItem = -1;
            for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
                List<MenuItemResponse> items = sections.get(sectionIndex).items();
                for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                    if (items.get(itemIndex).id().equals(id)) {
                        sourceSection = sectionIndex;
                        sourceItem = itemIndex;
                    }
                }
            }
            if (sourceSection < 0) {
                throw new IllegalArgumentException("Piatto sconosciuto: " + id);
            }

            int destinationSection = sectionIndex(sections, request.sectionId());
            List<MenuSectionResponse> updated = mutableSections(sections);
            if (sourceSection == destinationSection) {
                MenuSectionResponse section = updated.get(sourceSection);
                List<MenuItemResponse> items = new ArrayList<>(section.items());
                MenuItemResponse existing = sections.get(sourceSection).items().get(sourceItem);
                items.set(sourceItem, toItem(id, request, existing.translations()));
                updated.set(sourceSection, withItems(section, items));
                return updated;
            }

            MenuSectionResponse source = updated.get(sourceSection);
            List<MenuItemResponse> sourceItems = new ArrayList<>(source.items());
            sourceItems.remove(sourceItem);
            updated.set(sourceSection, withItems(source, sourceItems));

            MenuSectionResponse destination = updated.get(destinationSection);
            List<MenuItemResponse> destinationItems = new ArrayList<>(destination.items());
            MenuItemResponse existing = sections.get(sourceSection).items().get(sourceItem);
            destinationItems.add(toItem(id, request, existing.translations()));
            updated.set(destinationSection, withItems(destination, destinationItems));
            return updated;
        });
    }

    public List<MenuSectionResponse> deleteItem(String id) {
        return store.updateMenu(sections -> {
            List<MenuSectionResponse> updated = mutableSections(sections);
            for (int sectionIndex = 0; sectionIndex < updated.size(); sectionIndex++) {
                MenuSectionResponse section = updated.get(sectionIndex);
                List<MenuItemResponse> items = new ArrayList<>(section.items());
                if (items.removeIf(item -> item.id().equals(id))) {
                    updated.set(sectionIndex, withItems(section, items));
                    return updated;
                }
            }
            throw new IllegalArgumentException("Piatto sconosciuto: " + id);
        });
    }

    /** Kept for the compact inline editor; prices are numeric in both request and JSON. */
    public List<MenuSectionResponse> updatePrices(Map<String, BigDecimal> requestedPrices) {
        return store.updateMenu(sections -> {
            List<MenuSectionResponse> updated = mutableSections(sections);
            for (Map.Entry<String, BigDecimal> entry : requestedPrices.entrySet()) {
                if (entry.getValue() == null || entry.getValue().signum() < 0) {
                    throw new IllegalArgumentException("Prezzo non valido per: " + entry.getKey());
                }
                boolean found = false;
                for (int sectionIndex = 0; sectionIndex < updated.size(); sectionIndex++) {
                    MenuSectionResponse section = updated.get(sectionIndex);
                    List<MenuItemResponse> items = new ArrayList<>(section.items());
                    for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                        MenuItemResponse item = items.get(itemIndex);
                        if (item.id().equals(entry.getKey())) {
                            items.set(itemIndex, new MenuItemResponse(item.id(), item.name(), item.subtitle(),
                                    item.description(), item.notes(), entry.getValue(), item.translations()));
                            updated.set(sectionIndex, withItems(section, items));
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                if (!found) {
                    throw new IllegalArgumentException("Piatto sconosciuto: " + entry.getKey());
                }
            }
            return updated;
        });
    }

    public BackfillTranslationsResponse backfillMissingTranslations() {
        int[] counts = new int[2];
        List<MenuSectionResponse> sections = store.updateMenu(current -> current.stream()
                .map(section -> withItems(section, section.items().stream().map(item -> {
                    LocalizedMenuItemText italian = italian(item.name(), item.subtitle(), item.description(), item.notes());
                    boolean missing = SUPPORTED_LANGUAGES.stream()
                            .anyMatch(language -> hasMissing(italian, item.translations().get(language)));
                    if (!missing) {
                        counts[1]++;
                        return item;
                    }
                    Map<String, LocalizedMenuItemText> translated = new LinkedHashMap<>(item.translations());
                    for (String language : List.of("en", "de")) {
                        LocalizedMenuItemText existing = translated.get(language);
                        if (hasMissing(italian, existing)) {
                            translated.put(language, translateLocalized(italian, existing, language, false));
                        }
                    }
                    counts[0]++;
                    return new MenuItemResponse(item.id(), item.name(), item.subtitle(), item.description(),
                            item.notes(), item.price(), translated);
                }).toList()))
                .toList());
        return new BackfillTranslationsResponse(counts[0], counts[1], sections);
    }

    private MenuItemResponse toItem(String id, MenuItemRequest request,
            Map<String, LocalizedMenuItemText> existingTranslations) {
        List<String> notes = cleanNotes(request.notes());
        LocalizedMenuItemText italian = italian(request.name().trim(), trim(request.subtitle()),
                trim(request.description()), notes);
        Map<String, LocalizedMenuItemText> translations = Boolean.TRUE.equals(request.autoTranslate())
                ? translateAll(italian)
                : mergeManualTranslations(existingTranslations, request.translations(), notes.size());
        return new MenuItemResponse(
                id,
                italian.name(), italian.subtitle(), italian.description(), italian.notes(),
                request.price(), translations);
    }

    private Map<String, LocalizedMenuItemText> translateAll(LocalizedMenuItemText italian) {
        Map<String, LocalizedMenuItemText> translated = new LinkedHashMap<>();
        translated.put("en", translateLocalized(italian, null, "en", true));
        translated.put("de", translateLocalized(italian, null, "de", true));
        return Map.copyOf(translated);
    }

    private LocalizedMenuItemText translateLocalized(LocalizedMenuItemText italian,
            LocalizedMenuItemText existing, String language, boolean overwrite) {
        String[] fields = {
                initialTranslation(italian.name(), existing == null ? null : existing.name(), overwrite),
                initialTranslation(italian.subtitle(), existing == null ? null : existing.subtitle(), overwrite),
                initialTranslation(italian.description(), existing == null ? null : existing.description(), overwrite)
        };
        List<String> oldNotes = existing == null || existing.notes() == null ? List.of() : existing.notes();
        List<String> notes = new ArrayList<>();
        for (int index = 0; index < italian.notes().size(); index++) {
            notes.add(initialTranslation(italian.notes().get(index),
                    index < oldNotes.size() ? oldNotes.get(index) : null, overwrite));
        }
        List<String> texts = new ArrayList<>();
        List<FieldReference> references = new ArrayList<>();
        String[] originals = { italian.name(), italian.subtitle(), italian.description() };
        for (int index = 0; index < originals.length; index++) {
            if (shouldTranslate(originals[index]) && (overwrite || fields[index].isBlank())) {
                texts.add(originals[index]);
                references.add(new FieldReference(index, -1));
            }
        }
        for (int index = 0; index < italian.notes().size(); index++) {
            if (shouldTranslate(italian.notes().get(index)) && (overwrite || notes.get(index).isBlank())) {
                texts.add(italian.notes().get(index));
                references.add(new FieldReference(3, index));
            }
        }
        List<String> results = translationService.translate(texts, language.toUpperCase());
        if (results.size() != texts.size() || results.stream().anyMatch(result -> result == null || result.isBlank())) {
            throw new MenuTranslationException("translation_incomplete",
                    "Il servizio di traduzione ha restituito una risposta incompleta.");
        }
        for (int index = 0; index < results.size(); index++) {
            FieldReference reference = references.get(index);
            if (reference.field() < 3) fields[reference.field()] = results.get(index);
            else notes.set(reference.noteIndex(), results.get(index));
        }
        return new LocalizedMenuItemText(fields[0], fields[1], fields[2], List.copyOf(notes));
    }

    private static boolean hasMissing(LocalizedMenuItemText italian, LocalizedMenuItemText translated) {
        if (translated == null) return true;
        if (shouldTranslate(italian.name()) && value(translated.name()).isBlank()) return true;
        if (shouldTranslate(italian.subtitle()) && value(translated.subtitle()).isBlank()) return true;
        if (shouldTranslate(italian.description()) && value(translated.description()).isBlank()) return true;
        List<String> notes = translated.notes() == null ? List.of() : translated.notes();
        if (notes.size() != italian.notes().size()) return true;
        for (int index = 0; index < notes.size(); index++) {
            if (shouldTranslate(italian.notes().get(index)) && value(notes.get(index)).isBlank()) return true;
        }
        return false;
    }

    private static Map<String, LocalizedMenuItemText> mergeManualTranslations(
            Map<String, LocalizedMenuItemText> existing,
            Map<String, LocalizedMenuItemText> requested,
            int noteCount) {
        Map<String, LocalizedMenuItemText> merged = new LinkedHashMap<>(existing == null ? Map.of() : existing);
        if (requested == null) return Map.copyOf(merged);
        requested.forEach((language, incoming) -> {
            LocalizedMenuItemText previous = merged.get(language);
            merged.put(language, new LocalizedMenuItemText(
                    incoming.name() == null ? value(previous == null ? null : previous.name()) : trim(incoming.name()),
                    incoming.subtitle() == null ? value(previous == null ? null : previous.subtitle()) : trim(incoming.subtitle()),
                    incoming.description() == null ? value(previous == null ? null : previous.description()) : trim(incoming.description()),
                    incoming.notes() == null
                            ? normalizedNotes(previous == null ? null : previous.notes(), noteCount)
                            : incoming.notes().stream().map(MenuService::trim).toList()));
        });
        return Map.copyOf(merged);
    }

    private static void validateTranslations(Map<String, LocalizedMenuItemText> translations, int noteCount) {
        if (translations == null) return;
        for (Map.Entry<String, LocalizedMenuItemText> entry : translations.entrySet()) {
            if (!SUPPORTED_LANGUAGES.contains(entry.getKey())) {
                throw new IllegalArgumentException("Sono ammesse soltanto le traduzioni en e de");
            }
            if (entry.getValue() == null) throw new IllegalArgumentException("Traduzione non valida per " + entry.getKey());
            if (entry.getValue().notes() != null && entry.getValue().notes().size() != noteCount) {
                throw new IllegalArgumentException("Le note tradotte devono corrispondere alle note italiane");
            }
        }
    }

    private static LocalizedMenuItemText italian(String name, String subtitle, String description, List<String> notes) {
        return new LocalizedMenuItemText(name, subtitle, description, notes);
    }

    private static List<String> cleanNotes(List<String> notes) {
        return notes == null ? List.of() : notes.stream().filter(note -> note != null)
                .map(String::trim).filter(note -> !note.isEmpty()).toList();
    }

    private static List<String> normalizedNotes(List<String> notes, int noteCount) {
        if (notes == null) return List.of();
        return notes.stream().limit(noteCount).map(MenuService::trim).toList();
    }

    private static String value(String value) { return value == null ? "" : value; }

    private static String initialTranslation(String italian, String existing, boolean overwrite) {
        if (!shouldTranslate(italian)) return value(italian);
        return overwrite ? "" : value(existing);
    }

    private static boolean shouldTranslate(String text) {
        return text != null && !text.isBlank()
                && !text.trim().matches("(?i)^\\d+(?:[.,]\\d+)?\\s*(?:cl|ml|l|€)?$");
    }

    private static List<MenuSectionResponse> mutableSections(List<MenuSectionResponse> sections) {
        return new ArrayList<>(sections);
    }

    private static MenuSectionResponse withItems(MenuSectionResponse section, List<MenuItemResponse> items) {
        return new MenuSectionResponse(section.id(), section.title(), section.description(), List.copyOf(items));
    }

    private static int sectionIndex(List<MenuSectionResponse> sections, String id) {
        for (int index = 0; index < sections.size(); index++) {
            if (sections.get(index).id().equals(id)) return index;
        }
        throw new IllegalArgumentException("Categoria sconosciuta: " + id);
    }

    private static String uniqueId(List<MenuSectionResponse> sections, String base) {
        String id;
        do {
            id = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        } while (containsItem(sections, id));
        return id;
    }

    private static boolean containsItem(List<MenuSectionResponse> sections, String id) {
        return sections.stream().flatMap(section -> section.items().stream())
                .anyMatch(item -> item.id().equals(id));
    }

    private static String slug(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private record FieldReference(int field, int noteIndex) { }
}
