package com.angolodivino.menu;

import java.math.BigDecimal;
import java.util.List;

public record MenuItemResponse(
        String id,
        String name,
        String subtitle,
        String description,
        List<String> notes,
        BigDecimal price
) {
    /** Keeps the seeded menu readable while exposing and persisting a numeric price. */
    public MenuItemResponse(String id, String name, String subtitle, String description, List<String> notes, String price) {
        this(id, name, subtitle, description, notes, parsePrice(price));
    }

    private static BigDecimal parsePrice(String price) {
        if (price == null || price.isBlank() || "-".equals(price.trim())) return null;
        String numeric = price.replaceAll("[^0-9,.-]", "").replace(',', '.');
        return numeric.isBlank() ? null : new BigDecimal(numeric);
    }
}
