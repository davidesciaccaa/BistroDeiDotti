package com.angolodivino.menu;

import java.util.List;

public interface MenuTranslationService {
    List<String> translate(List<String> texts, String targetLanguage);
}
