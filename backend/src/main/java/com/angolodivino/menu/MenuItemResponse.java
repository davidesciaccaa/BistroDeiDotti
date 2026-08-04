package com.angolodivino.menu;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MenuItemResponse(
        String id,
        String name,
        String subtitle,
        String description,
        List<String> notes,
        @JsonDeserialize(using = PriceDeserializer.class)
        BigDecimal price,
        Map<String, LocalizedMenuItemText> translations
) {
    public MenuItemResponse {
        notes = notes == null ? List.of() : List.copyOf(notes);
        price = price == null ? null : price.stripTrailingZeros();
        translations = translations == null ? Map.of() : Map.copyOf(translations);
    }

    public MenuItemResponse(String id, String name, String subtitle, String description,
            List<String> notes, BigDecimal price) {
        this(id, name, subtitle, description, notes, price, Map.of());
    }

    public static BigDecimal parsePrice(String price) {
        if (price == null || price.isBlank() || "-".equals(price.trim())) return null;
        String numeric = price.trim().replace("€", "").trim().replace(',', '.');
        if (!numeric.matches("\\d+(?:\\.\\d{1,2})?")) {
            throw new IllegalArgumentException("Formato prezzo non convertibile: " + price);
        }
        return new BigDecimal(numeric);
    }
}
