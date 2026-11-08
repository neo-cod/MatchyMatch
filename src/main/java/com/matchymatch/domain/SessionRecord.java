package com.matchymatch.domain;

import java.util.Locale;

// SessionRecord sınıfı: Belirli bir dil/mod/zorluk kombinasyonuna (Örn: "EN-TR-WORD-EASY") ait
// En Yüksek Skor (Highest Score) ve En Hızlı Süre (Fastest Time) rekorlarını kapsülleyen veri modelidir.
// Comparable<SessionRecord> arayüzünü uygulayarak rekorların yüksek skora göre azalan (descending),
// skorlar eşitse en hızlı süreye göre artan (ascending) sırada sıralanmasını sağlar.
public class SessionRecord implements Comparable<SessionRecord> {

    private final String configKey;
    private int highestScore;
    private double fastestTimeSeconds; // Henüz 10 tur tamamlanmadıysa -1.0 değerini alır

    // Kurucu metot (Constructor): Rekor anahtarını ve başlangıç değerlerini doğrular.
    public SessionRecord(String configKey, int highestScore, double fastestTimeSeconds) {
        if (configKey == null || configKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration key cannot be null or empty.");
        }
        this.configKey = configKey.trim().toUpperCase(Locale.ROOT);
        this.highestScore = Math.max(0, highestScore);
        this.fastestTimeSeconds = (fastestTimeSeconds > 0.0) ? fastestTimeSeconds : -1.0;
    }

    // Yeni bir oyun bittiğinde skoru ve (10 turun tamamı tamamlandıysa) en hızlı süreyi günceller.
    public void updateWithSessionResult(int newScore, double newTimeSeconds, boolean completedAllRounds) {
        if (newScore > this.highestScore) {
            this.highestScore = newScore;
        }
        if (completedAllRounds && newTimeSeconds > 0.0) {
            if (this.fastestTimeSeconds < 0.0 || newTimeSeconds < this.fastestTimeSeconds) {
                this.fastestTimeSeconds = newTimeSeconds;
            }
        }
    }

    // "records.txt" dosyasına yazılacak "SKOR,SÜRE" (Örn: "1000,24.5") metnini üretir.
    public String toFileValueString() {
        String formattedTime = "-1.0";
        if (fastestTimeSeconds > 0.0) {
            formattedTime = String.format(Locale.US, "%.1f", fastestTimeSeconds);
        }
        return highestScore + "," + formattedTime;
    }

    // Comparable<SessionRecord> uygulaması:
    // 1. Önce highestScore değerine göre büyükten küçüğe (descending) sıralar.
    // 2. Skorlar eşitse, tamamlanmış süresi (fastestTimeSeconds > 0) daha küçük (daha hızlı) olanı öne alır.
    @Override
    public int compareTo(SessionRecord other) {
        if (other == null) {
            return -1;
        }
        if (this.highestScore != other.highestScore) {
            return Integer.compare(other.highestScore, this.highestScore);
        }
        boolean thisHasTime = this.fastestTimeSeconds > 0.0;
        boolean otherHasTime = other.fastestTimeSeconds > 0.0;
        if (thisHasTime && otherHasTime) {
            return Double.compare(this.fastestTimeSeconds, other.fastestTimeSeconds);
        } else if (thisHasTime) {
            return -1;
        } else if (otherHasTime) {
            return 1;
        }
        return this.configKey.compareTo(other.configKey);
    }

    public String getConfigKey() {
        return configKey;
    }

    public int getHighestScore() {
        return highestScore;
    }

    public double getFastestTimeSeconds() {
        return fastestTimeSeconds;
    }

    @Override
    public String toString() {
        String timeStr = (fastestTimeSeconds > 0.0)
                ? String.format(Locale.US, "%.1fs", fastestTimeSeconds)
                : "--";
        return configKey + " -> Score: " + highestScore + " pts, Fastest: " + timeStr;
    }
}
