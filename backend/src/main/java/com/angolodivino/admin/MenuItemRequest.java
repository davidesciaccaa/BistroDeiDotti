package com.angolodivino.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record MenuItemRequest(
        @NotBlank @Size(max = 80) String sectionId,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 120) String subtitle,
        @Size(max = 1000) String description,
        @Size(max = 30) List<@Size(max = 160) String> notes,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal price
) {}
