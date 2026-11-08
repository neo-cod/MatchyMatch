package com.matchymatch.domain;

import java.util.ArrayList;
import java.util.List;

// GameSession sınıfı: 10 turluk (her turda 5 kelime çifti) aktif bir oyun oturumunun tüm durumunu
// (mevcut tur, skor, yanlış sayısı, geçen süre ve o tura ait VocabularyItem listesi) kapsülleyen sınıftır.
// Nesneler arası ilişki (Composition / Aggregation): GameSession nesnesi birden fazla VocabularyItem içerir.
public class GameSession {

    public static final int TOTAL_ROUNDS = 10;
    public static final int PAIRS_PER_ROUND = 5;
    public static final int MAX_WRONG_COUNT = 10;

    // Oturum yapılandırma bilgileri
    private final String appLanguage;
    private final String targetLanguage;
    private final String mode;
    private final String difficulty;
    private final String configKey;

    // 10 tur x 5 çift = 50 öğelik oturum havuzu ve mevcut turun 5 öğesi (Polymorphic List<VocabularyItem>)
    private final List<VocabularyItem> sessionPool;
    private final List<VocabularyItem> currentRoundItems;

    // Oturum ilerleme ve durum değişkenleri
    private int currentRound;
    private int matchedPairsInRound;
    private int totalScore;
    private int wrongCount;
    private double totalSessionSeconds;
    private boolean sessionFinished;
    private boolean endedByMistakes;

    // Kurucu metot (Constructor): Yeni bir 10 turluk oyun oturumu başlatır ve 1. turu yükler.
    public GameSession(String appLanguage, String targetLanguage, String mode, String difficulty,
                       String configKey, List<VocabularyItem> sessionPool) {
        this.appLanguage = appLanguage;
        this.targetLanguage = targetLanguage;
        this.mode = mode;
        this.difficulty = difficulty;
        this.configKey = configKey;
        this.sessionPool = new ArrayList<>(sessionPool);
        this.currentRoundItems = new ArrayList<>();

        this.currentRound = 1;
        this.matchedPairsInRound = 0;
        this.totalScore = 0;
        this.wrongCount = 0;
        this.totalSessionSeconds = 0.0;
        this.sessionFinished = false;
        this.endedByMistakes = false;

        loadRoundItems(1);
    }

    // Belirtilen tur numarası (1..10) için havuzdan 5 adet VocabularyItem seçip currentRoundItems listesine yükler.
    public final void loadRoundItems(int roundNumber) {
        this.currentRound = roundNumber;
        this.matchedPairsInRound = 0;
        this.currentRoundItems.clear();

        if (sessionPool.isEmpty()) {
            return;
        }

        int startIndex = (roundNumber - 1) * PAIRS_PER_ROUND;
        for (int i = 0; i < PAIRS_PER_ROUND; i++) {
            int poolIndex = (startIndex + i) % sessionPool.size();
            this.currentRoundItems.add(sessionPool.get(poolIndex));
        }
    }

    // Doğru eşleşme yapıldığında öğenin taban puanını (item.getBasePoints() = +100) çok biçimli olarak skora ekler.
    public void recordCorrectMatch(VocabularyItem matchedItem) {
        if (sessionFinished) {
            return;
        }
        int points = (matchedItem != null) ? matchedItem.getBasePoints() : VocabularyItem.DEFAULT_MATCH_POINTS;
        this.totalScore += points;
        this.matchedPairsInRound++;
    }

    // Yanlış eşleşme yapıldığında wrongCount sayacını 1 artırır; 10 hataya ulaşılırsa oturumu GAME OVER olarak bitirir.
    public void recordWrongMatch() {
        if (sessionFinished) {
            return;
        }
        this.wrongCount++;
        if (this.wrongCount >= MAX_WRONG_COUNT) {
            this.sessionFinished = true;
            this.endedByMistakes = true;
        }
    }

    // Mevcut turdaki 5 çiftin tamamının eşleştirilip eşleştirilmediğini kontrol eder.
    public boolean isCurrentRoundComplete() {
        return matchedPairsInRound >= PAIRS_PER_ROUND;
    }

    // Bir sonraki tura geçer veya 10. tur tamamlandıysa oturumu başarıyla bitirir.
    public void advanceRoundOrComplete() {
        if (sessionFinished) {
            return;
        }
        if (currentRound < TOTAL_ROUNDS) {
            loadRoundItems(currentRound + 1);
        } else {
            this.sessionFinished = true;
            this.endedByMistakes = false;
        }
    }

    // Geçen süreyi saniye cinsinden toplama ekler.
    public void addElapsedSeconds(double deltaSeconds) {
        if (!sessionFinished && deltaSeconds > 0.0) {
            this.totalSessionSeconds += deltaSeconds;
        }
    }

    // Oturumun toplam süresini saniye cinsinden günceller.
    public void setTotalSessionSeconds(double totalSessionSeconds) {
        if (totalSessionSeconds >= 0.0) {
            this.totalSessionSeconds = totalSessionSeconds;
        }
    }

    // Oturumu dışarıdan sonlandırmak için kullanılan yardımcı metot.
    public void markFinished(boolean failedByMistakes) {
        this.sessionFinished = true;
        this.endedByMistakes = failedByMistakes;
    }

    // Standart Getter metotları
    public String getAppLanguage() {
        return appLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public String getMode() {
        return mode;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getConfigKey() {
        return configKey;
    }

    public List<VocabularyItem> getSessionPool() {
        return new ArrayList<>(sessionPool);
    }

    public List<VocabularyItem> getCurrentRoundItems() {
        return new ArrayList<>(currentRoundItems);
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public int getMatchedPairsInRound() {
        return matchedPairsInRound;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getWrongCount() {
        return wrongCount;
    }

    public double getTotalSessionSeconds() {
        return totalSessionSeconds;
    }

    public boolean isSessionFinished() {
        return sessionFinished;
    }

    public boolean isEndedByMistakes() {
        return endedByMistakes;
    }

    public boolean isCompletedAllRounds() {
        return sessionFinished && !endedByMistakes;
    }
}
