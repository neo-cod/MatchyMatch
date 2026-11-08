package com.matchymatch.domain;

import java.util.Locale;

// VocabularyItem soyut (abstract) sınıfı: "words.csv" dosyasındaki öğrenilebilir bir öğeyi temsil eden üst sınıftır.
// Nesne Yönelimli Programlama (OOP) ilkeleri:
// 1. Kapsülleme (Encapsulation): Tüm alanlar "private final" olarak korunur ve yapıcı metotta doğrulanır.
// 2. Soyutlama (Abstraction): Ortak özellikleri tutarken alt sınıfların (WordItem ve PhraseItem) kendi davranışlarını
//    belirlemesi için getModeType(), getBasePoints() ve formatDisplayText() soyut metotlarını tanımlar.
public abstract class VocabularyItem {

    // Her eşleşmede kazanılan varsayılan taban puan sabiti
    public static final int DEFAULT_MATCH_POINTS = 100;

    // Kapsüllenmiş (private final) ortak veri alanları
    private final int id;
    private final String difficulty;   // Zorluk seviyesi: "EASY", "MEDIUM" veya "HARD"
    private final String english;      // İngilizce karşılığı (EN)
    private final String turkish;      // Türkçe karşılığı (TR)
    private final String french;       // Fransızca karşılığı (FR)
    private final String italian;      // İtalyanca karşılığı (IT)
    private final String hungarian;    // Macarca karşılığı (HU)
    private final String arabicScript; // Arapça orijinal alfabe yazılışı (Örn: مرحبا)
    private final String arabicLatin;  // Arapça Latin harfli okunuşu (Örn: Marhaba)

    // Kurucu metot (Constructor): Parametreleri doğrular ve geçerli bir VocabularyItem nesnesi oluşturur.
    // Türkçe işletim sistemlerinde 'i' -> 'İ' dönüşüm hatasını önlemek için Locale.ROOT kullanılır.
    protected VocabularyItem(int id, String difficulty,
                             String english, String turkish, String french,
                             String italian, String hungarian,
                             String arabicScript, String arabicLatin) {
        if (id <= 0) {
            throw new IllegalArgumentException("VocabularyItem id must be positive: " + id);
        }
        if (difficulty == null || difficulty.trim().isEmpty()) {
            throw new IllegalArgumentException("Difficulty cannot be null or empty.");
        }
        if (english == null || english.trim().isEmpty()) {
            throw new IllegalArgumentException("English translation cannot be null or empty.");
        }

        this.id = id;
        this.difficulty = difficulty.trim().toUpperCase(Locale.ROOT);
        this.english = english.trim();
        this.turkish = (turkish == null) ? "" : turkish.trim();
        this.french = (french == null) ? "" : french.trim();
        this.italian = (italian == null) ? "" : italian.trim();
        this.hungarian = (hungarian == null) ? "" : hungarian.trim();
        this.arabicScript = (arabicScript == null) ? "" : arabicScript.trim();
        this.arabicLatin = (arabicLatin == null) ? "" : arabicLatin.trim();
    }

    // =========================================================================
    // Soyut Metotlar (Alt Sınıflar Tarafından Ezilecek - Runtime Polymorphism)
    // =========================================================================

    // Öğenin ait olduğu mod adını ("Word" veya "Phrase") döndürür.
    public abstract String getModeType();

    // Doğru eşleşme yapıldığında bu öğenin kazandırdığı puanı (+100) çok biçimli (polymorphic) olarak döndürür.
    public abstract int getBasePoints();

    // Ham çeviri metnini alt sınıfın kurallarına göre ekranda gösterilecek biçime hazırlar.
    public abstract String formatDisplayText(String rawText);

    // =========================================================================
    // Ortak Dil Çözümleme ve Yardımcı Metotlar
    // =========================================================================

    // İstenen dil adına (Örn: "English", "Turkish", "Arabic") göre ilgili çeviri metnini döndürür.
    // Eğer Arapça seçiliyse, useArabicLatin parametresine göre Arap alfabesi veya Latin okunuşu döner.
    public String getTextForLanguage(String languageName, boolean useArabicLatin) {
        if (languageName == null) {
            return formatDisplayText(english);
        }

        String key = languageName.trim().toUpperCase(Locale.ROOT);
        String rawResult;
        switch (key) {
            case "ENGLISH":
            case "EN":
                rawResult = english;
                break;
            case "TURKISH":
            case "TR":
                rawResult = turkish;
                break;
            case "FRENCH":
            case "FR":
                rawResult = french;
                break;
            case "ITALIAN":
            case "IT":
                rawResult = italian;
                break;
            case "HUNGARIAN":
            case "HU":
                rawResult = hungarian;
                break;
            case "ARABIC":
            case "AR":
                if (useArabicLatin) {
                    rawResult = arabicLatin;
                } else {
                    rawResult = arabicScript;
                }
                break;
            default:
                rawResult = english;
                break;
        }
        return formatDisplayText(rawResult);
    }

    // CSV dosyasına yazarken kullanılan büyük harfli tür kodunu ("WORD" veya "PHRASE") döndürür.
    public String getType() {
        return getModeType().toUpperCase(Locale.ROOT);
    }

    // Dilin tam adını "records.txt" dosyasında kullanılan 2 harfli kısa koda (EN, TR, FR, IT, HU, AR) dönüştürür.
    public static String getLanguageCode(String languageName) {
        if (languageName == null) {
            return "EN";
        }
        String key = languageName.trim().toUpperCase(Locale.ROOT);
        switch (key) {
            case "ENGLISH":
            case "EN":
                return "EN";
            case "TURKISH":
            case "TR":
                return "TR";
            case "FRENCH":
            case "FR":
                return "FR";
            case "ITALIAN":
            case "IT":
                return "IT";
            case "HUNGARIAN":
            case "HU":
                return "HU";
            case "ARABIC":
            case "AR":
                return "AR";
            default:
                return "EN";
        }
    }

    // Verilen dil adının desteklenen 6 dilden biri olup olmadığını kontrol eder.
    public static boolean isSupportedLanguage(String languageName) {
        if (languageName == null || languageName.trim().isEmpty()) {
            return false;
        }
        String key = languageName.trim().toUpperCase(Locale.ROOT);
        return "ENGLISH".equals(key) || "EN".equals(key)
                || "TURKISH".equals(key) || "TR".equals(key)
                || "FRENCH".equals(key) || "FR".equals(key)
                || "ITALIAN".equals(key) || "IT".equals(key)
                || "HUNGARIAN".equals(key) || "HU".equals(key)
                || "ARABIC".equals(key) || "AR".equals(key);
    }

    // =========================================================================
    // Kapsüllenmiş (Private) Alanlar İçin Standart Getter Metotları
    // =========================================================================

    public int getId() {
        return id;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getEnglish() {
        return english;
    }

    public String getTurkish() {
        return turkish;
    }

    public String getFrench() {
        return french;
    }

    public String getItalian() {
        return italian;
    }

    public String getHungarian() {
        return hungarian;
    }

    public String getArabicScript() {
        return arabicScript;
    }

    public String getArabicLatin() {
        return arabicLatin;
    }

    @Override
    public String toString() {
        return getModeType() + "[" + id + ", " + difficulty + ", EN=" + english + "]";
    }
}
