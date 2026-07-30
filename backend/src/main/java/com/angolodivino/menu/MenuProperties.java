package com.angolodivino.menu;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.menu")
public class MenuProperties {

    /** Relative paths are resolved from the backend process working directory. */
    @NotBlank
    private String dataDirectory = "data";

    /** Classpath seed used only when the runtime catalogue does not exist yet. */
    @NotBlank
    private String defaultResource = "menu.default.json";

    /** Previous deployment format; read once during first-run migration when present. */
    private String legacyOverridesFile = "data/menu-overrides.json";

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public String getDefaultResource() {
        return defaultResource;
    }

    public void setDefaultResource(String defaultResource) {
        this.defaultResource = defaultResource;
    }

    public String getLegacyOverridesFile() {
        return legacyOverridesFile;
    }

    public void setLegacyOverridesFile(String legacyOverridesFile) {
        this.legacyOverridesFile = legacyOverridesFile;
    }
}
