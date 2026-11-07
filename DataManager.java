import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// DataManager sınıfı: Uygulamanın tüm dosya okuma/yazma (File I/O) işlemlerini ve veri yapılarını yönetir.
// 1. "words.csv" dosyasını BufferedReader ile okuyarak mod ve zorluk seviyesine göre 6 ayrı ArrayList içine ayırır.
// 2. "records.txt" dosyasındaki en yüksek skor (Highest Score) ve en hızlı süre (Fastest Time) rekorlarını
//    anahtar-değer (Key-Value) mantığıyla çalışan bir HashMap<String, String> içinde tutar ve BufferedWriter ile kaydeder.
public class DataManager {

    // Kelime veritabanı ve kişisel rekorlar için kullanılan sabit dosya adları
    private static final String WORDS_FILE_NAME = "words.csv";
    private static final String RECORDS_FILE_NAME = "records.txt";

    // Kelime (Word) modu için zorluk seviyelerine göre ayrılmış 3 dinamik ArrayList koleksiyonu
    private final ArrayList<WordEntry> easyWords;
    private final ArrayList<WordEntry> mediumWords;
    private final ArrayList<WordEntry> hardWords;

    // İfade (Phrase) modu için zorluk seviyelerine göre ayrılmış 3 dinamik ArrayList koleksiyonu
    private final ArrayList<WordEntry> easyPhrases;
    private final ArrayList<WordEntry> mediumPhrases;
    private final ArrayList<WordEntry> hardPhrases;

    // Her dil/mod/zorluk kombinasyonunun (Örn: "EN-TR-WORD-EASY") rekorunu hızlıca bulmak için kullanılan HashMap
    // Anahtar (Key): "EN-TR-WORD-EASY" -> Değer (Value): "highScore,fastestTime" (Örn: "1000,24.5")
    private final HashMap<String, String> recordsMap;

    // Kurucu metot (Constructor): Koleksiyonları başlatır, "words.csv" ve "records.txt" dosyalarını belleğe yükler.
    public DataManager() {
        this.easyWords = new ArrayList<>();
        this.mediumWords = new ArrayList<>();
        this.hardWords = new ArrayList<>();

        this.easyPhrases = new ArrayList<>();
        this.mediumPhrases = new ArrayList<>();
        this.hardPhrases = new ArrayList<>();

        this.recordsMap = new HashMap<>();

        // "words.csv" dosyası yoksa oluşturur ve kelimeleri 6 ayrı ArrayList içine yükler
        ensureWordsFileExists(false);
        loadWordsFromFile();

        // Eğer diskteki dosya eksik veya bozuksa başlangıç veri setini yeniden yazıp tekrar yükler
        if (!isLoadedDataValid()) {
            ensureWordsFileExists(true);
            loadWordsFromFile();
        }

        // "records.txt" dosyasından geçmiş rekorları HashMap içine okur
        loadRecordsFromFile();
    }

    // "words.csv" dosyası diskte yoksa veya yenilenmesi gerekiyorsa BufferedWriter kullanarak UTF-8 formatında oluşturur.
    private void ensureWordsFileExists(boolean forceRegenerate) {
        File file = new File(WORDS_FILE_NAME);
        if (!forceRegenerate && file.exists() && file.length() > 0) {
            return;
        }

        String[] starterRows = getStarterDataset();
        // BufferedWriter: Disk yazma işlemlerini tamponlayarak (buffer) satır satır hızlı ve güvenli yazmayı sağlar
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            writer.write("ID,DIFFICULTY,TYPE,EN,TR,FR,IT,HU,AR_SCRIPT,AR_LATIN");
            writer.newLine();

            for (int i = 0; i < starterRows.length; i++) {
                writer.write(starterRows[i]);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error generating starter words.csv: " + e.getMessage());
        }
    }

    // "words.csv" dosyasını BufferedReader ile satır satır okur, virgüllere göre ayırır ve 6 ArrayList'e dağıtır.
    public final void loadWordsFromFile() {
        easyWords.clear();
        mediumWords.clear();
        hardWords.clear();
        easyPhrases.clear();
        mediumPhrases.clear();
        hardPhrases.clear();

        File file = new File(WORDS_FILE_NAME);
        if (!file.exists()) {
            return;
        }

        // BufferedReader: Dosyayı karakter karakter yerine satır satır (readLine) verimli şekilde okumak için kullanılır
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                // İlk satırdaki CSV sütun başlığını ("ID,DIFFICULTY,...") atlar
                if (isFirstLine && line.toUpperCase(Locale.ROOT).startsWith("ID,")) {
                    isFirstLine = false;
                    continue;
                }
                isFirstLine = false;

                // CSV satırını virgüle göre 10 sütuna ayırır
                String[] parts = line.split(",", -1);
                if (parts.length != 10) {
                    continue;
                }

                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String difficulty = parts[1].trim().toUpperCase(Locale.ROOT);
                    String type = parts[2].trim().toUpperCase(Locale.ROOT);
                    String en = parts[3].trim();
                    String tr = parts[4].trim();
                    String fr = parts[5].trim();
                    String it = parts[6].trim();
                    String hu = parts[7].trim();
                    String arScript = parts[8].trim();
                    String arLatin = parts[9].trim();

                    // Boş sütunu olan satırları güvenlik amacıyla atlar
                    if (en.isEmpty() || tr.isEmpty() || fr.isEmpty() || it.isEmpty()
                            || hu.isEmpty() || arScript.isEmpty() || arLatin.isEmpty()) {
                        continue;
                    }

                    WordEntry entry = new WordEntry(id, difficulty, type, en, tr, fr, it, hu, arScript, arLatin);
                    addEntryToCategoryList(entry);
                } catch (NumberFormatException ignored) {
                    // Sayısal ID değeri bozuk olan satırları atlar
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading words.csv: " + e.getMessage());
        }
    }

    // Yardımcı metot: Oluşturulan WordEntry nesnesini türüne (WORD/PHRASE) ve zorluğuna (EASY/MEDIUM/HARD) göre ilgili ArrayList'e ekler.
    private void addEntryToCategoryList(WordEntry entry) {
        String type = entry.getType();
        String difficulty = entry.getDifficulty();

        if ("PHRASE".equals(type)) {
            if ("EASY".equals(difficulty)) {
                easyPhrases.add(entry);
            } else if ("MEDIUM".equals(difficulty)) {
                mediumPhrases.add(entry);
            } else if ("HARD".equals(difficulty)) {
                hardPhrases.add(entry);
            } else {
                easyPhrases.add(entry);
            }
        } else {
            if ("EASY".equals(difficulty)) {
                easyWords.add(entry);
            } else if ("MEDIUM".equals(difficulty)) {
                mediumWords.add(entry);
            } else if ("HARD".equals(difficulty)) {
                hardWords.add(entry);
            } else {
                easyWords.add(entry);
            }
        }
    }

    // Yüklenen 6 listenin yeterli sayıda kelime içerdiğini ve aynı satırda iki farklı dilin birebir aynı metne sahip olmadığını doğrular.
    private boolean isLoadedDataValid() {
        if (easyWords.size() < 25 || mediumWords.size() < 25 || hardWords.size() < 25
                || easyPhrases.size() < 20 || mediumPhrases.size() < 20 || hardPhrases.size() < 20) {
            return false;
        }

        ArrayList<ArrayList<WordEntry>> allBuckets = new ArrayList<>();
        allBuckets.add(easyWords);
        allBuckets.add(mediumWords);
        allBuckets.add(hardWords);
        allBuckets.add(easyPhrases);
        allBuckets.add(mediumPhrases);
        allBuckets.add(hardPhrases);

        for (int b = 0; b < allBuckets.size(); b++) {
            ArrayList<WordEntry> bucket = allBuckets.get(b);
            for (int e = 0; e < bucket.size(); e++) {
                WordEntry entry = bucket.get(e);
                String[] vals = {
                    entry.getEnglish(),
                    entry.getTurkish(),
                    entry.getFrench(),
                    entry.getItalian(),
                    entry.getHungarian(),
                    entry.getArabicScript(),
                    entry.getArabicLatin()
                };
                for (int i = 0; i < vals.length; i++) {
                    if (vals[i] == null || vals[i].isEmpty()) {
                        return false;
                    }
                    for (int j = i + 1; j < vals.length; j++) {
                        if (vals[i].equalsIgnoreCase(vals[j])) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    // Seçilen oyun modu ("Word" veya "Phrase") ve zorluk seviyesine ("Easy", "Medium", "Hard") karşılık gelen ArrayList'i döndürür.
    public ArrayList<WordEntry> getCategoryList(String mode, String difficulty) {
        boolean isPhraseMode = (mode != null && mode.trim().toUpperCase(Locale.ROOT).equals("PHRASE"));
        String diffKey = (difficulty == null) ? "EASY" : difficulty.trim().toUpperCase(Locale.ROOT);

        if (isPhraseMode) {
            if ("EASY".equals(diffKey)) {
                return easyPhrases;
            } else if ("MEDIUM".equals(diffKey)) {
                return mediumPhrases;
            } else if ("HARD".equals(diffKey)) {
                return hardPhrases;
            } else {
                return easyPhrases;
            }
        } else {
            if ("EASY".equals(diffKey)) {
                return easyWords;
            } else if ("MEDIUM".equals(diffKey)) {
                return mediumWords;
            } else if ("HARD".equals(diffKey)) {
                return hardWords;
            } else {
                return easyWords;
            }
        }
    }

    // 10 turluk (her turda 5 eşleşme = toplam 50 öğe) bir oyun oturumu için ilgili ArrayList'ten rastgele karıştırılmış kelimeler seçer.
    // Aynı turdaki 5 kelime içinde asla tekrar (kopya kelime) olmamasını garanti eder.
    public ArrayList<WordEntry> getEntriesForSession(String mode, String difficulty, int count) {
        ArrayList<WordEntry> sourceList = getCategoryList(mode, difficulty);
        if (sourceList.isEmpty()) {
            if (!easyWords.isEmpty()) {
                sourceList = easyWords;
            } else {
                sourceList = easyPhrases;
            }
        }

        ArrayList<WordEntry> sessionPool = new ArrayList<>();
        if (sourceList.isEmpty()) {
            return sessionPool;
        }

        // Orijinal listeyi bozmamak için kopyasını oluşturup Collections.shuffle ile rastgele karıştırır
        ArrayList<WordEntry> copy = new ArrayList<>(sourceList);
        int pairsPerRound = 5;

        while (sessionPool.size() < count) {
            Collections.shuffle(copy);
            for (int c = 0; c < copy.size(); c++) {
                if (sessionPool.size() >= count) {
                    break;
                }
                WordEntry candidate = copy.get(c);

                // Mevcut 5'li tur içinde aynı ID'ye sahip bir kelimenin olup olmadığını kontrol eder
                int roundStart = (sessionPool.size() / pairsPerRound) * pairsPerRound;
                boolean duplicateInRound = false;
                for (int i = roundStart; i < sessionPool.size(); i++) {
                    if (sessionPool.get(i).getId() == candidate.getId()) {
                        duplicateInRound = true;
                        break;
                    }
                }

                if (!duplicateInRound) {
                    sessionPool.add(candidate);
                }
            }
        }
        return sessionPool;
    }

    // 6 kategori listesi için standart Getter metotları
    public ArrayList<WordEntry> getEasyWords()     { return easyWords; }
    public ArrayList<WordEntry> getMediumWords()   { return mediumWords; }
    public ArrayList<WordEntry> getHardWords()     { return hardWords; }
    public ArrayList<WordEntry> getEasyPhrases()   { return easyPhrases; }
    public ArrayList<WordEntry> getMediumPhrases() { return mediumPhrases; }
    public ArrayList<WordEntry> getHardPhrases()   { return hardPhrases; }

    // "records.txt" ve HashMap içinde her oyun ayarını tekil olarak tanımlayan anahtar metni (Örn: "EN-TR-WORD-EASY") üretir.
    public static String buildConfigKey(String appLang, String targetLang, String mode, String difficulty) {
        String appCode = WordEntry.getLanguageCode(appLang);
        String targetCode = WordEntry.getLanguageCode(targetLang);
        String modeCode = "WORD";
        if (mode != null && mode.trim().toUpperCase(Locale.ROOT).equals("PHRASE")) {
            modeCode = "PHRASE";
        }
        String diffCode = (difficulty == null) ? "EASY" : difficulty.trim().toUpperCase(Locale.ROOT);
        return appCode + "-" + targetCode + "-" + modeCode + "-" + diffCode;
    }

    // "records.txt" dosyasını BufferedReader ile okuyarak her satırdaki "ANAHTAR=SKOR,SÜRE" bilgisini recordsMap (HashMap) içine kaydeder.
    public final void loadRecordsFromFile() {
        recordsMap.clear();
        File file = new File(RECORDS_FILE_NAME);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Boş satırları ve '#' ile başlayan açıklama satırlarını atlar
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    String key = line.substring(0, eqIndex).trim();
                    String value = line.substring(eqIndex + 1).trim();
                    recordsMap.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading records.txt: " + e.getMessage());
        }
    }

    // Bellekteki recordsMap (HashMap) içindeki tüm rekorları BufferedWriter kullanarak "records.txt" dosyasına kalıcı olarak yazar.
    public void saveRecordsToFile() {
        File file = new File(RECORDS_FILE_NAME);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            writer.write("# MatchyMatch High Scores & Fastest Times (Format: KEY=HIGH_SCORE,FASTEST_SECONDS)");
            writer.newLine();
            for (Map.Entry<String, String> entry : recordsMap.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving records.txt: " + e.getMessage());
        }
    }

    // Verilen ayar anahtarı (configKey) için kaydedilmiş En Yüksek Skoru (Highest Score) döndürür; kayıt yoksa 0 döner.
    public int getHighScore(String configKey) {
        String val = recordsMap.get(configKey);
        if (val == null || !val.contains(",")) {
            return 0;
        }
        try {
            String[] parts = val.split(",");
            return Integer.parseInt(parts[0].trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Verilen ayar anahtarı (configKey) için kaydedilmiş En Hızlı Süreyi (Fastest Time) saniye cinsinden döndürür; kayıt yoksa -1.0 döner.
    public double getFastestTime(String configKey) {
        String val = recordsMap.get(configKey);
        if (val == null || !val.contains(",")) {
            return -1.0;
        }
        try {
            String[] parts = val.split(",");
            return Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    // Oyun sonunda yeni bir yüksek skor veya (10 turun tamamı bitirildiyse) daha hızlı bir süre elde edildiğinde HashMap ve dosyayı günceller.
    public void updateRecord(String configKey, int newScore, double newTimeSeconds, boolean completedAllRounds) {
        int bestScore = getHighScore(configKey);
        double bestTime = getFastestTime(configKey);

        if (newScore > bestScore) {
            bestScore = newScore;
        }
        if (completedAllRounds && newTimeSeconds > 0 && (bestTime < 0 || newTimeSeconds < bestTime)) {
            bestTime = newTimeSeconds;
        }

        String formattedTime = "-1.0";
        if (bestTime > 0) {
            formattedTime = String.format(Locale.US, "%.1f", bestTime);
        }
        recordsMap.put(configKey, bestScore + "," + formattedTime);
        saveRecordsToFile();
    }

    // 6 ArrayList içindeki toplam kelime ve ifade sayısını döndürür.
    public int getTotalWordCount() {
        return easyWords.size() + mediumWords.size() + hardWords.size()
                + easyPhrases.size() + mediumPhrases.size() + hardPhrases.size();
    }

    // Başlangıç doğrulama testi: 6 dil, 2 mod, 3 zorluk ve 2 Arapça kuralı (toplam 360 kombinasyon) için
    // 10 turluk oturumları simüle ederek boş çeviri veya aynı turda kopya kelime olmadığını doğrular.
    public static boolean runDiagnostics(DataManager dm) {
        String[] languages = {"English", "Turkish", "French", "Italian", "Hungarian", "Arabic"};
        String[] modes = {"Word", "Phrase"};
        String[] difficulties = {"Easy", "Medium", "Hard"};
        boolean[] arabicLatinRules = {false, true};

        int verifiedPermutations = 0;

        for (int a = 0; a < languages.length; a++) {
            String appLang = languages[a];
            for (int t = 0; t < languages.length; t++) {
                String targetLang = languages[t];
                if (appLang.equals(targetLang)) {
                    continue;
                }
                for (int m = 0; m < modes.length; m++) {
                    String mode = modes[m];
                    for (int d = 0; d < difficulties.length; d++) {
                        String diff = difficulties[d];
                        for (int r = 0; r < arabicLatinRules.length; r++) {
                            boolean useArabicLatin = arabicLatinRules[r];
                            ArrayList<WordEntry> pool = dm.getEntriesForSession(mode, diff, 50);
                            if (pool.size() < 50) {
                                throw new IllegalStateException("Insufficient pool for " + mode + " " + diff);
                            }

                            // 10 turun her birindeki 5 kelime çiftini tek tek kontrol eder
                            for (int round = 1; round <= 10; round++) {
                                ArrayList<Integer> roundIds = new ArrayList<>();
                                int startIdx = (round - 1) * 5;
                                for (int i = 0; i < 5; i++) {
                                    WordEntry entry = pool.get(startIdx + i);
                                    if (roundIds.contains(entry.getId())) {
                                        throw new IllegalStateException("Duplicate word ID in round " + round);
                                    }
                                    roundIds.add(entry.getId());

                                    String leftText = entry.getTextForLanguage(appLang, useArabicLatin);
                                    String rightText = entry.getTextForLanguage(targetLang, useArabicLatin);

                                    if (leftText == null || leftText.isEmpty() || rightText == null || rightText.isEmpty()) {
                                        throw new IllegalStateException("Empty translation for entry ID " + entry.getId());
                                    }
                                    if (leftText.equalsIgnoreCase(rightText)) {
                                        throw new IllegalStateException("Identical translation [" + leftText
                                                + "] between " + appLang + " and " + targetLang
                                                + " at ID " + entry.getId());
                                    }
                                }
                            }
                            verifiedPermutations++;
                        }
                    }
                }
            }
        }

        System.out.println("[MatchyMatch Diagnostics] Loaded " + dm.getTotalWordCount()
                + " entries (EasyWords=" + dm.getEasyWords().size()
                + ", MediumWords=" + dm.getMediumWords().size()
                + ", HardWords=" + dm.getHardWords().size()
                + ", EasyPhrases=" + dm.getEasyPhrases().size()
                + ", MediumPhrases=" + dm.getMediumPhrases().size()
                + ", HardPhrases=" + dm.getHardPhrases().size()
                + "). Verified " + verifiedPermutations
                + " language/mode/difficulty session permutations: 100% OK.");
        return true;
    }

    // "words.csv" dosyası için 6 dilde (EN, TR, FR, IT, HU, AR_SCRIPT, AR_LATIN) özenle hazırlanmış 180 satırlık başlangıç veri seti.
    private String[] getStarterDataset() {
        return new String[]{
            // ===================== EASY WORDS (35 Kelime) =====================
            "1,EASY,WORD,Hello,Merhaba,Salut,Ciao,Szia,مرحبا,Marhaba",
            "2,EASY,WORD,Water,Su,Eau,Acqua,Víz,ماء,Maa",
            "3,EASY,WORD,Bread,Ekmek,Pain,Pane,Kenyér,خبز,Khubz",
            "4,EASY,WORD,Book,Kitap,Livre,Libro,Könyv,كتاب,Kitab",
            "5,EASY,WORD,House,Ev,Maison,Casa,Ház,بيت,Bayt",
            "6,EASY,WORD,Sun,Güneş,Soleil,Sole,Nap,شمس,Shams",
            "7,EASY,WORD,Moon,Ay,Lune,Luna,Hold,قمر,Qamar",
            "8,EASY,WORD,Tree,Ağaç,Arbre,Albero,Fa,شجرة,Shajara",
            "9,EASY,WORD,Cat,Kedi,Chat,Gatto,Macska,قطة,Qitta",
            "10,EASY,WORD,Dog,Köpek,Chien,Cane,Kutya,كلب,Kalb",
            "11,EASY,WORD,Friend,Arkadaş,Ami,Amico,Barát,صديق,Sadiq",
            "12,EASY,WORD,School,Okul,École,Scuola,Iskola,مدرسة,Madrasa",
            "13,EASY,WORD,Apple,Elma,Pomme,Mela,Alma,تفاحة,Tuffaha",
            "14,EASY,WORD,Milk,Süt,Lait,Latte,Tej,حليب,Halib",
            "15,EASY,WORD,Car,Araba,Voiture,Auto,Autó,سيارة,Sayyara",
            "16,EASY,WORD,Door,Kapı,Porte,Porta,Ajtó,باب,Bab",
            "17,EASY,WORD,Window,Pencere,Fenêtre,Finestra,Ablak,نافذة,Nafidha",
            "18,EASY,WORD,Chair,Sandalye,Chaise,Sedia,Szék,كرسي,Kursi",
            "19,EASY,WORD,Desk,Çalışma masası,Bureau,Scrivania,Íróasztal,مكتب,Maktab",
            "20,EASY,WORD,Mother,Anne,Mère,Madre,Anya,أم,Umm",
            "21,EASY,WORD,Father,Baba,Père,Padre,Apa,أب,Ab",
            "22,EASY,WORD,Night,Gece,Nuit,Notte,Éjszaka,ليل,Layl",
            "23,EASY,WORD,Morning,Sabah,Matin,Mattina,Reggel,صباح,Subh",
            "24,EASY,WORD,City,Şehir,Ville,Città,Város,مدينة,Madina",
            "25,EASY,WORD,Sea,Deniz,Mer,Mare,Tenger,بحر,Bahr",
            "26,EASY,WORD,Star,Yıldız,Étoile,Stella,Csillag,نجمة,Najma",
            "27,EASY,WORD,Rain,Yağmur,Pluie,Pioggia,Eső,مطر,Matar",
            "28,EASY,WORD,Snow,Kar,Neige,Neve,Hó,ثلج,Thalj",
            "29,EASY,WORD,Bird,Kuş,Oiseau,Uccello,Madár,طائر,Tair",
            "30,EASY,WORD,Fish,Balık,Poisson,Pesce,Hal,سمك,Samak",
            "31,EASY,WORD,Flower,Çiçek,Fleur,Fiore,Virág,زهرة,Zahra",
            "32,EASY,WORD,Child,Çocuk,Enfant,Bambino,Gyermek,طفل,Tifl",
            "33,EASY,WORD,Hand,El,Main,Mano,Kéz,يد,Yad",
            "34,EASY,WORD,Eye,Göz,Œil,Occhio,Szem,عين,Ayn",
            "35,EASY,WORD,Heart,Kalp,Cœur,Cuore,Szív,قلب,Qalb",

            // ===================== EASY PHRASES (25 İfade) =====================
            "36,EASY,PHRASE,Thank you,Teşekkür ederim,Merci,Grazie,Köszönöm,شكرا لك,Shukran lak",
            "37,EASY,PHRASE,Good morning,Günaydın,Bonjour,Buongiorno,Jó reggelt,صباح الخير,Sabah al-khayr",
            "38,EASY,PHRASE,Good night,İyi geceler,Bonne nuit,Buonanotte,Jó éjszakát,تصبح على خير,Tusbih ala khayr",
            "39,EASY,PHRASE,How are you?,Nasılsın?,Comment ça va?,Come stai?,Hogy vagy?,كيف حالك؟,Kayfa haluk?",
            "40,EASY,PHRASE,See you later,Sonra görüşürüz,À plus tard,A dopo,Viszlát később,أراك لاحقا,Araka lahiqan",
            "41,EASY,PHRASE,My name is...,Benim adım...,Je m'appelle...,Mi chiamo...,A nevem...,اسمي...,Ismi...",
            "42,EASY,PHRASE,Good evening,İyi akşamlar,Bonsoir,Buonasera,Jó estét,مساء الخير,Masaa al-khayr",
            "43,EASY,PHRASE,Welcome,Hoş geldiniz,Bienvenue,Benvenuto,Üdvözlöm,أهلا وسهلا,Ahlan wa sahlan",
            "44,EASY,PHRASE,Please,Lütfen,S'il vous plaît,Per favore,Kérem,من فضلك,Min fadlik",
            "45,EASY,PHRASE,Excuse me,Affedersiniz,Excusez-moi,Mi scusi,Elnézést,عفوا,Afwan",
            "46,EASY,PHRASE,I am sorry,Özür dilerim,Je suis désolé,Mi dispiace,Sajnálom,أنا آسف,Ana asif",
            "47,EASY,PHRASE,Yes of course,Evet elbette,Oui bien sûr,Sì certo,Igen természetesen,نعم بالطبع,Naam biltab",
            "48,EASY,PHRASE,No thank you,Hayır teşekkürler,Non merci,No grazie,Nem köszönöm,لا شكرا,La shukran",
            "49,EASY,PHRASE,I am fine,İyiyim,Je vais bien,Sto bene,Jól vagyok,أنا بخير,Ana bikhayr",
            "50,EASY,PHRASE,What is this?,Bu nedir?,Qu'est-ce que c'est?,Che cos'è questo?,Mi ez?,ما هذا؟,Ma hadha?",
            "51,EASY,PHRASE,Where are you?,Neredesin?,Où es-tu?,Dove sei?,Hol vagy?,أين أنت؟,Ayna anta?",
            "52,EASY,PHRASE,Have a nice day,İyi günler,Bonne journée,Buona giornata,Szép napot,يوم سعيد,Yawm said",
            "53,EASY,PHRASE,Good luck,İyi şanslar,Bonne chance,Buona fortuna,Sok szerencsét,حظا سعيدا,Hazzan saidan",
            "54,EASY,PHRASE,Happy birthday,Doğum günün kutlu olsun,Joyeux anniversaire,Buon compleanno,Boldog születésnapot,عيد ميلاد سعيد,Eid milad said",
            "55,EASY,PHRASE,Congratulations,Tebrikler,Félicitations,Congratulazioni,Gratulálok,مبروك,Mabruk",
            "56,EASY,PHRASE,I love you,Seni seviyorum,Je t'aime,Ti amo,Szeretlek,أحبك,Uhibbuk",
            "57,EASY,PHRASE,Let us go,Hadi gidelim,Allons-y,Andiamo,Menjünk,هيا بنا,Hayya bina",
            "58,EASY,PHRASE,Wait a minute,Bir dakika bekle,Attends une minute,Aspetta un minuto,Várj egy percet,انتظر دقيقة,Intazir daqiqa",
            "59,EASY,PHRASE,Come here,Buraya gel,Viens ici,Vieni qui,Gyere ide,تعال إلى هنا,Taal ila huna",
            "60,EASY,PHRASE,See you tomorrow,Yarın görüşürüz,À demain,A domani,Holnap találkozunk,أراك غدا,Araka ghadan",

            // ===================== MEDIUM WORDS (35 Kelime) =====================
            "61,MEDIUM,WORD,Journey,Yolculuk,Voyage,Viaggio,Utazás,رحلة,Rihla",
            "62,MEDIUM,WORD,Knowledge,Bilgi,Connaissance,Conoscenza,Tudás,معرفة,Marifa",
            "63,MEDIUM,WORD,Freedom,Özgürlük,Liberté,Libertà,Szabadság,حرية,Hurriyya",
            "64,MEDIUM,WORD,Bridge,Köprü,Pont,Ponte,Híd,جسر,Jisr",
            "65,MEDIUM,WORD,Mountain,Dağ,Montagne,Montagna,Hegy,جبل,Jabal",
            "66,MEDIUM,WORD,River,Nehir,Rivière,Fiume,Folyó,نهر,Nahr",
            "67,MEDIUM,WORD,Weather,Hava durumu,Météo,Meteo,Időjárás,طقس,Taqs",
            "68,MEDIUM,WORD,Library,Kütüphane,Bibliothèque,Biblioteca,Könyvtár,مكتبة,Maktaba",
            "69,MEDIUM,WORD,Hospital,Hastane,Hôpital,Ospedale,Kórház,مستشفى,Mustashfa",
            "70,MEDIUM,WORD,Airport,Havalimanı,Aéroport,Aeroporto,Repülőtér,مطار,Al-Matar",
            "71,MEDIUM,WORD,Success,Başarı,Succès,Successo,Siker,نجاح,Najah",
            "72,MEDIUM,WORD,Bravery,Cesaret,Bravoure,Coraggio,Bátorság,شجاعة,Shajaa",
            "73,MEDIUM,WORD,Promise,Söz,Promesse,Promessa,Ígéret,وعد,Waad",
            "74,MEDIUM,WORD,Memory,Hafıza,Mémoire,Memoria,Emlék,ذاكرة,Dhakira",
            "75,MEDIUM,WORD,Health,Sağlık,Santé,Salute,Egészség,صحة,Sihha",
            "76,MEDIUM,WORD,Future,Gelecek,Avenir,Futuro,Jövő,مستقبل,Mustaqbal",
            "77,MEDIUM,WORD,Valley,Vadi,Vallée,Valle,Völgy,وادي,Wadi",
            "78,MEDIUM,WORD,Research,Araştırma,Recherche,Ricerca,Kutatás,بحث,Bahth",
            "79,MEDIUM,WORD,Language,Dil,Langue,Lingua,Nyelv,لغة,Lugha",
            "80,MEDIUM,WORD,History,Tarih,Histoire,Storia,Történelem,تاريخ,Tarikh",
            "81,MEDIUM,WORD,Market,Pazar,Marché,Mercato,Piac,سوق,Suq",
            "82,MEDIUM,WORD,Ticket,Bilet,Billet,Biglietto,Jegy,تذكرة,Tadhkira",
            "83,MEDIUM,WORD,Key,Anahtar,Clé,Chiave,Kulcs,مفتاح,Miftah",
            "84,MEDIUM,WORD,Mirror,Ayna,Miroir,Specchio,Tükör,مرآة,Mirat",
            "85,MEDIUM,WORD,Island,Ada,Île,Isola,Sziget,جزيرة,Jazira",
            "86,MEDIUM,WORD,Forest,Orman,Forêt,Foresta,Erdő,غابة,Ghaba",
            "87,MEDIUM,WORD,Desert,Çöl,Désert,Deserto,Sivatag,صحراء,Sahra",
            "88,MEDIUM,WORD,Painting,Tablo,Peinture,Pittura,Festmény,لوحة,Lawha",
            "89,MEDIUM,WORD,Music,Müzik,Musique,Musica,Zene,موسيقى,Musiqa",
            "90,MEDIUM,WORD,Garden,Bahçe,Jardin,Giardino,Kert,حديقة,Hadiqa",
            "91,MEDIUM,WORD,Kitchen,Mutfak,Cuisine,Cucina,Konyha,مطبخ,Matbakh",
            "92,MEDIUM,WORD,Suitcase,Bavul,Valise,Valigia,Bőrönd,حقيبة سفر,Haqibat safar",
            "93,MEDIUM,WORD,Passport,Pasaport,Passeport,Passaporto,Útlevél,جواز سفر,Jawaz safar",
            "94,MEDIUM,WORD,Neighbor,Komşu,Voisin,Vicino,Szomszéd,جار,Jar",
            "95,MEDIUM,WORD,Gift,Hediye,Cadeau,Regalo,Ajándék,هدية,Hadiyya",

            // ===================== MEDIUM PHRASES (25 İfade) =====================
            "96,MEDIUM,PHRASE,Where is the station?,İstasyon nerede?,Où est la gare?,Dov'è la stazione?,Hol van az állomás?,أين المحطة؟,Ayna al-mahatta?",
            "97,MEDIUM,PHRASE,How much does this cost?,Bunun fiyatı ne kadar?,Combien ça coûte?,Quanto costa questo?,Mennyibe kerül ez?,كم سعر هذا؟,Kam sir hadha?",
            "98,MEDIUM,PHRASE,I would like coffee,Kahve istiyorum,Je voudrais un café,Vorrei un caffè,Kávét szeretnék,أريد قهوة,Uridu qahwa",
            "99,MEDIUM,PHRASE,Can you help me?,Bana yardım edebilir misin?,Pouvez-vous m'aider?,Puoi aiutarmi?,Tudnál segíteni?,هل يمكنك مساعدتي؟,Hal yumkinuka musaadati?",
            "100,MEDIUM,PHRASE,Nice to meet you,Tanıştığımıza memnun oldum,Enchanté de vous connaître,Piacere di conoscerti,Örülök hogy találkoztunk,تشرفنا بمعرفتك,Tasharrafna bimarifatik",
            "101,MEDIUM,PHRASE,Have a safe trip,İyi yolculuklar,Bon voyage,Buon viaggio,Jó utat,رحلة سعيدة,Rihla saida",
            "102,MEDIUM,PHRASE,Do you speak English?,İngilizce konuşuyor musun?,Parlez-vous anglais?,Parli inglese?,Beszélsz angolul?,هل تتحدث الإنجليزية؟,Hal tatahadath al-injiliziyya?",
            "103,MEDIUM,PHRASE,I do not understand,Anlamıyorum,Je ne comprends pas,Non capisco,Nem értem,لا أفهم,La afham",
            "104,MEDIUM,PHRASE,Could you repeat that?,Tekrar eder misiniz?,Pouvez-vous répéter?,Puoi ripetere?,Megismételnéd?,هل يمكنك الإعادة؟,Hal yumkinuka al-iada?",
            "105,MEDIUM,PHRASE,What time is it?,Saat kaç?,Quelle heure est-il?,Che ore sono?,Hány óra van?,كم الساعة؟,Kam al-saa?",
            "106,MEDIUM,PHRASE,I am looking for a hotel,Bir otel arıyorum,Je cherche un hôtel,Cerco un albergo,Szállodát keresek,أبحث عن فندق,Abhathu an funduq",
            "107,MEDIUM,PHRASE,The bill please,Hesap lütfen,L'addition s'il vous plaît,Il conto per favore,A számlát kérem,الفاتورة من فضلك,Al-fatura min fadlik",
            "108,MEDIUM,PHRASE,Where is the airport?,Havalimanı nerede?,Où est l'aéroport?,Dov'è l'aeroporto?,Hol van a repülőtér?,أين المطار؟,Ayna al-matar?",
            "109,MEDIUM,PHRASE,I need a doctor,Doktora ihtiyacım var,J'ai besoin d'un médecin,Ho bisogno di un medico,Orvosra van szükségem,أحتاج إلى طبيب,Ahtaju ila tabib",
            "110,MEDIUM,PHRASE,It is very delicious,Çok lezzetli,C'est très délicieux,È molto delizioso,Nagyon finom,إنه لذيذ جدا,Innahu ladhidh jiddan",
            "111,MEDIUM,PHRASE,What is the weather like?,Hava nasıl?,Quel temps fait-il?,Com'è il tempo?,Milyen az idő?,كيف حال الطقس؟,Kayfa hal al-taqs?",
            "112,MEDIUM,PHRASE,I lost my keys,Anahtarlarımı kaybettim,J'ai perdu mes clés,Ho perso le chiavi,Elvesztettem a kulcsomat,أضعت مفاتيحي,Adatu mafatihi",
            "113,MEDIUM,PHRASE,Call me tonight,Bu akşam beni ara,Appelle-moi ce soir,Chiamami stasera,Hívj fel ma este,اتصل بي الليلة,Ittasil bi al-layla",
            "114,MEDIUM,PHRASE,Turn left here,Buradan sola dön,Tournez à gauche ici,Gira a sinistra qui,Fordulj balra itt,انعطف يسارا هنا,Inatif yasaran huna",
            "115,MEDIUM,PHRASE,Go straight ahead,Düz devam et,Allez tout droit,Vai sempre dritto,Menj egyenesen tovább,اذهب مباشرة,Idhhab mubasharatan",
            "116,MEDIUM,PHRASE,I agree with you,Sana katılıyorum,Je suis d'accord avec toi,Sono d'accordo con te,Egyetértek veled,أتفق معك,Attafiqu maak",
            "117,MEDIUM,PHRASE,Take care of yourself,Kendine iyi bak,Prends soin de toi,Prenditi cura di te,Vigyázz magadra,اعتن بنفسك,Itani binafsik",
            "118,MEDIUM,PHRASE,May I ask a question?,Bir soru sorabilir miyim?,Puis-je poser une question?,Posso fare una domanda?,Feltehetek egy kérdést?,هل لي بسؤال؟,Hal li bisual?",
            "119,MEDIUM,PHRASE,I am running late,Geç kalıyorum,Je suis en retard,Sono in ritardo,Késésben vagyok,أنا متأخر,Ana mutaakhir",
            "120,MEDIUM,PHRASE,Enjoy your meal,Afiyet olsun,Bon appétit,Buon appetito,Jó étvágyat,شهية طيبة,Shahiyya tayyiba",

            // ===================== HARD WORDS (35 Kelime) =====================
            "121,HARD,WORD,Perseverance,Azim,Persévérance,Perseveranza,Kitartás,مثابرة,Muthabara",
            "122,HARD,WORD,Consciousness,Bilinç,Conscience,Coscienza,Tudatosság,وعي,Wayi",
            "123,HARD,WORD,Responsibility,Sorumluluk,Responsabilité,Responsabilità,Felelősség,مسؤولية,Masuliyya",
            "124,HARD,WORD,Creativity,Yaratıcılık,Créativité,Creatività,Kreativitás,إبداع,Ibdaa",
            "125,HARD,WORD,Opportunity,Fırsat,Opportunité,Opportunità,Lehetőség,فرصة,Fursa",
            "126,HARD,WORD,Environment,Çevre,Environnement,Ambiente,Környezet,بيئة,Bia",
            "127,HARD,WORD,Achievement,Başarım,Accomplissement,Realizzazione,Teljesítmény,إنجاز,Injaz",
            "128,HARD,WORD,Consequence,Sonuç,Conséquence,Conseguenza,Következmény,نتيجة,Natija",
            "129,HARD,WORD,Curiosity,Merak,Curiosité,Curiosità,Kíváncsiság,فضول,Fudul",
            "130,HARD,WORD,Generosity,Cömertlik,Générosité,Generosità,Nagylelkűség,كرم,Karam",
            "131,HARD,WORD,Wisdom,Bilgelik,Sagesse,Saggezza,Bölcsesség,حكمة,Hikma",
            "132,HARD,WORD,Forgiveness,Affetme,Pardon,Perdono,Megbocsátás,تسامح,Tasamuh",
            "133,HARD,WORD,Fairness,Adalet,Équité,Giustizia,Igazságosság,عدالة,Adala",
            "134,HARD,WORD,Harmony,Uyum,Harmonie,Armonia,Harmónia,انسجام,Insijam",
            "135,HARD,WORD,Thankfulness,Minnettarlık,Reconnaissance,Gratitudine,Hála,امتنان,Imtinan",
            "136,HARD,WORD,Independence,Bağımsızlık,Indépendance,Indipendenza,Függetlenség,استقلال,Istiqlal",
            "137,HARD,WORD,Philosophy,Felsefe,Philosophie,Filosofia,Filozófia,فلسفة,Falsafa",
            "138,HARD,WORD,Engineering,Mühendislik,Ingénierie,Ingegneria,Mérnöki tudomány,هندسة,Handasa",
            "139,HARD,WORD,Kindness,Şefkat,Bienveillance,Gentilezza,Kedvesség,تعاطف,Taatuf",
            "140,HARD,WORD,Determination,Kararlılık,Détermination,Determinazione,Elszántság,عزيمة,Azima",
            "141,HARD,WORD,Discovery,Keşif,Découverte,Scoperta,Felfedezés,اكتشاف,Iktishaf",
            "142,HARD,WORD,Enlightenment,Aydınlanma,Illumination,Illuminazione,Megvilágosodás,تنوير,Tanwir",
            "143,HARD,WORD,Negotiation,Müzakere,Négociation,Negoziazione,Tárgyalás,تفاوض,Tafawud",
            "144,HARD,WORD,Prosperity,Refah,Prospérité,Prosperità,Jólét,ازدهار,Izdihar",
            "145,HARD,WORD,Authenticity,Özgünlük,Authenticité,Autenticità,Hitelesség,أصالة,Asala",
            "146,HARD,WORD,Resilience,Dayanıklılık,Résilience,Resilienza,Rugalmasság,مرونة,Muruna",
            "147,HARD,WORD,Eloquence,Belagat,Éloquence,Eloquenza,Ékesszólás,فصاحة,Fasaha",
            "148,HARD,WORD,Altruism,Fedakarlık,Altruisme,Altruismo,Önzetlenség,إيثار,Ithar",
            "149,HARD,WORD,Serendipity,Mutlu tesadüf,Sérendipité,Serendipità,Szerencsés véletlen,صدفة سعيدة,Sudfa saida",
            "150,HARD,WORD,Meditation,Tefekkür,Méditation,Meditazione,Szemlélődés,تأمل,Taammul",
            "151,HARD,WORD,Foresight,Öngörü,Clairvoyance,Lungimiranza,Előrelátás,بصيرة,Basira",
            "152,HARD,WORD,Integrity,Dürüstlük,Intégrité,Integrità,Feddhetetlenség,نزاهة,Nazaha",
            "153,HARD,WORD,Empathy,Empati,Empathie,Empatia,Empátia,تقمص عاطفي,Taqammus atifi",
            "154,HARD,WORD,Equilibrium,Denge,Équilibre,Equilibrio,Egyensúly,توازن,Tawazun",
            "155,HARD,WORD,Sovereignty,Egemenlik,Souveraineté,Sovranità,Szuverenitás,سيادة,Siyada",

            // ===================== HARD PHRASES (25 İfade) =====================
            "156,HARD,PHRASE,Actions speak louder,Eylemler sözlerden güçlüdür,Les actes valent mieux,I fatti contano più,A tettek hangosabbak,الأفعال أبلغ من الأقوال,Al-afaal ablagh min al-aqwal",
            "157,HARD,PHRASE,Knowledge is power,Bilgi güçtür,Le savoir est le pouvoir,La conoscenza è potere,A tudás hatalom,المعرفة قوة,Al-marifa quwwa",
            "158,HARD,PHRASE,Time heals all wounds,Zaman her şeyin ilacıdır,Le temps guérit tout,Il tempo guarisce tutto,Az idő minden sebet begyógyít,الوقت يشفي الجراح,Al-waqt yashfi al-jirah",
            "159,HARD,PHRASE,Better late than never,Geç olsun güç olmasın,Mieux vaut tard que jamais,Meglio tardi che mai,Jobb későn mint soha,أن تصل متأخرا خير,An tasila mutaakhiran khayr",
            "160,HARD,PHRASE,Practice makes perfect,Pratik mükemmelleştirir,C'est en forgeant,La pratica rende perfetti,Gyakorlat teszi a mestert,الممارسة تصنع الإتقان,Al-mumarasa tasnau al-itqan",
            "161,HARD,PHRASE,Every cloud has a silver lining,Her şerde bir hayır vardır,Après la pluie le beau temps,Ogni male non vien per nuocere,Minden rosszban van valami jó,رب ضارة نافعة,Rubba darratin nafia",
            "162,HARD,PHRASE,Fortune favors the bold,Şans cesurlardan yanadır,La fortune sourit aux audacieux,La fortuna aiuta gli audaci,Bátraké a szerencse,الحظ يحالف الشجعان,Al-hazz yuhalif al-shujan",
            "163,HARD,PHRASE,Honesty is the best policy,Dürüstlük en iyi politikadır,L'honnêteté est la meilleure politique,L'onestà è la miglior politica,Az őszinteség a legjobb út,الصدق منجاة,Al-sidq manjat",
            "164,HARD,PHRASE,Where there is a will,Azmin elinden bir şey kurtulmaz,Vouloir c'est pouvoir,Volere è potere,Ahol akarat van ott út is van,حيثما توجد الإرادة,Haythuma tujad al-irada",
            "165,HARD,PHRASE,Don't judge a book by its cover,Dış görünüşe aldanma,L'habit ne fait pas le moine,L'abito non fa il monaco,Ne ítélj külső alapján,لا تحكم بالمظاهر,La tahkum bil-mazahir",
            "166,HARD,PHRASE,A blessing in disguise,Görünmez bir nimet,Un mal pour un bien,Una fortuna nella sfortuna,Áldás álruhában,نعمة في طيات محنة,Nima fi tayyat mihna",
            "167,HARD,PHRASE,Once in a blue moon,Kırk yılda bir,Tous les trente-six du mois,Ogni morte di papa,Nagyon ritkán,في النادر جدا,Fi al-nadir jiddan",
            "168,HARD,PHRASE,Break the ice,Buzları eritmek,Briser la glace,Rompere il ghiaccio,Megtörni a jeget,كسر الحاجز,Kasr al-hajiz",
            "169,HARD,PHRASE,Patience is bitter but its fruit is sweet,Sabır acıdır ama meyvesi tatlıdır,La patience est amère,La pazienza è amara,A türelem rózsát terem,الصبر مفتاح الفرج,Al-sabr miftah al-faraj",
            "170,HARD,PHRASE,Look before you leap,Boğaz dokuz boğumdur,Réfléchir avant d'agir,Guarda prima di saltare,Előbb gondolkodj aztán cselekedj,فكر قبل أن تقدم,Fakkir qabla an tuqdim",
            "171,HARD,PHRASE,Silence is golden,Sükut altındır,Le silence est d'or,Il silenzio è d'oro,Hallgatni arany,الصمت ذهب,Al-samt dhahab",
            "172,HARD,PHRASE,Strike while the iron is hot,Demir tavında dövülür,Battre le fer tant qu'il est chaud,Batti il ferro finché è caldo,Addig üsd a vasat amíg meleg,اطرق الحديد وهو ساخن,Itriq al-hadid wa huwa sakhin",
            "173,HARD,PHRASE,No pain no gain,Emek olmadan yemek olmaz,On n'a rien sans rien,Chi non risica non rosica,Munka nélkül nincs eredmény,لا حلاوة بدون تعب,La halawa bidun taab",
            "174,HARD,PHRASE,All that glitters is not gold,Her parlayan altın değildir,Tout ce qui brille n'est pas or,Non è tutto oro quel che luccica,Nem mind arany ami fénylik,ليس كل ما يلمع ذهبا,Laysa kull ma yalma dhahaban",
            "175,HARD,PHRASE,Rome was not built in a day,Roma bir günde kurulmadı,Rome ne s'est pas faite en un jour,Roma non fu costruita in un giorno,Róma sem egy nap alatt épült,لم تبن روما في يوم,Lam tubna Ruma fi yawm",
            "176,HARD,PHRASE,Unity is strength,Birlikten kuvvet doğar,L'union fait la force,L'unione fa la forza,Egységben az erő,الاتحاد قوة,Al-ittihad quwwa",
            "177,HARD,PHRASE,Necessity is the mother of invention,İhtiyaç icadın anasıdır,La nécessité est mère d'invention,La necessità aguzza l'ingegno,A szükség nagy úr,الحاجة أم الاختراع,Al-haja umm al-ikhtira",
            "178,HARD,PHRASE,Experience is the best teacher,Tecrübe en iyi öğretmendir,L'expérience est le meilleur maître,L'esperienza è la miglior maestra,A tapasztalat a legjobb tanár,التجربة خير برهان,Al-tajriba khayr burhan",
            "179,HARD,PHRASE,A friend in need is a friend indeed,Dost kara günde belli olur,C'est dans le besoin qu'on reconnaît ses amis,Chi trova un amico trova un tesoro,Bajban ismerszik meg a barát,الصديق وقت الضيق,Al-sadiq waqt al-diq",
            "180,HARD,PHRASE,Still waters run deep,Durgun sular derin olur,Il n'est pire eau que l'eau qui dort,L'acqua cheta rovina i ponti,Lassú víz partot mos,تحت السواهي دواهي,Tahta al-sawahi dawahi"
        };
    }
}
