package com.angolodivino.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.angolodivino.admin.MenuItemRequest;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuServiceTest {

    @TempDir
    Path tempDir;

    private MenuOverridesStore store;
    private MenuService service;

    @BeforeEach
    void setUp() {
        MenuProperties properties = propertiesFor(tempDir.resolve("data"));
        store = new MenuOverridesStore(properties);
        store.initialize();
        service = new MenuService(store);
    }

    @Test
    void defaultMenuHasExpectedOrderedSectionsAndUniqueItems() {
        List<MenuSectionResponse> sections = service.defaultMenuSections();
        assertThat(sections).extracting(MenuSectionResponse::id)
                .startsWith("antipasti", "sfizi", "insalatone")
                .endsWith("vini-bio", "prosecco");
        assertThat(sections.stream().flatMap(section -> section.items().stream()).map(MenuItemResponse::id))
                .doesNotHaveDuplicates();
    }

    @Test
    void createsUpdatesMovesAndDeletesAnItem() {
        service.createItem(request("antipasti", "Piatto test", "10.50"));
        MenuItemResponse created = findByName(service.findMenuSections(), "Piatto test");
        assertThat(created.price()).isEqualByComparingTo("10.5");

        service.updateItem(created.id(), new MenuItemRequest(
                "dolci", "Piatto aggiornato", "Novità", "Descrizione",
                List.of("Senza glutine"), new BigDecimal("12")));
        MenuItemResponse updated = findByName(service.findMenuSections(), "Piatto aggiornato");
        assertThat(updated.subtitle()).isEqualTo("Novità");
        assertThat(updated.notes()).containsExactly("Senza glutine");
        assertThat(sectionOf(service.findMenuSections(), updated.id())).isEqualTo("dolci");

        service.deleteItem(updated.id());
        assertThat(service.findMenuSections().stream().flatMap(section -> section.items().stream())
                .noneMatch(item -> item.id().equals(updated.id()))).isTrue();
    }

    @Test
    void persistsCrudAfterStoreRestart() {
        service.createItem(request("antipasti", "Persistente", "9.90"));
        String id = findByName(service.findMenuSections(), "Persistente").id();

        MenuOverridesStore restartedStore = new MenuOverridesStore(propertiesFor(tempDir.resolve("data")));
        restartedStore.initialize();
        MenuService restarted = new MenuService(restartedStore);

        assertThat(findByName(restarted.findMenuSections(), "Persistente").price())
                .isEqualByComparingTo("9.9");
        restarted.deleteItem(id);

        MenuOverridesStore secondRestart = new MenuOverridesStore(propertiesFor(tempDir.resolve("data")));
        secondRestart.initialize();
        assertThat(secondRestart.readMenu().stream().flatMap(section -> section.items().stream())
                .noneMatch(item -> item.id().equals(id))).isTrue();
    }

    @Test
    void updatesPricesAsNumbersAndRejectsInvalidReferences() {
        service.updatePrices(Map.of("negroni", new BigDecimal("8.50")));
        assertThat(findById(service.findMenuSections(), "negroni").price()).isEqualByComparingTo("8.5");

        assertThatThrownBy(() -> service.updatePrices(Map.of("missing", BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void parsesOnlyUnambiguousLegacyPrices() {
        assertThat(MenuItemResponse.parsePrice("12 €")).isEqualByComparingTo("12");
        assertThat(MenuItemResponse.parsePrice("2,50 €")).isEqualByComparingTo("2.5");
        assertThat(MenuItemResponse.parsePrice("-")).isNull();
        assertThatThrownBy(() -> MenuItemResponse.parsePrice("5 € / 20 €"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5 € / 20 €");
    }

    @Test
    void rejectsUnknownCategoriesAndItemsWithoutChangingTheFile() throws Exception {
        byte[] before = java.nio.file.Files.readAllBytes(store.menuFile());
        assertThatThrownBy(() -> service.createItem(request("missing", "No", "2")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deleteItem("missing"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(java.nio.file.Files.readAllBytes(store.menuFile())).isEqualTo(before);
    }

    private static MenuItemRequest request(String section, String name, String price) {
        return new MenuItemRequest(section, name, "", "", List.of(), new BigDecimal(price));
    }

    static MenuProperties propertiesFor(Path dataDirectory) {
        MenuProperties properties = new MenuProperties();
        properties.setDataDirectory(dataDirectory.toString());
        properties.setLegacyOverridesFile("");
        return properties;
    }

    private static MenuItemResponse findByName(List<MenuSectionResponse> sections, String name) {
        return sections.stream().flatMap(section -> section.items().stream())
                .filter(item -> item.name().equals(name)).findFirst().orElseThrow();
    }

    private static MenuItemResponse findById(List<MenuSectionResponse> sections, String id) {
        return sections.stream().flatMap(section -> section.items().stream())
                .filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    private static String sectionOf(List<MenuSectionResponse> sections, String itemId) {
        return sections.stream().filter(section -> section.items().stream()
                .anyMatch(item -> item.id().equals(itemId))).findFirst().orElseThrow().id();
    }
}
