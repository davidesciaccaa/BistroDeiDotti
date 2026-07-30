package com.angolodivino.menu;

import com.angolodivino.admin.MenuItemRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

    private final MenuOverridesStore store;

    public MenuService(MenuOverridesStore store) {
        this.store = store;
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
        return store.updateMenu(sections -> {
            int sectionIndex = sectionIndex(sections, request.sectionId());
            String base = slug(request.name());
            String id = uniqueId(sections, base.isBlank() ? "piatto" : base);
            List<MenuSectionResponse> updated = mutableSections(sections);
            MenuSectionResponse section = updated.get(sectionIndex);
            List<MenuItemResponse> items = new ArrayList<>(section.items());
            items.add(toItem(id, request));
            updated.set(sectionIndex, withItems(section, items));
            return updated;
        });
    }

    public List<MenuSectionResponse> updateItem(String id, MenuItemRequest request) {
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
                items.set(sourceItem, toItem(id, request));
                updated.set(sourceSection, withItems(section, items));
                return updated;
            }

            MenuSectionResponse source = updated.get(sourceSection);
            List<MenuItemResponse> sourceItems = new ArrayList<>(source.items());
            sourceItems.remove(sourceItem);
            updated.set(sourceSection, withItems(source, sourceItems));

            MenuSectionResponse destination = updated.get(destinationSection);
            List<MenuItemResponse> destinationItems = new ArrayList<>(destination.items());
            destinationItems.add(toItem(id, request));
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
                if (entry.getValue() != null && entry.getValue().signum() < 0) {
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
                                    item.description(), item.notes(), entry.getValue()));
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

    private static MenuItemResponse toItem(String id, MenuItemRequest request) {
        return new MenuItemResponse(
                id,
                request.name().trim(),
                trim(request.subtitle()),
                trim(request.description()),
                request.notes() == null
                        ? List.of()
                        : request.notes().stream().filter(note -> note != null && !note.isBlank())
                                .map(String::trim).toList(),
                request.price());
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
}
