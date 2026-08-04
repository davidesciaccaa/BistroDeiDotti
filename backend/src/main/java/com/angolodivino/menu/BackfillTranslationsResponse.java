package com.angolodivino.menu;

import java.util.List;

public record BackfillTranslationsResponse(int updatedItems, int completeItems, List<MenuSectionResponse> sections) { }
