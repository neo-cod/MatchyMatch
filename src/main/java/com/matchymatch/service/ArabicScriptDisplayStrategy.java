package com.matchymatch.service;

import com.matchymatch.domain.VocabularyItem;

// ArabicScriptDisplayStrategy sınıfı: Strategy Tasarım Deseninin birinci somut (concrete) strateji uygulamasıdır.
// Arapça seçildiğinde kelimeleri orijinal Arap alfabesiyle (Örn: "مرحبا") döndürür; diğer 5 dilde standart çeviriyi döndürür.
public class ArabicScriptDisplayStrategy implements LanguageDisplayStrategy {

    @Override
    public String resolveText(VocabularyItem item, String language) {
        if (item == null) {
            return "";
        }
        return item.getTextForLanguage(language, false);
    }

    @Override
    public String getStrategyName() {
        return "Arabic Script (مرحبا)";
    }
}
