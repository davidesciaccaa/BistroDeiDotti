package com.angolodivino.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.angolodivino.admin.MenuItemRequest;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MenuOverridesStoreTest {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    private static final Instant NOW = Instant.parse("2026-07-30T10:00:00Z");

    @TempDir
    Path tempDir;

    @Test
    void createsDirectoriesAndInitializesRuntimeFromDefault() {
        MenuOverridesStore store = storeAt(tempDir.resolve("nested data"), NOW);
        store.initialize();

        assertThat(store.menuFile()).isRegularFile();
        assertThat(store.dailyDirectory()).isDirectory();
        assertThat(store.monthlyDirectory()).isDirectory();
        assertThat(store.readMenu()).isEqualTo(store.readDefaultMenu());
    }

    @Test
    void neverOverwritesAnExistingRuntimeMenuOnRestart() {
        MenuOverridesStore first = storeAt(tempDir.resolve("data"), NOW);
        first.initialize();
        MenuService service = new MenuService(first);
        service.createItem(new MenuItemRequest("antipasti", "Da conservare", "", "", List.of(), BigDecimal.TEN));
        byte[] customized = bytes(first.menuFile());

        MenuOverridesStore restarted = storeAt(tempDir.resolve("data"), NOW.plusSeconds(3600));
        restarted.initialize();

        assertThat(bytes(restarted.menuFile())).isEqualTo(customized);
        assertThat(restarted.readMenu().stream().flatMap(section -> section.items().stream())
                .anyMatch(item -> item.name().equals("Da conservare"))).isTrue();
    }

    @Test
    void usesTemporaryFilesAndLeavesNoneBehindAfterAtomicSave() throws Exception {
        MenuOverridesStore store = storeAt(tempDir.resolve("data"), NOW);
        store.initialize();
        new MenuService(store).updatePrices(java.util.Map.of("negroni", new BigDecimal("8")));

        try (var files = Files.list(store.dataDirectory())) {
            assertThat(files.map(path -> path.getFileName().toString()))
                    .noneMatch(name -> name.endsWith(".tmp"));
        }
        assertThat(store.readMenu()).isNotEmpty();
    }

    @Test
    void failedValidationLeavesTheLastValidFileUntouched() {
        MenuOverridesStore store = storeAt(tempDir.resolve("data"), NOW);
        store.initialize();
        byte[] before = bytes(store.menuFile());

        assertThatThrownBy(() -> store.updateMenu(sections -> {
            List<MenuSectionResponse> invalid = new ArrayList<>(sections);
            invalid.add(sections.get(0));
            return invalid;
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(bytes(store.menuFile())).isEqualTo(before);
        assertThat(store.readMenu()).isNotEmpty();
    }

    @Test
    void ioFailureDuringAtomicWriteLeavesTheLastValidMenuUntouched() {
        MenuProperties properties = MenuServiceTest.propertiesFor(tempDir.resolve("data"));
        FailingStore store = new FailingStore(properties, Clock.fixed(NOW, ROME));
        store.initialize();
        byte[] before = bytes(store.menuFile());
        store.failWrites = true;

        assertThatThrownBy(() -> new MenuService(store)
                .updatePrices(java.util.Map.of("negroni", new BigDecimal("99"))))
                .isInstanceOf(java.io.UncheckedIOException.class);

        assertThat(bytes(store.menuFile())).isEqualTo(before);
    }

    @Test
    void migratesLegacyEuroPricesOnlyWhenInitializingTheRuntimeFile() throws Exception {
        Path legacy = tempDir.resolve("legacy").resolve("menu-overrides.json");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy, """
                {"updatedAt":"2025-01-01T00:00:00Z","prices":{"negroni":"11 €"}}
                """);
        MenuProperties properties = MenuServiceTest.propertiesFor(tempDir.resolve("data"));
        properties.setLegacyOverridesFile(legacy.toString());

        MenuOverridesStore store = new MenuOverridesStore(properties, Clock.fixed(NOW, ROME));
        store.initialize();

        assertThat(store.readMenu().stream().flatMap(section -> section.items().stream())
                .filter(item -> item.id().equals("negroni")).findFirst().orElseThrow().price())
                .isEqualByComparingTo("11");
    }

    @Test
    void readsFormattedPricesFromAnExistingProductionMenuAsNumbers() throws Exception {
        Path dataDirectory = tempDir.resolve("existing-data");
        Files.createDirectories(dataDirectory);
        Files.writeString(dataDirectory.resolve("menu.json"), """
                {
                  "updatedAt": "2026-01-01T00:00:00Z",
                  "sections": [{
                    "id": "bevande",
                    "title": "Bevande",
                    "description": "",
                    "items": [{
                      "id": "acqua",
                      "name": "Acqua",
                      "subtitle": "",
                      "description": "",
                      "notes": [],
                      "price": "2,50 €"
                    }]
                  }]
                }
                """);

        MenuOverridesStore store = storeAt(dataDirectory, NOW);
        store.initialize();

        MenuItemResponse item = store.readMenu().getFirst().items().getFirst();
        assertThat(item.price()).isEqualByComparingTo("2.5");
        assertThat(new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(item).get("price").isNumber())
                .isTrue();
    }

    @Test
    void createsOnlyOneDailyAndMonthlyBackupAndDoesNotOverwriteThem() throws Exception {
        MenuOverridesStore store = storeAt(tempDir.resolve("data"), NOW);
        store.initialize();
        Path daily = store.dailyDirectory().resolve("menu-2026-07-30.json");
        Path monthly = store.monthlyDirectory().resolve("menu-2026-07.json");
        byte[] dailyBefore = bytes(daily);
        byte[] monthlyBefore = bytes(monthly);

        new MenuService(store).updatePrices(java.util.Map.of("negroni", new BigDecimal("12")));
        store.ensureBackups();

        assertThat(bytes(daily)).isEqualTo(dailyBefore);
        assertThat(bytes(monthly)).isEqualTo(monthlyBefore);
        assertThat(Files.list(store.dailyDirectory()).count()).isEqualTo(1);
        assertThat(Files.list(store.monthlyDirectory()).count()).isEqualTo(1);
    }

    @Test
    void removesExpiredDailyAndMonthlyBackups() throws Exception {
        MenuOverridesStore store = storeAt(tempDir.resolve("data"), NOW);
        store.initialize();
        Files.writeString(store.dailyDirectory().resolve("menu-2026-06-01.json"), "{}");
        Files.writeString(store.dailyDirectory().resolve("menu-2026-07-02.json"), "{}");
        Files.writeString(store.monthlyDirectory().resolve("menu-2025-07.json"), "{}");
        Files.writeString(store.monthlyDirectory().resolve("menu-2025-08.json"), "{}");

        store.ensureBackups();

        assertThat(store.dailyDirectory().resolve("menu-2026-06-01.json")).doesNotExist();
        assertThat(store.dailyDirectory().resolve("menu-2026-07-02.json")).exists();
        assertThat(store.monthlyDirectory().resolve("menu-2025-07.json")).doesNotExist();
        assertThat(store.monthlyDirectory().resolve("menu-2025-08.json")).exists();
    }

    @Test
    void doesNotCreateBackupFromCorruptRuntimeJson() throws Exception {
        MenuOverridesStore store = storeAt(tempDir.resolve("data"), NOW);
        store.initialize();
        Path daily = store.dailyDirectory().resolve("menu-2026-07-30.json");
        Path monthly = store.monthlyDirectory().resolve("menu-2026-07.json");
        Files.delete(daily);
        Files.delete(monthly);
        Files.writeString(store.menuFile(), "{not-json");

        store.ensureBackups();

        assertThat(daily).doesNotExist();
        assertThat(monthly).doesNotExist();
    }

    @Test
    void serializesConcurrentMutationsWithoutLosingItems() throws Exception {
        MenuOverridesStore store = storeAt(tempDir.resolve("data"), NOW);
        store.initialize();
        MenuService service = new MenuService(store);
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int index = 0; index < 24; index++) {
                int current = index;
                tasks.add(() -> {
                    service.createItem(new MenuItemRequest("antipasti", "Concorrente " + current,
                            "", "", List.of(), BigDecimal.valueOf(current)));
                    return null;
                });
            }
            executor.invokeAll(tasks).forEach(future -> {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        } finally {
            executor.shutdownNow();
        }

        assertThat(store.readMenu().stream().flatMap(section -> section.items().stream())
                .filter(item -> item.name().startsWith("Concorrente"))).hasSize(24);
    }

    @Test
    void resolvesRelativeAndNestedPathsWithNioApis() {
        Path configured = tempDir.resolve("cartella con spazi").resolve("runtime").resolve("..").resolve("runtime");
        MenuOverridesStore store = storeAt(configured, NOW);
        store.initialize();

        assertThat(store.dataDirectory()).isAbsolute().isEqualTo(configured.toAbsolutePath().normalize());
        assertThat(store.menuFile()).hasParent(store.dataDirectory());
    }

    private MenuOverridesStore storeAt(Path dataDirectory, Instant instant) {
        return new MenuOverridesStore(MenuServiceTest.propertiesFor(dataDirectory), Clock.fixed(instant, ROME));
    }

    private static byte[] bytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static final class FailingStore extends MenuOverridesStore {
        private boolean failWrites;

        private FailingStore(MenuProperties properties, Clock clock) {
            super(properties, clock);
        }

        @Override
        protected void writeAtomically(Path target, byte[] bytes) throws IOException {
            if (failWrites) throw new IOException("simulated write failure");
            super.writeAtomically(target, bytes);
        }
    }
}
