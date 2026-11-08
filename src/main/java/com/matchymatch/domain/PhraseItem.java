package com.matchymatch.domain;

// PhraseItem sınıfı: VocabularyItem soyut sınıfından kalıtım (inheritance) alan ve çok kelimeli
// günlük ifadeleri / atasözlerini ("Phrase" modu) temsil eden somut (concrete) alt sınıftır.
public class PhraseItem extends VocabularyItem {

    // Kurucu metot (Constructor): Üst sınıfın (VocabularyItem) yapıcısını çağırarak alanları başlatır.
    public PhraseItem(int id, String difficulty,
                      String english, String turkish, String french,
                      String italian, String hungarian,
                      String arabicScript, String arabicLatin) {
        super(id, difficulty, english, turkish, french, italian, hungarian, arabicScript, arabicLatin);
    }

    // Çok biçimlilik (Polymorphism): Bu nesnenin "Phrase" (İfade) moduna ait olduğunu döndürür.
    @Override
    public String getModeType() {
        return "Phrase";
    }

    // Çok biçimlilik (Polymorphism): Doğru ifade eşleşmesinde kazanılan sabit +100 puanı döndürür.
    @Override
    public int getBasePoints() {
        return DEFAULT_MATCH_POINTS;
    }

    // Çok biçimlilik (Polymorphism): İfade metnindeki fazla boşlukları temizleyerek ekrana hazırlar.
    @Override
    public String formatDisplayText(String rawText) {
        if (rawText == null) {
            return "";
        }
        return rawText.trim();
    }
}
