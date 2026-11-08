package com.matchymatch.service;

import com.matchymatch.domain.GameSession;
import com.matchymatch.domain.InvalidLanguagePairException;
import com.matchymatch.domain.SessionRecord;
import com.matchymatch.domain.VocabularyItem;
import com.matchymatch.persistence.DataManager;

import java.util.List;
import java.util.Locale;

// GameController sınıfı: MVC (Model-View-Controller) mimarisindeki kontrolcü katmanıdır.
// Arayüz sınıfları (MenuPanel, GamePanel, ConsoleUI) ile veri/model sınıfları (DataManager, GameSession, VocabularyItem)
// arasındaki iş mantığını, dil doğrulamasını (InvalidLanguagePairException) ve Strategy desenini yönetir.
public class GameController {

    private final DataManager dataManager;
    private LanguageDisplayStrategy displayStrategy;
    private GameSession activeSession;

    // Kurucu metot (Constructor): Varsayılan olarak ArabicScriptDisplayStrategy stratejisi ile başlar.
    public GameController(DataManager dataManager) {
        if (dataManager == null) {
            throw new IllegalArgumentException("DataManager cannot be null.");
        }
        this.dataManager = dataManager;
        this.displayStrategy = new ArabicScriptDisplayStrategy();
    }

    // Seçilen iki dilin geçerli ve birbirinden farklı olup olmadığını kontrol eder.
    // Geçersiz durumda özel kontrol edilen istisna olan InvalidLanguagePairException fırlatır.
    public void validateLanguagePair(String appLanguage, String targetLanguage) throws InvalidLanguagePairException {
        if (appLanguage == null || appLanguage.trim().isEmpty()
                || targetLanguage == null || targetLanguage.trim().isEmpty()) {
            throw new InvalidLanguagePairException("Application language and target language must not be null or empty.");
        }

        if (!VocabularyItem.isSupportedLanguage(appLanguage)) {
            throw new InvalidLanguagePairException("Unsupported application language: " + appLanguage);
        }
        if (!VocabularyItem.isSupportedLanguage(targetLanguage)) {
            throw new InvalidLanguagePairException("Unsupported target language: " + targetLanguage);
        }

        String appCode = VocabularyItem.getLanguageCode(appLanguage);
        String targetCode = VocabularyItem.getLanguageCode(targetLanguage);
        if (appCode.equalsIgnoreCase(targetCode)) {
            throw new InvalidLanguagePairException(
                    "Application language (" + appLanguage + ") and Target language (" + targetLanguage + ") cannot be identical."
            );
        }
    }

    // Strategy Tasarım Deseni: Seçilen dillere ve kullanıcının "Arabic Alphabet Rule" tercihine göre
    // çalışma zamanında (runtime) uygun LanguageDisplayStrategy nesnesini seçer.
    public void configureDisplayStrategy(String appLanguage, String targetLanguage, boolean preferArabicLatin) {
        boolean arabicInvolved = false;
        if (appLanguage != null && "AR".equals(VocabularyItem.getLanguageCode(appLanguage))) {
            arabicInvolved = true;
        }
        if (targetLanguage != null && "AR".equals(VocabularyItem.getLanguageCode(targetLanguage))) {
            arabicInvolved = true;
        }

        if (arabicInvolved && preferArabicLatin) {
            this.displayStrategy = new ArabicLatinDisplayStrategy();
        } else {
            this.displayStrategy = new ArabicScriptDisplayStrategy();
        }
    }

    // Dışarıdan doğrudan bir LanguageDisplayStrategy nesnesi atanmasını sağlar.
    public void setDisplayStrategy(LanguageDisplayStrategy displayStrategy) {
        if (displayStrategy != null) {
            this.displayStrategy = displayStrategy;
        }
    }

    public LanguageDisplayStrategy getDisplayStrategy() {
        return displayStrategy;
    }

    // Aktif stratejiyi kullanarak bir VocabularyItem nesnesinin istenen dildeki metnini döndürür.
    public String resolveDisplayText(VocabularyItem item, String language) {
        return displayStrategy.resolveText(item, language);
    }

    // Dil çiftini doğrular, stratejiyi ayarlar ve yeni bir 10 turluk GameSession nesnesi oluşturup başlatır.
    public GameSession startNewSession(String appLanguage, String targetLanguage,
                                       String mode, String difficulty,
                                       boolean preferArabicLatin) throws InvalidLanguagePairException {
        validateLanguagePair(appLanguage, targetLanguage);
        configureDisplayStrategy(appLanguage, targetLanguage, preferArabicLatin);

        String configKey = DataManager.buildConfigKey(appLanguage, targetLanguage, mode, difficulty);
        List<VocabularyItem> pool = dataManager.getEntriesForSession(
                mode,
                difficulty,
                GameSession.TOTAL_ROUNDS * GameSession.PAIRS_PER_ROUND
        );

        this.activeSession = new GameSession(appLanguage, targetLanguage, mode, difficulty, configKey, pool);
        return this.activeSession;
    }

    // Seçilen sol ve sağ kelime kimliklerini (ID) karşılaştırarak eşleşmeyi değerlendirir.
    // Eşleşme doğruysa polimorfik item.getBasePoints() (+100 puan) eklenir ve true döner;
    // yanlışsa wrongCount 1 artırılır ve false döner.
    public boolean evaluateMatch(int leftItemId, int rightItemId) {
        if (activeSession == null || activeSession.isSessionFinished()) {
            return false;
        }

        if (leftItemId == rightItemId) {
            VocabularyItem matchedItem = findItemInCurrentRound(leftItemId);
            activeSession.recordCorrectMatch(matchedItem);
            return true;
        } else {
            activeSession.recordWrongMatch();
            return false;
        }
    }

    // Mevcut turdaki 5 öğe içinden verilen ID'ye sahip VocabularyItem nesnesini bulur.
    private VocabularyItem findItemInCurrentRound(int itemId) {
        if (activeSession == null) {
            return null;
        }
        List<VocabularyItem> roundItems = activeSession.getCurrentRoundItems();
        for (int i = 0; i < roundItems.size(); i++) {
            VocabularyItem item = roundItems.get(i);
            if (item.getId() == itemId) {
                return item;
            }
        }
        return null;
    }

    // Aktif oturumun sonucunu DataManager üzerinden "records.txt" dosyasına kaydeder.
    public void saveActiveSessionRecord() {
        if (activeSession == null) {
            return;
        }
        dataManager.updateRecord(
                activeSession.getConfigKey(),
                activeSession.getTotalScore(),
                activeSession.getTotalSessionSeconds(),
                activeSession.isCompletedAllRounds()
        );
    }

    public GameSession getActiveSession() {
        return activeSession;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public List<SessionRecord> getSortedRecords() {
        return dataManager.getSortedRecords();
    }

    public List<VocabularyItem> getSortedVocabulary(String mode, String difficulty) {
        return dataManager.getSortedVocabularyItems(mode, difficulty);
    }

    public String getFormattedRecordSummary(String appLang, String targetLang, String mode, String difficulty) {
        String key = DataManager.buildConfigKey(appLang, targetLang, mode, difficulty);
        int hs = dataManager.getHighScore(key);
        double ft = dataManager.getFastestTime(key);
        String timeStr = (ft > 0.0) ? String.format(Locale.US, "%.1f sec", ft) : "--";
        return "Highest Score: " + (hs > 0 ? hs + " pts" : "--") + " | Fastest Time: " + timeStr;
    }
}
