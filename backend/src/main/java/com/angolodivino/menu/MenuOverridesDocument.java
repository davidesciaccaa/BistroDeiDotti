package com.angolodivino.menu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Runtime JSON format. The legacy prices field is read only to migrate deployments
 * that used menu-overrides.json before the complete catalogue became editable.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MenuOverridesDocument(
        Instant updatedAt,
        List<MenuSectionResponse> sections,
        Map<String, String> prices
) {
    public MenuOverridesDocument(Instant updatedAt, List<MenuSectionResponse> sections) {
        this(updatedAt, sections, null);
    }
}
