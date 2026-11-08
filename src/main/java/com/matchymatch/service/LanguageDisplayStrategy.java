package com.matchymatch.service;

import com.matchymatch.domain.VocabularyItem;

// LanguageDisplayStrategy arayüzü (Interface): Strategy Tasarım Deseninin (Strategy Design Pattern) soyut strateji arayüzüdür.
// Seçilen dile ve Arapça alfabe kuralına göre bir VocabularyItem nesnesinin ekranda nasıl gösterileceğini çalışma zamanında
// (runtime) dinamik olarak değiştirmeyi sağlar.
public interface LanguageDisplayStrategy {

    // Verilen VocabularyItem nesnesinin belirtilen dildeki gösterim metnini çözümler ve döndürür.
    String resolveText(VocabularyItem item, String language);

    // Stratejinin açıklayıcı adını döndürür.
    String getStrategyName();
}
