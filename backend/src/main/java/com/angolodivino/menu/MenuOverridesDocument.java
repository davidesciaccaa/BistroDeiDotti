package com.angolodivino.menu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * On-disk shape of {@code menu-overrides.json}: prices keyed by menu item id.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MenuOverridesDocument(
        Instant updatedAt,
        Map<String, String> prices,
        List<MenuSectionResponse> sections
) {
}
