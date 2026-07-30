package com.angolodivino.admin;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.angolodivino.menu.MenuOverridesStore;
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
    private static final Path DATA_DIRECTORY = createTempDataDirectory();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.admin.password", () -> PASSWORD);
        registry.add("app.menu.data-directory", DATA_DIRECTORY::toString);
        registry.add("app.menu.legacy-overrides-file", () -> "");
    }

    private static Path createTempDataDirectory() {
        try {
            return Files.createTempDirectory("admin-controller-test").resolve("data");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MenuOverridesStore menuStore;

    @BeforeEach
    void resetMenu() {
        menuStore.updateMenu(ignored -> menuStore.readDefaultMenu());
    }

    @Test
    void rejectsMenuAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/menu/sections"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("unauthorized")));
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/admin/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("invalid_password")));
    }

    @Test
    void numericPricePatchUpdatesPublicMenu() throws Exception {
        String token = login();
        mockMvc.perform(patch("/api/admin/menu/prices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prices\":{\"negroni\":11}}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/menu/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].items[?(@.id == 'negroni')].price", is(List.of(11))));
    }

    @Test
    void createsUpdatesAndDeletesCompleteMenuItems() throws Exception {
        String token = login();
        String body = mockMvc.perform(post("/api/admin/menu/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sectionId":"antipasti","name":"Nuovo piatto","subtitle":"Novità",
                                 "description":"Descrizione","notes":["Senza glutine"],"price":12.50}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].items[?(@.name == 'Nuovo piatto')].price", is(List.of(12.5))))
                .andReturn().getResponse().getContentAsString();

        String id = findItemId(objectMapper.readTree(body), "Nuovo piatto");
        mockMvc.perform(patch("/api/admin/menu/items/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sectionId":"dolci","name":"Piatto modificato","subtitle":"",
                                 "description":"Nuova descrizione","notes":[],"price":14}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].items[?(@.name == 'Piatto modificato')].price", is(List.of(14))));

        mockMvc.perform(delete("/api/admin/menu/items/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].items[?(@.id == '" + id + "')]", is(List.of())));
    }

    @Test
    void rejectsCurrencySymbolsInApiPrice() throws Exception {
        mockMvc.perform(post("/api/admin/menu/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sectionId":"antipasti","name":"Non valido","subtitle":"",
                                 "description":"","notes":[],"price":"30 €"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutInvalidatesToken() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/admin/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/menu/sections").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private static String findItemId(JsonNode sections, String name) {
        for (JsonNode section : sections) {
            for (JsonNode item : section.get("items")) {
                if (name.equals(item.get("name").asText())) return item.get("id").asText();
            }
        }
        throw new AssertionError("Item not found: " + name);
    }
}
