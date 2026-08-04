package com.angolodivino.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.angolodivino.admin.MenuItemRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuTranslationTest {
    @TempDir Path tempDir;

    @Test
    void automaticCreateAndRegenerationBatchBothLanguagesAndPreserveStructure() {
        MenuOverridesStore store = store(tempDir);
        RecordingTranslator translator = new RecordingTranslator();
        MenuService service = new MenuService(store, translator);
        List<MenuSectionResponse> createdMenu = service.createItem(request(
                "Nuovo piatto", List.of("Senza glutine", "50 cl", "Servire caldo"), true, null));
        MenuItemResponse created = byName(createdMenu, "Nuovo piatto");

        assertThat(translator.languages).containsExactly("EN", "DE");
        assertThat(created.translations().get("en").notes())
                .containsExactly("EN:Senza glutine", "50 cl", "EN:Servire caldo");
        assertThat(created.price()).isEqualByComparingTo("12.5");
        assertThat(sectionOf(createdMenu, created.id())).isEqualTo("antipasti");

        translator.languages.clear();
        service.updateItem(created.id(), request("Piatto aggiornato", List.of("Caldo"), true, null));
        MenuItemResponse updated = byId(store.readMenu(), created.id());
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.translations().get("de").name()).isEqualTo("DE:Piatto aggiornato");
        assertThat(updated.translations().get("de").notes()).containsExactly("DE:Caldo");
    }

    @Test
    void manualUpdatesDoNotCallProviderAndPreserveUnsentLanguage() {
        MenuOverridesStore store = store(tempDir);
        RecordingTranslator translator = new RecordingTranslator();
        MenuService service = new MenuService(store, translator);
        Map<String, LocalizedMenuItemText> both = Map.of(
                "en", text("Dish", "Description", List.of("Hot")),
                "de", text("Gericht", "Beschreibung", List.of("Heiß")));
        MenuItemResponse created = byName(service.createItem(request("Piatto", List.of("Caldo"), false, both)), "Piatto");

        service.updateItem(created.id(), request("Piatto", List.of("Caldo"), false,
                Map.of("en", text("Edited dish", "Edited", List.of("Hot")))));
        MenuItemResponse updated = byId(store(tempDir).readMenu(), created.id());

        assertThat(translator.languages).isEmpty();
        assertThat(updated.translations().get("en").name()).isEqualTo("Edited dish");
        assertThat(updated.translations().get("de").name()).isEqualTo("Gericht");
    }

    @Test
    void providerFailureAndMissingKeyNeverWritePartialMenu() throws Exception {
        MenuOverridesStore store = store(tempDir);
        byte[] before = Files.readAllBytes(store.menuFile());
        MenuService failing = new MenuService(store,
                (texts, language) -> { throw new MenuTranslationException("translation_unavailable", "down"); });
        assertThatThrownBy(() -> failing.createItem(request("Non salvare", List.of(), true, null)))
                .isInstanceOf(MenuTranslationException.class);
        assertThat(Files.readAllBytes(store.menuFile())).isEqualTo(before);

        TranslationProperties properties = new TranslationProperties();
        properties.setEnabled(true);
        assertThatThrownBy(() -> new DeepLMenuTranslationService(properties).translate(List.of("Ciao"), "EN"))
                .isInstanceOf(MenuTranslationException.class)
                .hasMessageContaining("DEEPL_AUTH_KEY");
    }

    @Test
    void validatesAllowedLanguagesBeforeTranslation() {
        RecordingTranslator translator = new RecordingTranslator();
        MenuService service = new MenuService(store(tempDir), translator);
        assertThatThrownBy(() -> service.createItem(request("Piatto", List.of(), false,
                Map.of("fr", text("Plat", "", List.of())))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("en e de");
        assertThat(translator.languages).isEmpty();
    }

    @Test
    void backfillOnlyFillsMissingTextPreservesExistingAndWritesOnce() {
        CountingStore store = countingStore(tempDir);
        RecordingTranslator translator = new RecordingTranslator();
        MenuService service = new MenuService(store, translator);
        Map<String, LocalizedMenuItemText> partial = new LinkedHashMap<>();
        partial.put("en", new LocalizedMenuItemText("Existing dish", "", null, List.of("Existing note")));
        MenuItemResponse created = byName(service.createItem(request("Piatto", List.of("Nota"), false, partial)), "Piatto");
        store.runtimeWrites = 0;

        BackfillTranslationsResponse result = service.backfillMissingTranslations();
        MenuItemResponse updated = byId(result.sections(), created.id());

        assertThat(updated.translations().get("en").name()).isEqualTo("Existing dish");
        assertThat(updated.translations().get("en").notes()).containsExactly("Existing note");
        assertThat(updated.translations().get("en").description()).isEqualTo("EN:Descrizione");
        assertThat(result.updatedItems()).isPositive();
        assertThat(store.runtimeWrites).isEqualTo(1);
    }

    @Test
    void oldJsonLoadsAndTranslationsSurviveRestartBackupAndDeletion() throws Exception {
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("menu.json"), """
                {"updatedAt":"2026-01-01T00:00:00Z","sections":[{"id":"antipasti","title":"Antipasti",
                "description":"","items":[{"id":"piatto","name":"Piatto","subtitle":"","description":"",
                "notes":[],"price":12}]}]}
                """);
        MenuOverridesStore store = store(tempDir);
        assertThat(byId(store.readMenu(), "piatto").translations()).isEmpty();
        new MenuService(store, new RecordingTranslator()).updateItem("piatto",
                request("Piatto", List.of(), false, Map.of("en", text("Dish", "", List.of()))));
        Files.deleteIfExists(store.dailyDirectory().resolve("menu-2026-08-04.json"));
        Files.deleteIfExists(store.monthlyDirectory().resolve("menu-2026-08.json"));
        store.ensureBackups();

        assertThat(byId(store(tempDir).readMenu(), "piatto").translations().get("en").name()).isEqualTo("Dish");
        assertThat(Files.readString(store.dailyDirectory().resolve("menu-2026-08-04.json")))
                .contains("\"translations\"").contains("\"Dish\"");
        new MenuService(store, new RecordingTranslator()).deleteItem("piatto");
        assertThat(store.readMenu().stream().flatMap(section -> section.items().stream())).isEmpty();
    }

    private static MenuItemRequest request(String name, List<String> notes, boolean automatic,
            Map<String, LocalizedMenuItemText> translations) {
        return new MenuItemRequest("antipasti", name, "Specialità", "Descrizione", notes,
                new BigDecimal("12.5"), translations, automatic);
    }

    private static LocalizedMenuItemText text(String name, String description, List<String> notes) {
        return new LocalizedMenuItemText(name, "Special", description, notes);
    }

    private static MenuOverridesStore store(Path directory) {
        MenuOverridesStore store = new MenuOverridesStore(MenuServiceTest.propertiesFor(directory),
                Clock.fixed(Instant.parse("2026-08-04T10:00:00Z"), ZoneId.of("Europe/Rome")));
        store.initialize();
        return store;
    }

    private static CountingStore countingStore(Path directory) {
        CountingStore store = new CountingStore(MenuServiceTest.propertiesFor(directory));
        store.initialize();
        return store;
    }

    private static MenuItemResponse byName(List<MenuSectionResponse> sections, String name) {
        return sections.stream().flatMap(section -> section.items().stream())
                .filter(item -> item.name().equals(name)).findFirst().orElseThrow();
    }

    private static MenuItemResponse byId(List<MenuSectionResponse> sections, String id) {
        return sections.stream().flatMap(section -> section.items().stream())
                .filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    private static String sectionOf(List<MenuSectionResponse> sections, String id) {
        return sections.stream().filter(section -> section.items().stream().anyMatch(item -> item.id().equals(id)))
                .findFirst().orElseThrow().id();
    }

    private static final class RecordingTranslator implements MenuTranslationService {
        private final List<String> languages = new ArrayList<>();
        @Override public List<String> translate(List<String> texts, String targetLanguage) {
            languages.add(targetLanguage);
            return texts.stream().map(text -> targetLanguage + ":" + text).toList();
        }
    }

    private static final class CountingStore extends MenuOverridesStore {
        private int runtimeWrites;
        private CountingStore(MenuProperties properties) {
            super(properties, Clock.fixed(Instant.parse("2026-08-04T10:00:00Z"), ZoneId.of("Europe/Rome")));
        }
        @Override protected void writeAtomically(Path target, byte[] bytes) throws IOException {
            if (target.equals(menuFile())) runtimeWrites++;
            super.writeAtomically(target, bytes);
        }
    }
}
