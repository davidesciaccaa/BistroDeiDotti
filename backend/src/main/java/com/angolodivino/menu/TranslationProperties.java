package com.angolodivino.menu;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.translation")
public class TranslationProperties {
    private boolean enabled;
    private String authKey = "";
    private String sourceLanguage = "IT";
    private List<String> targetLanguages = List.of("EN", "DE");
    private Duration timeout = Duration.ofSeconds(10);
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAuthKey() { return authKey; }
    public void setAuthKey(String authKey) { this.authKey = authKey == null ? "" : authKey; }
    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }
    public List<String> getTargetLanguages() { return targetLanguages; }
    public void setTargetLanguages(List<String> targetLanguages) { this.targetLanguages = List.copyOf(targetLanguages); }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
}
