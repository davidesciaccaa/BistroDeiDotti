package com.angolodivino.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

public record UpdatePricesRequest(
        @NotEmpty @Size(max = 500) Map<String, @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal> prices
) {
}
