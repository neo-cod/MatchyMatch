package com.matchymatch.ui;

import com.matchymatch.domain.GameSession;
import com.matchymatch.domain.InvalidLanguagePairException;
import com.matchymatch.domain.SessionRecord;
import com.matchymatch.domain.VocabularyItem;
import com.matchymatch.service.GameController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

// ConsoleUI sınıfı: Oyunun komut satırı (Terminal / Console) arayüzünü sağlar.
// "java com.matchymatch.Main --console" komutu ile başlatıldığında Scanner nesnesi üzerinden
// metin tabanlı etkileşimli menü, kelime eşleştirme oyunu, sıralı rekor tablosu ve sözlük listesi sunar.
public class ConsoleUI {

    private static final String[] LANGUAGES = {
        "English", "Turkish", "French", "Italian", "Hungarian", "Arabic"
    };
    private static final String[] MODES = {"Word", "Phrase"};
    private static final String[] DIFFICULTIES = {"Easy", "Medium", "Hard"};

    private final GameController gameController;
    private final Scanner scanner;

    private String appLanguage = "English";
    private String targetLanguage = "Turkish";
    private String mode = "Word";
    private String difficulty = "Easy";
    private boolean preferArabicLatin = false;

    // Kurucu metot (Constructor): Kontrolcü referansını alır ve UTF-8 destekli Scanner başlatır.
    public ConsoleUI(GameController gameController) {
        this.gameController = gameController;
        this.scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    }

    // Ana konsol döngüsünü başlatır; kullanıcı "6" (Exit) seçeneğini girene kadar menüyü gösterir.
    public void start() {
        boolean running = true;
        printBanner();

        while (running) {
            printMainMenu();
            System.out.print("Select an option (1-6): ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String choice = scanner.nextLine().trim();

            if ("1".equals(choice)) {
                playConsoleSession();
            } else if ("2".equals(choice)) {
                configureSettingsMenu();
            } else if ("3".equals(choice)) {
                displaySortedRecords();
            } else if ("4".equals(choice)) {
                displaySortedVocabulary();
            } else if ("5".equals(choice)) {
                displayGameRules();
            } else if ("6".equals(choice) || "0".equals(choice) || "exit".equalsIgnoreCase(choice)) {
                System.out.println("\nThank you for playing MatchyMatch! Goodbye.");
                running = false;
            } else {
                System.out.println("[!] Invalid option. Please enter a number between 1 and 6.");
            }
        }
    }

    private void printBanner() {
        System.out.println("==============================================================");
        System.out.println("          MATCHYMATCH -- VOCABULARY MATCHING GAME             ");
        System.out.println("               Interactive Console Mode (CLI)                 ");
        System.out.println("==============================================================");
    }

    private void printMainMenu() {
        String arabicModeText = preferArabicLatin ? "Latin Alphabet (Marhaba)" : "Arabic Script (مرحبا)";
        System.out.println("\n--------------------------------------------------------------");
        System.out.println("Current Configuration:");
        System.out.println("  App Language : " + appLanguage + "  ->  Target Language : " + targetLanguage);
        System.out.println("  Mode         : " + mode + "         |   Difficulty      : " + difficulty);
        System.out.println("  Arabic Rule  : " + arabicModeText);
        System.out.println("  " + gameController.getFormattedRecordSummary(appLanguage, targetLanguage, mode, difficulty));
        System.out.println("--------------------------------------------------------------");
        System.out.println("  [1] Start 10-Round Matching Session");
        System.out.println("  [2] Change Languages, Mode, Difficulty & Arabic Rule");
        System.out.println("  [3] View Personal Records (Sorted via Comparable)");
        System.out.println("  [4] Browse Vocabulary List (Sorted via Comparator)");
        System.out.println("  [5] Game Rules & Help");
        System.out.println("  [6] Exit");
        System.out.println("--------------------------------------------------------------");
    }

    // Konsol üzerinden 10 turluk kelime eşleştirme oturumunu oynatır.
    private void playConsoleSession() {
        GameSession session;
        try {
            session = gameController.startNewSession(appLanguage, targetLanguage, mode, difficulty, preferArabicLatin);
        } catch (InvalidLanguagePairException e) {
            System.out.println("[ERROR] Cannot start session: " + e.getMessage());
            return;
        }

        long sessionStartMs = System.currentTimeMillis();
        System.out.println("\n>>> Starting Session: " + appLanguage + " -> " + targetLanguage
                + " [" + mode + " / " + difficulty + "]");
        System.out.println(">>> Enter '0' at any prompt to return to the Main Menu.\n");

        while (!session.isSessionFinished()) {
            List<VocabularyItem> leftItems = session.getCurrentRoundItems();
            List<VocabularyItem> rightItems = new ArrayList<>(leftItems);
            Collections.shuffle(rightItems);

            boolean[] leftMatched = new boolean[leftItems.size()];
            boolean[] rightMatched = new boolean[rightItems.size()];

            while (!session.isCurrentRoundComplete() && !session.isSessionFinished()) {
                double elapsedSec = (System.currentTimeMillis() - sessionStartMs) / 1000.0;
                session.setTotalSessionSeconds(elapsedSec);

                System.out.println("\n==============================================================");
                System.out.printf(Locale.US, " ROUND %d / %d   |   SCORE: %d   |   Wrong Count: %s   |   Time: %.1fs%n",
                        session.getCurrentRound(),
                        GameSession.TOTAL_ROUNDS,
                        session.getTotalScore(),
                        session.getWrongCount() == 0 ? "-" : String.valueOf(session.getWrongCount()),
                        session.getTotalSessionSeconds());
                System.out.println("==============================================================");
                System.out.printf(" %-28s | %-28s%n", "LEFT (" + appLanguage + ")", "RIGHT (" + targetLanguage + ")");
                System.out.println("--------------------------------------------------------------");

                for (int i = 0; i < leftItems.size(); i++) {
                    String leftStr = leftMatched[i]
                            ? "[MATCHED]"
                            : gameController.resolveDisplayText(leftItems.get(i), appLanguage);
                    String rightStr = rightMatched[i]
                            ? "[MATCHED]"
                            : gameController.resolveDisplayText(rightItems.get(i), targetLanguage);
                    System.out.printf(" (%d) %-24s | (%d) %-24s%n", i + 1, leftStr, i + 1, rightStr);
                }
                System.out.println("--------------------------------------------------------------");

                int leftChoice = promptIndex("Select LEFT item (1-5, or 0 to quit): ", 0, 5);
                if (leftChoice == 0) {
                    System.out.println("Returning to Console Main Menu...");
                    return;
                }
                if (leftMatched[leftChoice - 1]) {
                    System.out.println("[!] That left item is already matched. Try another.");
                    continue;
                }

                int rightChoice = promptIndex("Select RIGHT item (1-5, or 0 to quit): ", 0, 5);
                if (rightChoice == 0) {
                    System.out.println("Returning to Console Main Menu...");
                    return;
                }
                if (rightMatched[rightChoice - 1]) {
                    System.out.println("[!] That right item is already matched. Try another.");
                    continue;
                }

                VocabularyItem chosenLeft = leftItems.get(leftChoice - 1);
                VocabularyItem chosenRight = rightItems.get(rightChoice - 1);

                boolean correct = gameController.evaluateMatch(chosenLeft.getId(), chosenRight.getId());
                if (correct) {
                    leftMatched[leftChoice - 1] = true;
                    rightMatched[rightChoice - 1] = true;
                    System.out.println("[+100 PTS] Correct match! ("
                            + gameController.resolveDisplayText(chosenLeft, appLanguage) + " = "
                            + gameController.resolveDisplayText(chosenRight, targetLanguage) + ")");
                } else {
                    System.out.println("[X] Wrong match! Wrong Count: " + session.getWrongCount()
                            + " / " + GameSession.MAX_WRONG_COUNT);
                }
            }

            if (session.isCurrentRoundComplete() && !session.isSessionFinished()) {
                if (session.getCurrentRound() < GameSession.TOTAL_ROUNDS) {
                    System.out.println("\n*** Round " + session.getCurrentRound() + " complete! Advancing to next round... ***");
                }
                session.advanceRoundOrComplete();
            }
        }

        double totalSec = (System.currentTimeMillis() - sessionStartMs) / 1000.0;
        session.setTotalSessionSeconds(totalSec);
        gameController.saveActiveSessionRecord();

        System.out.println("\n**************************************************************");
        if (session.isEndedByMistakes()) {
            System.out.println("                        GAME OVER!                            ");
            System.out.println("        You reached the maximum of 10 wrong attempts.         ");
        } else {
            System.out.println("                        COMPLETED!                            ");
            System.out.println("        Congratulations on finishing all 10 rounds!           ");
        }
        System.out.println("  Final Score        : " + session.getTotalScore() + " pts");
        System.out.printf(Locale.US, "  Total Session Time : %.1f sec%n", session.getTotalSessionSeconds());
        System.out.println("  Wrong Count        : " + (session.getWrongCount() == 0 ? "-" : session.getWrongCount()));
        System.out.println("  " + gameController.getFormattedRecordSummary(appLanguage, targetLanguage, mode, difficulty));
        System.out.println("**************************************************************");
    }

    // Dil, mod, zorluk ve Arapça görünüm stratejisini değiştiren ayarlar alt menüsü.
    private void configureSettingsMenu() {
        System.out.println("\n--- Select App Language (Known Language) ---");
        for (int i = 0; i < LANGUAGES.length; i++) {
            System.out.println("  [" + (i + 1) + "] " + LANGUAGES[i]);
        }
        int appIdx = promptIndex("Choice (1-" + LANGUAGES.length + "): ", 1, LANGUAGES.length) - 1;
        String candidateAppLang = LANGUAGES[appIdx];

        System.out.println("\n--- Select Target Language (Language to Learn) ---");
        for (int i = 0; i < LANGUAGES.length; i++) {
            System.out.println("  [" + (i + 1) + "] " + LANGUAGES[i]);
        }
        int targetIdx = promptIndex("Choice (1-" + LANGUAGES.length + "): ", 1, LANGUAGES.length) - 1;
        String candidateTargetLang = LANGUAGES[targetIdx];

        // Özel kontrol edilen istisna (InvalidLanguagePairException) ile dil çiftini kontrol eder
        try {
            gameController.validateLanguagePair(candidateAppLang, candidateTargetLang);
            this.appLanguage = candidateAppLang;
            this.targetLanguage = candidateTargetLang;
        } catch (InvalidLanguagePairException e) {
            System.out.println("[!] Validation Error: " + e.getMessage());
            System.out.println("[!] Keeping previous language pair (" + appLanguage + " -> " + targetLanguage + ").");
        }

        System.out.println("\n--- Select Game Mode ---");
        System.out.println("  [1] Word");
        System.out.println("  [2] Phrase");
        int modeChoice = promptIndex("Choice (1-2): ", 1, 2);
        this.mode = MODES[modeChoice - 1];

        System.out.println("\n--- Select Difficulty ---");
        System.out.println("  [1] Easy");
        System.out.println("  [2] Medium");
        System.out.println("  [3] Hard");
        int diffChoice = promptIndex("Choice (1-3): ", 1, 3);
        this.difficulty = DIFFICULTIES[diffChoice - 1];

        System.out.println("\n--- Select Arabic Alphabet Rule (Strategy Pattern) ---");
        System.out.println("  [1] Arabic Script (مرحبا)");
        System.out.println("  [2] Latin Alphabet (Marhaba)");
        int arabicChoice = promptIndex("Choice (1-2): ", 1, 2);
        this.preferArabicLatin = (arabicChoice == 2);
        gameController.configureDisplayStrategy(appLanguage, targetLanguage, preferArabicLatin);

        System.out.println("[OK] Settings updated! Active Strategy: "
                + gameController.getDisplayStrategy().getStrategyName());
    }

    // Comparable<SessionRecord> ile yüksek puana ve en kısa süreye göre sıralanmış rekorları listeler.
    private void displaySortedRecords() {
        List<SessionRecord> records = gameController.getSortedRecords();
        System.out.println("\n================ PERSONAL RECORDS (Sorted by Score & Time) ================");
        if (records.isEmpty()) {
            System.out.println("  No personal records saved yet. Play a session to set a record!");
        } else {
            for (int i = 0; i < records.size(); i++) {
                SessionRecord rec = records.get(i);
                String timeText = rec.getFastestTimeSeconds() > 0
                        ? String.format(Locale.US, "%.1f sec", rec.getFastestTimeSeconds())
                        : "--";
                System.out.printf("  #%d  %-22s | Highest Score: %4d pts | Fastest Time: %s%n",
                        i + 1, rec.getConfigKey(), rec.getHighestScore(), timeText);
            }
        }
        System.out.println("===========================================================================");
    }

    // Comparator<VocabularyItem> ile İngilizce metne göre alfabetik sıralanmış kelimeleri listeler.
    private void displaySortedVocabulary() {
        List<VocabularyItem> sortedItems = gameController.getSortedVocabulary(mode, difficulty);
        System.out.println("\n========== VOCABULARY LIST [" + mode + " - " + difficulty + "] (Alphabetical) ==========");
        for (int i = 0; i < sortedItems.size(); i++) {
            VocabularyItem item = sortedItems.get(i);
            String leftText = gameController.resolveDisplayText(item, appLanguage);
            String rightText = gameController.resolveDisplayText(item, targetLanguage);
            System.out.printf("  %2d. %-24s  <-->  %-24s  (Type: %s, +%d pts)%n",
                    i + 1, leftText, rightText, item.getModeType(), item.getBasePoints());
        }
        System.out.println("===========================================================================");
    }

    private void displayGameRules() {
        System.out.println("\n=========================== HOW TO PLAY MATCHYMATCH ===========================");
        System.out.println(" 1. Session & Round Structure: 10 Rounds per session, 5 Pairs per round.");
        System.out.println(" 2. Scoring: Every correct match awards a flat +100 points.");
        System.out.println(" 3. Wrong Count Rule: Every wrong match increases Wrong Count by 1.");
        System.out.println("    Reaching 10 wrong attempts ends the session immediately (GAME OVER!).");
        System.out.println(" 4. Arabic Alphabet Rule: When Arabic is selected, choose between Arabic Script");
        System.out.println("    (مرحبا) and Latin Alphabet (Marhaba) using the Strategy Pattern.");
        System.out.println("===============================================================================");
    }

    private int promptIndex(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return min;
            }
            String line = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(line);
                if (val >= min && val <= max) {
                    return val;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("[!] Please enter a valid number between " + min + " and " + max + ".");
        }
    }
}
