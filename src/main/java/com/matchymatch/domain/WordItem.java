package com.matchymatch.domain;

// WordItem sınıfı: VocabularyItem soyut sınıfından kalıtım (inheritance) alan ve tek kelimelik
// ("Word" modu) öğeleri temsil eden somut (concrete) alt sınıftır.
public class WordItem extends VocabularyItem {

    // Kurucu metot (Constructor): Üst sınıfın (VocabularyItem) yapıcısını çağırarak alanları başlatır.
    public WordItem(int id, String difficulty,
                    String english, String turkish, String french,
                    String italian, String hungarian,
                    String arabicScript, String arabicLatin) {
        super(id, difficulty, english, turkish, french, italian, hungarian, arabicScript, arabicLatin);
    }

    // Çok biçimlilik (Polymorphism): Bu nesnenin "Word" (Kelime) moduna ait olduğunu döndürür.
    @Override
    public String getModeType() {
        return "Word";
    }

    // Çok biçimlilik (Polymorphism): Doğru kelime eşleşmesinde kazanılan sabit +100 puanı döndürür.
    @Override
    public int getBasePoints() {
        return DEFAULT_MATCH_POINTS;
    }

    // Çok biçimlilik (Polymorphism): Tekil kelime metninin kenar boşluklarını temizleyerek döndürür.
    @Override
    public String formatDisplayText(String rawText) {
        if (rawText == null) {
            return "";
        }
        return rawText.trim();
    }
}
