package com.angolodivino.menu;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLClientOptions;
import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.TextTranslationOptions;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeepLMenuTranslationService implements MenuTranslationService {
    private static final String CONTEXT = "Testi di un menu italiano di cocktail, vini, aperitivi, liquori e piatti da ristorante. "
            + "Preserva nomi propri, marchi, vini, piatti tradizionali, cocktail, sigle, parentesi e punteggiatura.";
    private final TranslationProperties properties;

    public DeepLMenuTranslationService(TranslationProperties properties) { this.properties = properties; }

    @Override
    public List<String> translate(List<String> texts, String targetLanguage) {
        if (!properties.isEnabled()) {
            throw new MenuTranslationException("translation_disabled", "La traduzione automatica non è configurata.");
        }
        if (properties.getAuthKey().isBlank()) {
            throw new MenuTranslationException("translation_key_missing",
                    "La traduzione automatica è abilitata ma DEEPL_AUTH_KEY non è configurata.");
        }
        if (texts.isEmpty()) return List.of();
        if (!properties.getTargetLanguages().stream().anyMatch(targetLanguage::equalsIgnoreCase)) {
            throw new MenuTranslationException("unsupported_language", "Lingua di traduzione non supportata.");
        }
        DeepLClientOptions options = new DeepLClientOptions();
        options.setTimeout(properties.getTimeout());
        options.setMaxRetries(1);
        options.setAppInfo("bistro-dei-dotti", "0.1.0");
        TextTranslationOptions textOptions = new TextTranslationOptions()
                .setContext(CONTEXT).setPreserveFormatting(true);
        try {
            List<TextResult> results = new DeepLClient(properties.getAuthKey(), options)
                    .translateText(texts, properties.getSourceLanguage(), targetLanguage, textOptions);
            return results.stream().map(TextResult::getText).toList();
        } catch (DeepLException e) {
            throw new MenuTranslationException("translation_unavailable",
                    "Il servizio di traduzione è temporaneamente non disponibile.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MenuTranslationException("translation_interrupted", "La richiesta di traduzione è stata interrotta.", e);
        }
    }
}
