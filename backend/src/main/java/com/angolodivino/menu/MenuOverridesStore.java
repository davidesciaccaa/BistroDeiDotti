package com.angolodivino.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads and writes the price overrides JSON file. The file is the only persistence layer:
 * it is read on every menu request so edits are visible without a restart, and a missing or
 * unreadable file simply means "no overrides", i.e. the hardcoded prices win.
 */
@Component
public class MenuOverridesStore {

    private static final Logger log = LoggerFactory.getLogger(MenuOverridesStore.class);

    /** Owned by the store: the on-disk format must not shift with the app's web serialization settings. */
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final Path file;

    public MenuOverridesStore(MenuProperties properties) {
        this.file = Path.of(properties.getOverridesFile()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void logStartupState() {
        Map<String, String> prices = readPrices();
        if (Files.isRegularFile(file)) {
            log.info("Menu price overrides loaded from {} ({} entries)", file, prices.size());
        } else {
            log.info("No menu price overrides file at {}, using hardcoded prices", file);
        }
    }

    public Path file() {
        return file;
    }

    /**
     * @return the overridden prices keyed by item id, or an empty map when the file is absent or invalid.
     */
    public Map<String, String> readPrices() {
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }

        try {
            MenuOverridesDocument document = objectMapper.readValue(file.toFile(), MenuOverridesDocument.class);
            if (document == null || document.prices() == null) {
                return Map.of();
            }

            Map<String, String> prices = new LinkedHashMap<>();
            document.prices().forEach((id, price) -> {
                if (id != null && !id.isBlank() && price != null && !price.isBlank()) {
                    prices.put(id, price);
                }
            });
            return prices;
        } catch (IOException e) {
            log.warn("Could not read menu price overrides from {}, falling back to hardcoded prices", file, e);
            return Map.of();
        }
    }

    /** A saved full catalogue takes precedence; legacy price-only files remain supported. */
    public List<MenuSectionResponse> readSections() {
        if (!Files.isRegularFile(file)) return List.of();
        try {
            MenuOverridesDocument document = objectMapper.readValue(file.toFile(), MenuOverridesDocument.class);
            return document == null || document.sections() == null ? List.of() : document.sections();
        } catch (IOException e) {
            log.warn("Could not read menu catalogue from {}", file, e);
            return List.of();
        }
    }

    public synchronized void writeSections(List<MenuSectionResponse> sections) {
        writeDocument(new MenuOverridesDocument(Instant.now(), Map.of(), sections));
    }

    /**
     * Replaces the overrides file with the given prices, writing atomically so a crash mid-write
     * cannot leave a truncated file behind.
     */
    public synchronized void writePrices(Map<String, String> prices) {
        writeDocument(new MenuOverridesDocument(Instant.now(), new TreeMap<>(prices), List.of()));
    }

    private void writeDocument(MenuOverridesDocument document) {

        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path temp = Files.createTempFile(parent, "menu-overrides", ".json.tmp");
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), document);
                move(temp, file);
            } finally {
                Files.deleteIfExists(temp);
            }

            log.info("Menu catalogue saved to {}", file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write menu price overrides to " + file, e);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
