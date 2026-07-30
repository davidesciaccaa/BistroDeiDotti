package com.angolodivino;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class BistroDeiDottiApplicationTests {

    private static final Path DATA_DIRECTORY = tempDataDirectory();

    @DynamicPropertySource
    static void menuStorage(DynamicPropertyRegistry registry) {
        registry.add("app.menu.data-directory", DATA_DIRECTORY::toString);
        registry.add("app.menu.legacy-overrides-file", () -> "");
    }

    @Test
    void contextLoads() {
    }

    private static Path tempDataDirectory() {
        try {
            return Files.createTempDirectory("bistro-context-test").resolve("data");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
