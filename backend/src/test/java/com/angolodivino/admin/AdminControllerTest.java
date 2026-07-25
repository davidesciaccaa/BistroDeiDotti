package com.angolodivino.admin;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    private static final String PASSWORD = "test-secret";

    /** Resolved eagerly: @DynamicPropertySource runs before JUnit would inject a @TempDir. */
    private static final Path OVERRIDES_FILE = createTempOverridesFile();

    @DynamicPropertySource
    static void adminProperties(DynamicPropertyRegistry registry) {
        registry.add("app.admin.password", () -> PASSWORD);
        registry.add("app.menu.overrides-file", OVERRIDES_FILE::toString);
    }

    private static Path createTempOverridesFile() {
        try {
            return Files.createTempDirectory("admin-controller-test").resolve("menu-overrides.json");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanOverridesFile() throws IOException {
        Files.deleteIfExists(OVERRIDES_FILE);
    }

    @Test
    void rejectsMenuAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/menu/sections"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Test
    void rejectsMenuAccessWithBogusToken() throws Exception {
        mockMvc.perform(get("/api/admin/menu/sections")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("invalid_password")));
    }

    @Test
    void loginReturnsTokenThatUnlocksTheMenu() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/admin/menu/sections")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("antipasti")));
    }

    @Test
    void patchPricesUpdatesThePublicMenu() throws Exception {
        String token = login();

        mockMvc.perform(patch("/api/admin/menu/prices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prices\":{\"negroni\":\"11 €\"}}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/menu/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].items[?(@.id == 'negroni')].price", is(List.of("11 €"))));
    }

    @Test
    void patchPricesRejectsUnknownItem() throws Exception {
        String token = login();

        mockMvc.perform(patch("/api/admin/menu/prices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prices\":{\"voce_inesistente\":\"11 €\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("invalid_request")));
    }

    @Test
    void logoutInvalidatesTheToken() throws Exception {
        String token = login();

        mockMvc.perform(post("/api/admin/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/menu/sections")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }
}
