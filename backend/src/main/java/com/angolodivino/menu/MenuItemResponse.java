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
    public MenuItemResponse {
        notes = notes == null ? List.of() : List.copyOf(notes);
        price = price == null ? null : price.stripTrailingZeros();
    }

    public static BigDecimal parsePrice(String price) {
        if (price == null || price.isBlank() || "-".equals(price.trim())) return null;
        String numeric = price.replaceAll("[^0-9,.-]", "").replace(',', '.');
        if (numeric.indexOf('.') != numeric.lastIndexOf('.')) {
            throw new IllegalArgumentException("Formato prezzo non convertibile: " + price);
        }
        return numeric.isBlank() ? null : new BigDecimal(numeric);
    }
}
