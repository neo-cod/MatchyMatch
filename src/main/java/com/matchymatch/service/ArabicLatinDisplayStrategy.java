package com.matchymatch.service;

import com.matchymatch.domain.VocabularyItem;

// ArabicLatinDisplayStrategy sınıfı: Strategy Tasarım Deseninin ikinci somut (concrete) strateji uygulamasıdır.
// Arapça seçildiğinde kelimeleri Latin harfli okunuşuyla (Örn: "Marhaba") döndürür; diğer 5 dilde standart çeviriyi döndürür.
public class ArabicLatinDisplayStrategy implements LanguageDisplayStrategy {

    @Override
    public String resolveText(VocabularyItem item, String language) {
        if (item == null) {
            return "";
        }
        return item.getTextForLanguage(language, true);
    }

    @Override
    public String getStrategyName() {
        return "Latin Alphabet (Marhaba)";
    }
}
