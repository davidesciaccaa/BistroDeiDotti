package com.angolodivino.admin;

import com.angolodivino.menu.LocalizedMenuItemText;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MenuItemRequest(
        @NotBlank @Size(max = 80) String sectionId,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 120) String subtitle,
        @Size(max = 1000) String description,
        @Size(max = 30) List<@Size(max = 160) String> notes,
        @DecimalMin(value = "0.0", inclusive = true) @Digits(integer = 8, fraction = 2) BigDecimal price,
        Map<String, @Valid LocalizedMenuItemText> translations,
        Boolean autoTranslate
) {
    public MenuItemRequest(String sectionId, String name, String subtitle, String description,
            List<String> notes, BigDecimal price) {
        this(sectionId, name, subtitle, description, notes, price, null, false);
    }
}
