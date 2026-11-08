package com.matchymatch;

import com.matchymatch.domain.GameSession;
import com.matchymatch.domain.InvalidLanguagePairException;
import com.matchymatch.domain.PhraseItem;
import com.matchymatch.domain.SessionRecord;
import com.matchymatch.domain.VocabularyItem;
import com.matchymatch.domain.WordItem;
import com.matchymatch.persistence.DataManager;
import com.matchymatch.service.ArabicLatinDisplayStrategy;
import com.matchymatch.service.ArabicScriptDisplayStrategy;
import com.matchymatch.service.GameController;
import com.matchymatch.service.LanguageDisplayStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// MatchyMatchTest sınıfı: Prog 2 ödev kriterlerine uygun olarak OOP prensiplerini,
// Strategy tasarım desenini, özel istisna yönetimini, sıralama arayüzlerini, oyun kurallarını
// ve dosya okuma/yazma (DataManager) işlemlerini doğrulayan JUnit 5 birim testi sınıfıdır.
public class MatchyMatchTest {

    // Test 1: Kapsülleme (Encapsulation), Kalıtım (Inheritance) ve Çok Biçimlilik (Polymorphism) testi.
    // WordItem ve PhraseItem alt sınıflarının VocabularyItem üst sınıf referansı ile davranışını doğrular.
    @Test
    public void testInheritanceAndPolymorphism() {
        VocabularyItem word = new WordItem(
                1, "Easy",
                "Water", "Su", "Eau", "Acqua", "Víz", "ماء", "Maa'"
        );
        VocabularyItem phrase = new PhraseItem(
                91, "Medium",
                "Good morning", "Günaydın", "Bonjour", "Buongiorno", "Jó reggelt", "صباح الخير", "Sabah al-khayr"
        );

        assertEquals(1, word.getId());
        assertEquals("Word", word.getModeType());
        assertEquals(100, word.getBasePoints());
        assertEquals("Su", word.getTextForLanguage("Turkish", false));
        assertEquals("Water", word.formatDisplayText("  Water  "));
        assertTrue(word instanceof WordItem);

        assertEquals(91, phrase.getId());
        assertEquals("Phrase", phrase.getModeType());
        assertEquals(100, phrase.getBasePoints());
        assertEquals("Good morning", phrase.getTextForLanguage("English", false));
        assertEquals("Good morning", phrase.formatDisplayText("  Good morning  "));
        assertTrue(phrase instanceof PhraseItem);
    }

    // Test 2: Kurucu metot doğrulaması (Constructor Validation / Encapsulation) testi.
    // Geçersiz ID (<= 0) veya boş çeviri metni verildiğinde IllegalArgumentException fırlatıldığını doğrular.
    @Test
    public void testVocabularyItemConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new WordItem(
                0, "Easy",
                "Water", "Su", "Eau", "Acqua", "Víz", "ماء", "Maa'"
        ));

        assertThrows(IllegalArgumentException.class, () -> new PhraseItem(
                10, "Easy",
                "", "Merhaba", "Bonjour", "Ciao", "Szia", "مرحبا", "Marhaba"
        ));
    }

    // Test 3: Strategy Tasarım Deseni (LanguageDisplayStrategy) testi.
    // Arapça kelimelerde ArabicScriptDisplayStrategy ("مرحبا") ve ArabicLatinDisplayStrategy ("Marhaba")
    // arasında çalışma zamanında geçiş yapılabildiğini doğrular.
    @Test
    public void testArabicLanguageDisplayStrategies() {
        VocabularyItem helloPhrase = new PhraseItem(
                91, "Easy",
                "Hello", "Merhaba", "Bonjour", "Ciao", "Szia", "مرحبا", "Marhaba"
        );

        LanguageDisplayStrategy scriptStrategy = new ArabicScriptDisplayStrategy();
        LanguageDisplayStrategy latinStrategy = new ArabicLatinDisplayStrategy();

        assertEquals("مرحبا", scriptStrategy.resolveText(helloPhrase, "Arabic"));
        assertEquals("Marhaba", latinStrategy.resolveText(helloPhrase, "Arabic"));

        // Arapça dışındaki dillerde her iki strateji de normal çeviriyi döndürmelidir
        assertEquals("Merhaba", scriptStrategy.resolveText(helloPhrase, "Turkish"));
        assertEquals("Merhaba", latinStrategy.resolveText(helloPhrase, "Turkish"));
    }

    // Test 4: Özel Kontrol Edilen İstisna (Custom Checked Exception - InvalidLanguagePairException) testi.
    // Uygulama Dili ile Öğrenilecek Hedef Dil aynı seçildiğinde InvalidLanguagePairException fırlatıldığını doğrular.
    @Test
    public void testInvalidLanguagePairException(@TempDir Path tempDir) {
        File tempWords = tempDir.resolve("words.csv").toFile();
        File tempRecords = tempDir.resolve("records.txt").toFile();
        DataManager dm = new DataManager(tempWords, tempRecords);
        GameController controller = new GameController(dm);

        assertThrows(InvalidLanguagePairException.class, () -> {
            controller.validateLanguagePair("English", "English");
        });

        assertThrows(InvalidLanguagePairException.class, () -> {
            controller.startNewSession("Turkish", "Turkish", "Word", "Easy", false);
        });
    }

    // Test 5: Comparable<SessionRecord> ile doğal sıralama testi.
    // Yüksek puanlı rekorların önce, eşit puanda ise daha kısa sürede biten rekorların önce sıralandığını doğrular.
    @Test
    public void testSessionRecordComparableSorting() {
        SessionRecord r1 = new SessionRecord("EN-TR-WORD-EASY", 3000, 45.0);
        SessionRecord r2 = new SessionRecord("EN-FR-WORD-EASY", 5000, 52.0);
        SessionRecord r3 = new SessionRecord("EN-IT-WORD-EASY", 5000, 38.5);

        List<SessionRecord> records = new ArrayList<>();
        records.add(r1);
        records.add(r2);
        records.add(r3);

        Collections.sort(records);

        // 1. sırada 5000 puan ve 38.5 sn olan r3 yer almalıdır
        assertEquals("EN-IT-WORD-EASY", records.get(0).getConfigKey());
        // 2. sırada 5000 puan ve 52.0 sn olan r2 yer almalıdır
        assertEquals("EN-FR-WORD-EASY", records.get(1).getConfigKey());
        // 3. sırada 3000 puan olan r1 yer almalıdır
        assertEquals("EN-TR-WORD-EASY", records.get(2).getConfigKey());
    }

    // Test 6: Comparator<VocabularyItem> ile alfabetik kelime sıralaması testi.
    // DataManager.getSortedVocabularyItems(...) metodunun kelimeleri İngilizce metne göre A'dan Z'ye sıraladığını doğrular.
    @Test
    public void testVocabularyAlphabeticalComparatorSorting(@TempDir Path tempDir) {
        File tempWords = tempDir.resolve("words.csv").toFile();
        File tempRecords = tempDir.resolve("records.txt").toFile();
        DataManager dm = new DataManager(tempWords, tempRecords);

        List<VocabularyItem> sortedEasyWords = dm.getSortedVocabularyItems("Word", "Easy");
        assertEquals(35, sortedEasyWords.size());

        for (int i = 0; i < sortedEasyWords.size() - 1; i++) {
            String currentEn = sortedEasyWords.get(i).getEnglish();
            String nextEn = sortedEasyWords.get(i + 1).getEnglish();
            assertTrue(currentEn.compareToIgnoreCase(nextEn) <= 0,
                    "Expected alphabetical order between '" + currentEn + "' and '" + nextEn + "'");
        }
    }

    // Test 7: Oyun oturumu (GameSession ve GameController) puanlama (+100) ve 10 hata (Game Over) kuralı testi.
    @Test
    public void testGameSessionScoringAndTenMistakesGameOver(@TempDir Path tempDir) throws InvalidLanguagePairException {
        File tempWords = tempDir.resolve("words.csv").toFile();
        File tempRecords = tempDir.resolve("records.txt").toFile();
        DataManager dm = new DataManager(tempWords, tempRecords);
        GameController controller = new GameController(dm);

        GameSession session = controller.startNewSession("English", "Arabic", "Word", "Easy", true);
        assertNotNull(session);
        assertEquals(1, session.getCurrentRound());
        assertEquals(0, session.getTotalScore());
        assertEquals(0, session.getWrongCount());

        // Doğru eşleşme: +100 puan kazandırmalıdır
        List<VocabularyItem> roundItems = session.getCurrentRoundItems();
        int firstId = roundItems.get(0).getId();
        int secondId = roundItems.get(1).getId();

        assertTrue(controller.evaluateMatch(firstId, firstId));
        assertEquals(100, session.getTotalScore());
        assertEquals(1, session.getMatchedPairsInRound());

        // 10 kez yanlış eşleşme yapıldığında oturum anında "Game Over" olarak sonlanmalıdır
        for (int i = 1; i <= 10; i++) {
            assertFalse(controller.evaluateMatch(firstId, secondId));
            assertEquals(i, session.getWrongCount());
        }

        assertTrue(session.isSessionFinished());
        assertTrue(session.isEndedByMistakes());
        assertFalse(session.isCompletedAllRounds());
    }

    // Test 8: Dosya Okuma/Yazma (DataManager File I/O) testi.
    // Geçici dizinde (@TempDir) 180 kelimenin (her kategoride 30 adet) üretilip okunduğunu
    // ve rekorların "records.txt" dosyasına kaydedilip geri yüklendiğini doğrular.
    @Test
    public void testDataManagerPersistenceAndRecordSaving(@TempDir Path tempDir) {
        File tempWords = tempDir.resolve("words.csv").toFile();
        File tempRecords = tempDir.resolve("records.txt").toFile();

        DataManager dm = new DataManager(tempWords, tempRecords);
        assertTrue(tempWords.exists());
        assertTrue(dm.isLoadedDataValid());

        assertEquals(35, dm.getSortedVocabularyItems("Word", "Easy").size());
        assertEquals(35, dm.getSortedVocabularyItems("Word", "Medium").size());
        assertEquals(35, dm.getSortedVocabularyItems("Word", "Hard").size());
        assertEquals(25, dm.getSortedVocabularyItems("Phrase", "Easy").size());
        assertEquals(25, dm.getSortedVocabularyItems("Phrase", "Medium").size());
        assertEquals(25, dm.getSortedVocabularyItems("Phrase", "Hard").size());

        String key = DataManager.buildConfigKey("English", "Turkish", "Word", "Easy");
        dm.updateRecord(key, 4800, 41.2, true);
        assertTrue(tempRecords.exists());

        // Yeni bir DataManager nesnesi oluşturarak diskteki "records.txt" dosyasından okuma yapar
        DataManager reloadedDm = new DataManager(tempWords, tempRecords);
        assertEquals(4800, reloadedDm.getHighScore(key));
        assertEquals(41.2, reloadedDm.getFastestTime(key), 0.05);
    }
}
