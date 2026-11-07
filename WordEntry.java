import java.util.Locale;

// WordEntry sınıfı: "words.csv" dosyasındaki tek bir kelime veya ifade (phrase) satırını temsil eden veri modelidir.
// Nesne Yönelimli Programlama (OOP) kapsülleme (encapsulation) ilkesine uygun olarak tüm alanlar "private final"
// tanımlanmış ve dışarıya yalnızca okuma (getter) metotları ile açılmıştır.
public class WordEntry {

    // Kelime/ifade satırına ait temel özellikler ve 6 dildeki karşılıkları
    private final int id;
    private final String difficulty;   // Zorluk seviyesi: "EASY", "MEDIUM" veya "HARD"
    private final String type;         // Kategori türü: "WORD" (Kelime) veya "PHRASE" (İfade)
    private final String english;      // İngilizce karşılığı (EN)
    private final String turkish;      // Türkçe karşılığı (TR)
    private final String french;       // Fransızca karşılığı (FR)
    private final String italian;      // İtalyanca karşılığı (IT)
    private final String hungarian;    // Macarca karşılığı (HU)
    private final String arabicScript; // Arapça orijinal alfabe yazılışı (Örn: مرحبا)
    private final String arabicLatin;  // Arapça Latin harfli okunuşu (Örn: Marhaba)

    // Kurucu metot (Constructor): CSV dosyasından okunan verilerle yeni bir WordEntry nesnesi oluşturur.
    // Türkçe işletim sistemlerinde 'i' -> 'İ' dönüşüm hatasını önlemek için Locale.ROOT kullanılır.
    public WordEntry(int id, String difficulty, String type,
                     String english, String turkish, String french,
                     String italian, String hungarian,
                     String arabicScript, String arabicLatin) {
        this.id = id;
        this.difficulty = difficulty.trim().toUpperCase(Locale.ROOT);
        this.type = type.trim().toUpperCase(Locale.ROOT);
        this.english = english.trim();
        this.turkish = turkish.trim();
        this.french = french.trim();
        this.italian = italian.trim();
        this.hungarian = hungarian.trim();
        this.arabicScript = arabicScript.trim();
        this.arabicLatin = arabicLatin.trim();
    }

    // İstenen dil adına (Örn: "English", "Turkish", "Arabic") göre ilgili çeviri metnini döndürür.
    // Eğer Arapça seçiliyse, kullanıcının menüdeki tercihine (useArabicLatin) göre Arap alfabesi veya Latin okunuşu döner.
    public String getTextForLanguage(String languageName, boolean useArabicLatin) {
        if (languageName == null) {
            return english;
        }

        // Dil adını büyük harfe çevirirken Locale.ROOT kullanarak "ARABIC", "TURKISH", "ITALIAN" eşleşmesini garanti eder
        String key = languageName.trim().toUpperCase(Locale.ROOT);
        switch (key) {
            case "ENGLISH":
            case "EN":
                return english;
            case "TURKISH":
            case "TR":
                return turkish;
            case "FRENCH":
            case "FR":
                return french;
            case "ITALIAN":
            case "IT":
                return italian;
            case "HUNGARIAN":
            case "HU":
                return hungarian;
            case "ARABIC":
            case "AR":
                if (useArabicLatin) {
                    return arabicLatin;
                } else {
                    return arabicScript;
                }
            default:
                return english;
        }
    }

    // Dilin tam adını "records.txt" dosyasında kullanılan 2 harfli kısa koda (EN, TR, FR, IT, HU, AR) dönüştürür.
    public static String getLanguageCode(String languageName) {
        if (languageName == null) {
            return "EN";
        }
        String key = languageName.trim().toUpperCase(Locale.ROOT);
        switch (key) {
            case "ENGLISH":   return "EN";
            case "TURKISH":   return "TR";
            case "FRENCH":    return "FR";
            case "ITALIAN":   return "IT";
            case "HUNGARIAN": return "HU";
            case "ARABIC":    return "AR";
            default:          return "EN";
        }
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

    public String getType() {
        return type;
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
}
