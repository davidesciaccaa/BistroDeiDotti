package com.angolodivino.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuServiceTest {

    @TempDir
    Path tempDir;

    private Path overridesFile;
    private MenuService menuService;

    @BeforeEach
    void setUp() {
        overridesFile = tempDir.resolve("menu-overrides.json");
        menuService = new MenuService(new MenuOverridesStore(propertiesFor(overridesFile)));
    }

    @Test
    void returnsMenuSectionsInCorrectOrder() {
        List<MenuSectionResponse> sections = menuService.findMenuSections();
        assertThat(sections)
                .extracting(MenuSectionResponse::id)
                .containsExactly(
                        "antipasti",
                        "sfizi",
                        "insalatone",
                        "primi-terra",
                        "secondi-terra",
                        "primi-mare",
                        "secondi-mare",
                        "dolci",
                        "bevande",
                        "gin",
                        "cocktails",
                        "amari",
                        "distillati",
                        "vini-bianchi",
                        "vini-rosati",
                        "vini-rossi",
                        "vini-bio",
                        "prosecco");
    }

    @Test
    void allSectionsArePopulated() {
        List<MenuSectionResponse> sections = menuService.findMenuSections();

        sections.forEach(section -> {
            assertThat(section.items()).isNotEmpty();
            assertThat(section.items()).allSatisfy(item -> {
                assertThat(item.name()).isNotBlank();
            });
        });
    }

    /** Overrides are keyed by item id alone, which only works while ids stay globally unique. */
    @Test
    void itemIdsAreUniqueAcrossSections() {
        List<String> ids = menuService.defaultMenuSections().stream()
                .flatMap(section -> section.items().stream())
                .map(MenuItemResponse::id)
                .toList();

        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    void usesHardcodedPricesWhenOverridesFileIsMissing() {
        assertThat(overridesFile).doesNotExist();
        assertThat(priceOf(menuService.findMenuSections(), "negroni")).isEqualTo("7 €");
    }

    @Test
    void appliesOverriddenPricesFromFile() throws Exception {
        Files.writeString(overridesFile, "{\"prices\":{\"negroni\":\"9 €\"}}");

        List<MenuSectionResponse> sections = menuService.findMenuSections();
        assertThat(priceOf(sections, "negroni")).isEqualTo("9 €");
        assertThat(priceOf(sections, "aperol_spritz")).isEqualTo("5 €");
    }

    @Test
    void fallsBackToHardcodedPricesWhenFileIsCorrupt() throws Exception {
        Files.writeString(overridesFile, "{ this is not json");

        assertThat(priceOf(menuService.findMenuSections(), "negroni")).isEqualTo("7 €");
    }

    @Test
    void updatePricesWritesOverridesAndReturnsUpdatedMenu() {
        List<MenuSectionResponse> sections = menuService.updatePrices(Map.of("negroni", "9 €", "mojito_scuro", "10 €"));

        assertThat(priceOf(sections, "negroni")).isEqualTo("9 €");
        assertThat(priceOf(sections, "mojito_scuro")).isEqualTo("10 €");
        assertThat(overridesFile).exists();
        assertThat(priceOf(menuService.findMenuSections(), "negroni")).isEqualTo("9 €");
    }

    @Test
    void updatePricesMergesWithExistingOverrides() {
        menuService.updatePrices(Map.of("negroni", "9 €"));
        menuService.updatePrices(Map.of("mojito_scuro", "10 €"));

        List<MenuSectionResponse> sections = menuService.findMenuSections();
        assertThat(priceOf(sections, "negroni")).isEqualTo("9 €");
        assertThat(priceOf(sections, "mojito_scuro")).isEqualTo("10 €");
    }

    @Test
    void updatePricesDropsOverridesThatMatchTheHardcodedPrice() {
        menuService.updatePrices(Map.of("negroni", "9 €"));
        menuService.updatePrices(Map.of("negroni", "7 €"));

        assertThat(priceOf(menuService.findMenuSections(), "negroni")).isEqualTo("7 €");
        assertThat(new MenuOverridesStore(propertiesFor(overridesFile)).readPrices())
                .doesNotContainKey("negroni");
    }

    @Test
    void updatePricesRejectsUnknownItemIds() {
        assertThatThrownBy(() -> menuService.updatePrices(Map.of("voce_inesistente", "9 €")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voce_inesistente");

        assertThat(overridesFile).doesNotExist();
    }

    @Test
    void updatePricesRejectsPricesWithMarkup() {
        assertThatThrownBy(() -> menuService.updatePrices(Map.of("negroni", "<script>alert(1)</script>")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negroni");

        assertThat(overridesFile).doesNotExist();
    }

    @Test
    void updatePricesAcceptsTheExistingPriceFormats() {
        List<MenuSectionResponse> sections = menuService.updatePrices(Map.of(
                "calice_vino", "5 € / 20 €",
                "acqua", "2,50 €",
                "crudo_iberico", "-"));

        assertThat(priceOf(sections, "calice_vino")).isEqualTo("5 € / 20 €");
        assertThat(priceOf(sections, "acqua")).isEqualTo("2,50 €");
        assertThat(priceOf(sections, "crudo_iberico")).isEqualTo("-");
    }

    @Test
    void updatePricesCanRestoreTheUnpricedMenuItem() {
        menuService.updatePrices(Map.of("crudo_iberico", "12 €"));
        List<MenuSectionResponse> sections = menuService.updatePrices(Map.of("crudo_iberico", ""));

        assertThat(priceOf(sections, "crudo_iberico")).isEmpty();
        assertThat(new MenuOverridesStore(propertiesFor(overridesFile)).readPrices())
                .doesNotContainKey("crudo_iberico");
    }

    private static MenuProperties propertiesFor(Path file) {
        MenuProperties properties = new MenuProperties();
        properties.setOverridesFile(file.toString());
        return properties;
    }

    private static String priceOf(List<MenuSectionResponse> sections, String itemId) {
        return sections.stream()
                .flatMap(section -> section.items().stream())
                .filter(item -> item.id().equals(itemId))
                .findFirst()
                .map(MenuItemResponse::price)
                .orElseThrow(() -> new AssertionError("Item not found: " + itemId));
    }
}
