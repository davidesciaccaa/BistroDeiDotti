package com.angolodivino.menu;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;
import java.util.List;

public record MenuItemResponse(
        String id,
        String name,
        String subtitle,
        String description,
        List<String> notes,
        @JsonDeserialize(using = PriceDeserializer.class)
        BigDecimal price
) {
    public MenuItemResponse {
        notes = notes == null ? List.of() : List.copyOf(notes);
        price = price == null ? null : price.stripTrailingZeros();
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
