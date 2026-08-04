package com.angolodivino.menu;

import jakarta.validation.constraints.Size;
import java.util.List;

/** Localized, editable text stored in the same document as the Italian menu item. */
public record LocalizedMenuItemText(
        @Size(max = 160) String name,
        @Size(max = 120) String subtitle,
        @Size(max = 1000) String description,
        @Size(max = 30) List<@Size(max = 160) String> notes) {
    public LocalizedMenuItemText {
        notes = notes == null ? null : List.copyOf(notes);
    }
}
