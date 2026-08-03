package com.angolodivino.menu;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Reads current numeric prices and the single-value strings written by older
 * deployments. Serialization still uses {@link BigDecimal}, so every API
 * response and every subsequent save remains numeric.
 */
final class PriceDeserializer extends StdDeserializer<BigDecimal> {

    PriceDeserializer() {
        super(BigDecimal.class);
    }

    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
            return parser.getDecimalValue();
        }
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token == JsonToken.VALUE_STRING) {
            String value = parser.getValueAsString();
            try {
                return MenuItemResponse.parsePrice(value);
            } catch (IllegalArgumentException exception) {
                throw InvalidFormatException.from(
                        parser,
                        "Formato prezzo non convertibile",
                        value,
                        BigDecimal.class);
            }
        }
        return (BigDecimal) context.handleUnexpectedToken(BigDecimal.class, parser);
    }
}
