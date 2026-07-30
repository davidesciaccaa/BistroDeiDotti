package com.angolodivino.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Owns every read, write and backup of the runtime catalogue. A single lock
 * coordinates CRUD operations, atomic writes and snapshots.
 */
@Component
public class MenuOverridesStore {

    private static final Logger log = LoggerFactory.getLogger(MenuOverridesStore.class);
    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    private static final Pattern DAILY_NAME = Pattern.compile("menu-(\\d{4}-\\d{2}-\\d{2})\\.json");
    private static final Pattern MONTHLY_NAME = Pattern.compile("menu-(\\d{4}-\\d{2})\\.json");

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private final ReentrantLock lock = new ReentrantLock(true);
    private final MenuProperties properties;
    private final Clock clock;
    private final Path dataDirectory;
    private final Path menuFile;
    private final Path dailyDirectory;
    private final Path monthlyDirectory;
    private final Path legacyOverridesFile;

    @Autowired
    public MenuOverridesStore(MenuProperties properties) {
        this(properties, Clock.system(ROME));
    }

    MenuOverridesStore(MenuProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.dataDirectory = Path.of(properties.getDataDirectory()).toAbsolutePath().normalize();
        this.menuFile = dataDirectory.resolve("menu.json");
        this.dailyDirectory = dataDirectory.resolve("backups").resolve("daily");
        this.monthlyDirectory = dataDirectory.resolve("backups").resolve("monthly");
        String legacy = properties.getLegacyOverridesFile();
        this.legacyOverridesFile = legacy == null || legacy.isBlank()
                ? null
                : Path.of(legacy).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void initialize() {
        lock.lock();
        try {
            Files.createDirectories(dataDirectory);
            Files.createDirectories(dailyDirectory);
            Files.createDirectories(monthlyDirectory);

            if (Files.notExists(menuFile)) {
                List<MenuSectionResponse> seed = applyLegacyPrices(readDefaultMenu(), readLegacyPrices());
                writeMenuUnlocked(seed);
                log.info("Runtime menu initialized from classpath:{} at {}",
                        properties.getDefaultResource(), menuFile);
            } else {
                readMenuUnlocked();
                log.info("Existing runtime menu loaded from {}; default seed left untouched", menuFile);
            }

            ensureBackupsUnlocked();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not initialize menu storage at " + dataDirectory, e);
        } finally {
            lock.unlock();
        }
    }

    public List<MenuSectionResponse> readMenu() {
        lock.lock();
        try {
            return readMenuUnlocked();
        } finally {
            lock.unlock();
        }
    }

    /** Atomic read-modify-write used by every admin mutation. */
    public List<MenuSectionResponse> updateMenu(UnaryOperator<List<MenuSectionResponse>> mutation) {
        lock.lock();
        try {
            List<MenuSectionResponse> updated = mutation.apply(readMenuUnlocked());
            validate(updated);
            writeMenuUnlocked(updated);
            return List.copyOf(updated);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not update runtime menu " + menuFile, e);
        } finally {
            lock.unlock();
        }
    }

    public void ensureBackups() {
        lock.lock();
        try {
            ensureBackupsUnlocked();
        } catch (IOException e) {
            log.error("Menu backup cycle failed", e);
        } finally {
            lock.unlock();
        }
    }

    public List<MenuSectionResponse> readDefaultMenu() {
        ClassPathResource resource = new ClassPathResource(properties.getDefaultResource());
        try (InputStream input = resource.getInputStream()) {
            MenuOverridesDocument document = objectMapper.readValue(input, MenuOverridesDocument.class);
            List<MenuSectionResponse> sections = requireSections(document, "classpath:" + properties.getDefaultResource());
            validate(sections);
            return sections;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read default menu resource " + properties.getDefaultResource(), e);
        }
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    public Path menuFile() {
        return menuFile;
    }

    public Path dailyDirectory() {
        return dailyDirectory;
    }

    public Path monthlyDirectory() {
        return monthlyDirectory;
    }

    private List<MenuSectionResponse> readMenuUnlocked() {
        try {
            if (!Files.isRegularFile(menuFile) || Files.size(menuFile) == 0) {
                throw new IllegalStateException("Runtime menu is missing or empty: " + menuFile);
            }
            MenuOverridesDocument document = objectMapper.readValue(menuFile.toFile(), MenuOverridesDocument.class);
            List<MenuSectionResponse> sections = requireSections(document, menuFile.toString());
            validate(sections);
            return sections;
        } catch (IOException e) {
            throw new UncheckedIOException("Runtime menu is not valid JSON: " + menuFile, e);
        }
    }

    private void writeMenuUnlocked(List<MenuSectionResponse> sections) throws IOException {
        validate(sections);
        MenuOverridesDocument document = new MenuOverridesDocument(Instant.now(clock), sections);
        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(document);
        validateSerialized(bytes);
        writeAtomically(menuFile, bytes);
    }

    private void ensureBackupsUnlocked() throws IOException {
        if (!Files.isRegularFile(menuFile) || Files.size(menuFile) == 0) {
            log.error("Skipping backups: runtime menu is missing or empty at {}", menuFile);
            return;
        }

        byte[] source = Files.readAllBytes(menuFile);
        try {
            validateSerialized(source);
        } catch (RuntimeException | IOException e) {
            log.error("Skipping backups: runtime menu is corrupt at {}", menuFile, e);
            return;
        }

        LocalDate today = LocalDate.now(clock.withZone(ROME));
        YearMonth month = YearMonth.from(today);
        createBackupIfMissing(dailyDirectory.resolve("menu-" + today + ".json"), source);
        createBackupIfMissing(monthlyDirectory.resolve("menu-" + month + ".json"), source);
        cleanupDaily(today.minusDays(29));
        cleanupMonthly(month.minusMonths(11));
    }

    private void createBackupIfMissing(Path target, byte[] source) throws IOException {
        if (Files.exists(target)) {
            try {
                validateSerialized(Files.readAllBytes(target));
                return;
            } catch (RuntimeException | IOException e) {
                log.error("Existing backup is invalid and will be replaced: {}", target, e);
            }
        }
        writeAtomically(target, source);
        log.info("Menu backup created at {}", target);
    }

    private void cleanupDaily(LocalDate oldestToKeep) throws IOException {
        cleanup(dailyDirectory, DAILY_NAME, name -> LocalDate.parse(name).isBefore(oldestToKeep));
    }

    private void cleanupMonthly(YearMonth oldestToKeep) throws IOException {
        cleanup(monthlyDirectory, MONTHLY_NAME, name -> YearMonth.parse(name).isBefore(oldestToKeep));
    }

    private void cleanup(Path directory, Pattern pattern, java.util.function.Predicate<String> expired) throws IOException {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "menu-*.json")) {
            for (Path file : files) {
                Matcher matcher = pattern.matcher(file.getFileName().toString());
                if (matcher.matches() && expired.test(matcher.group(1))) {
                    Files.deleteIfExists(file);
                    log.info("Expired menu backup removed: {}", file);
                }
            }
        }
    }

    protected void writeAtomically(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            Files.write(temp, bytes, StandardOpenOption.TRUNCATE_EXISTING);
            move(temp, target);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void validateSerialized(byte[] bytes) throws IOException {
        if (bytes.length == 0) {
            throw new IllegalArgumentException("Menu JSON cannot be empty");
        }
        MenuOverridesDocument document = objectMapper.readValue(bytes, MenuOverridesDocument.class);
        List<MenuSectionResponse> sections = requireSections(document, "serialized menu");
        validate(sections);
    }

    static void validate(List<MenuSectionResponse> sections) {
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("The menu must contain at least one category");
        }
        Set<String> sectionIds = new HashSet<>();
        Set<String> itemIds = new HashSet<>();
        for (MenuSectionResponse section : sections) {
            if (section == null || blank(section.id()) || blank(section.title()) || section.items() == null) {
                throw new IllegalArgumentException("Every category needs an id, title and item list");
            }
            if (!sectionIds.add(section.id())) {
                throw new IllegalArgumentException("Duplicate category id: " + section.id());
            }
            for (MenuItemResponse item : section.items()) {
                if (item == null || blank(item.id()) || blank(item.name())) {
                    throw new IllegalArgumentException("Every dish needs an id and name");
                }
                if (!itemIds.add(item.id())) {
                    throw new IllegalArgumentException("Duplicate dish id: " + item.id());
                }
                if (item.price() != null && item.price().signum() < 0) {
                    throw new IllegalArgumentException("Negative price for: " + item.id());
                }
                if (item.price() != null && item.price().scale() > 2) {
                    throw new IllegalArgumentException("Price has more than two decimals for: " + item.id());
                }
            }
        }
    }

    private Map<String, String> readLegacyPrices() {
        if (legacyOverridesFile == null || legacyOverridesFile.equals(menuFile)
                || !Files.isRegularFile(legacyOverridesFile)) {
            return Map.of();
        }
        try {
            MenuOverridesDocument document = objectMapper.readValue(legacyOverridesFile.toFile(),
                    MenuOverridesDocument.class);
            return document.prices() == null ? Map.of() : document.prices();
        } catch (IOException e) {
            log.warn("Legacy price overrides at {} could not be migrated", legacyOverridesFile, e);
            return Map.of();
        }
    }

    private static List<MenuSectionResponse> applyLegacyPrices(List<MenuSectionResponse> sections,
            Map<String, String> prices) {
        if (prices.isEmpty()) {
            return sections;
        }
        List<MenuSectionResponse> migrated = new ArrayList<>();
        for (MenuSectionResponse section : sections) {
            List<MenuItemResponse> items = new ArrayList<>();
            for (MenuItemResponse item : section.items()) {
                String legacy = prices.get(item.id());
                items.add(legacy == null ? item : new MenuItemResponse(item.id(), item.name(), item.subtitle(),
                        item.description(), item.notes(), MenuItemResponse.parsePrice(legacy)));
            }
            migrated.add(new MenuSectionResponse(section.id(), section.title(), section.description(), items));
        }
        return migrated;
    }

    private static List<MenuSectionResponse> requireSections(MenuOverridesDocument document, String source) {
        if (document == null || document.sections() == null) {
            throw new IllegalArgumentException("Menu sections are missing in " + source);
        }
        return List.copyOf(document.sections());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
