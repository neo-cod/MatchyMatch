import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

// GamePanel sınıfı: Oyunun 60 FPS hızında çalışan aktif kelime eşleştirme ekranıdır.
// 1. javax.swing.Timer kullanarak her 16 milisaniyede bir (~60 FPS) oyun durumunu ve animasyonları günceller.
// 2. Graphics2D kütüphanesi ile tüm kelime kutularını (WordBox), üst bilgi çubuğunu (HUD) ve "+100" puan animasyonlarını çizer.
// 3. Her doğru eşleşmede +100 puan verir; her yanlış eşleşmede "Wrong Count" sayacını 1 artırır.
// 4. Yanlış sayısı 10'a ulaştığında ("GAME OVER!") veya 10 tur tamamlandığında ("COMPLETED!") oturumu bitirir.
public class GamePanel extends JPanel {

    // Oyun kuralları ve 60 FPS zamanlayıcı sabitleri
    private static final int TOTAL_ROUNDS = 10;      // Bir oturumdaki toplam tur sayısı
    private static final int PAIRS_PER_ROUND = 5;    // Her turdaki eşleştirilecek kelime çifti sayısı
    private static final int MATCH_POINTS = 100;     // Her doğru eşleşmede kazanılan sabit puan
    private static final int MAX_WRONG_COUNT = 10;   // Oyun bitişine (Game Over) sebep olan maksimum hata sayısı
    private static final int FPS_INTERVAL_MS = 16;   // ~60 FPS için milisaniye cinsinden yenileme aralığı (1000 / 60 ≈ 16ms)

    // Ana pencere (Main) ve veri yöneticisi (DataManager) referansları
    private final Main mainFrame;
    private final DataManager dataManager;

    // Seçilen oyun oturumu ayarları
    private String mainLanguage;
    private String targetLanguage;
    private String mode;                  // "Word" veya "Phrase"
    private String difficulty;            // "Easy", "Medium" veya "Hard"
    private boolean useArabicLatin;
    private boolean sfxEnabled;
    private String configKey;             // "records.txt" için oluşturulan tekil anahtar (Örn: "EN-TR-WORD-EASY")

    // Aktif oyun oturumu durum değişkenleri
    private ArrayList<WordEntry> sessionWordPool;
    private int currentRound;             // Mevcut tur (1'den 10'a kadar)
    private int matchedPairsInRound;      // Mevcut turda eşleştirilen çift sayısı (0'dan 5'e kadar)
    private int totalScore;               // Toplam puan
    private int wrongCount;               // Yapılan yanlış eşleştirme sayısı (en fazla 10)
    private double totalSessionSeconds;   // Oturumun başlangıcından itibaren geçen toplam süre (saniye)

    // Tur geçişlerinde son eşleşmenin kaybolma animasyonunu beklemek için kullanılan sayaç
    private int roundTransitionTicks;

    // Oturum sonu (Completed / Game Over) özet ekranı durum değişkenleri
    private boolean sessionFinished;
    private boolean endedByMistakes;
    private boolean achievedNewHighScore;
    private boolean achievedNewFastestTime;
    private int savedBestScore;
    private double savedFastestTime;

    // Sol sütun (Uygulama Dili) ve sağ sütun (Öğrenilecek Dil) kelime kutularını tutan ArrayList koleksiyonları
    private final ArrayList<WordBox> leftBoxes;
    private final ArrayList<WordBox> rightBoxes;
    private WordBox selectedLeftBox;
    private WordBox selectedRightBox;

    // Doğru eşleşmelerde yukarı doğru süzülen "+100" yazılarını tutan ArrayList
    private final ArrayList<FloatingText> floatingTexts;

    // Tuval (Canvas) üzerine çizilen etkileşimli butonların geometrik dikdörtgen alanları
    private final RoundRectangle2D backButtonRect;
    private final RoundRectangle2D playAgainButtonRect;
    private final RoundRectangle2D mainMenuButtonRect;
    private boolean hoverBackButton;
    private boolean hoverPlayAgainButton;
    private boolean hoverMainMenuButton;

    // Ses kesilmelerini önlemek için başlangıçta bir kez oluşturulan yeniden kullanılabilir (reusable) Clip nesneleri
    private final Clip selectClip;
    private final Clip matchClip;
    private final Clip wrongClip;

    // 60 FPS oyun döngüsünü yürüten tekil Swing Timer nesnesi
    private final Timer gameLoopTimer;
    private long lastTickNanoTime;

    // Kurucu metot (Constructor): Oyun panelini, ArrayList yapılarını, ses kliplerini ve 60 FPS zamanlayıcıyı başlatır.
    public GamePanel(Main mainFrame, DataManager dataManager) {
        this.mainFrame = mainFrame;
        this.dataManager = dataManager;

        // Ekran titremesini (flickering) önlemek için çift tamponlamayı (Double Buffering) etkinleştirir
        setDoubleBuffered(true);
        setBackground(Color.WHITE);

        this.leftBoxes = new ArrayList<>();
        this.rightBoxes = new ArrayList<>();
        this.floatingTexts = new ArrayList<>();

        // Sol üstteki "← Main Menu" butonu ve oyun sonu butonlarının koordinat şablonları
        this.backButtonRect = new RoundRectangle2D.Double(28, 24, 150, 44, 14, 14);
        this.playAgainButtonRect = new RoundRectangle2D.Double(0, 0, 200, 46, 14, 14);
        this.mainMenuButtonRect = new RoundRectangle2D.Double(0, 0, 200, 46, 14, 14);

        // Seçim, doğru eşleşme ve yanlış eşleşme seslerini belleğe (RAM) bir kez yükler
        this.selectClip = createPreloadedClip(780.0, 440.0, 22);
        this.matchClip = createPreloadedClip(580.0, 880.0, 95);
        this.wrongClip = createPreloadedClip(250.0, 170.0, 130);

        setupMouseListeners();

        // Her 16 milisaniyede bir onFrameTick() metodunu çağıran 60 FPS Swing Timer
        this.gameLoopTimer = new Timer(FPS_INTERVAL_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onFrameTick();
            }
        });
    }

    // Verilen başlangıç/bitiş frekanslarına göre matematiksel PCM ses dalgası üretir ve hazır bir Clip döndürür.
    private Clip createPreloadedClip(double startFreq, double endFreq, int durationMs) {
        try {
            float sampleRate = 22050f;
            int totalSamples = (int) (durationMs * (sampleRate / 1000f));
            byte[] buffer = new byte[totalSamples];

            for (int i = 0; i < totalSamples; i++) {
                double progress = (double) i / totalSamples;
                double freq = startFreq + (endFreq - startFreq) * progress;
                double angle = (i / sampleRate) * freq * 2.0 * Math.PI;
                double envelope = Math.pow(1.0 - progress, 1.5);
                buffer[i] = (byte) (Math.sin(angle) * 55.0 * envelope);
            }

            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
            Clip clip = AudioSystem.getClip();
            clip.open(format, buffer, 0, buffer.length);
            return clip;
        } catch (Exception ignored) {
            return null;
        }
    }

    // Bellekte hazır bekleyen ses klibini başa sararak (frame 0) gecikmesiz çalar.
    private void playSoundClip(Clip clip) {
        if (!sfxEnabled || clip == null) {
            return;
        }
        try {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception ignored) {
        }
    }

    // Pencere kapatıldığında zamanlayıcıyı durdurur ve tüm ses kanallarını serbest bırakır.
    public void closeAudioResources() {
        stopGameLoop();
        closeClipQuietly(selectClip);
        closeClipQuietly(matchClip);
        closeClipQuietly(wrongClip);
    }

    // Tek bir Clip nesnesini güvenli şekilde kapatan yardımcı metot.
    private void closeClipQuietly(Clip clip) {
        if (clip != null) {
            try {
                clip.stop();
                clip.close();
            } catch (Exception ignored) {
            }
        }
    }

    // Seçilen menü ayarları ile yeni bir 10 turluk oyun oturumu başlatır.
    public void startNewSession(String appLang, String targetLang, String mode, String difficulty,
                                boolean useArabicLatin, boolean sfxEnabled) {
        this.mainLanguage = appLang;
        this.targetLanguage = targetLang;
        this.mode = mode;
        this.difficulty = difficulty;
        this.sfxEnabled = sfxEnabled;

        // Arapça Alfabe Kuralının yalnızca dillerden biri Arapça olduğunda uygulanmasını sağlar
        boolean arabicUsed = "ARABIC".equals(appLang.trim().toUpperCase(Locale.ROOT))
                || "ARABIC".equals(targetLang.trim().toUpperCase(Locale.ROOT));
        this.useArabicLatin = arabicUsed && useArabicLatin;

        this.configKey = DataManager.buildConfigKey(appLang, targetLang, mode, difficulty);

        this.totalScore = 0;
        this.wrongCount = 0;
        this.totalSessionSeconds = 0.0;
        this.currentRound = 1;
        this.roundTransitionTicks = 0;
        this.sessionFinished = false;
        this.endedByMistakes = false;
        this.achievedNewHighScore = false;
        this.achievedNewFastestTime = false;

        this.floatingTexts.clear();

        // DataManager içindeki ilgili kategori listesinden 10 tur x 5 çift = 50 kelimelik karıştırılmış havuzu alır
        this.sessionWordPool = dataManager.getEntriesForSession(
                this.mode, this.difficulty, TOTAL_ROUNDS * PAIRS_PER_ROUND
        );

        // 1. Turu yükler ve 60 FPS zamanlayıcıyı başlatır
        loadRound(1);

        this.lastTickNanoTime = System.nanoTime();
        this.gameLoopTimer.start();
        requestFocusInWindow();
    }

    // 60 FPS oyun döngüsü zamanlayıcısını durdurur.
    public void stopGameLoop() {
        if (gameLoopTimer != null && gameLoopTimer.isRunning()) {
            gameLoopTimer.stop();
        }
    }

    // Belirtilen tur numarası (1-10) için sol sütuna 5 kelime (Uygulama Dili) ve sağ sütuna karıştırılmış 5 kelime (Hedef Dil) yükler.
    private void loadRound(int roundNumber) {
        this.currentRound = roundNumber;
        this.matchedPairsInRound = 0;
        this.selectedLeftBox = null;
        this.selectedRightBox = null;

        leftBoxes.clear();
        rightBoxes.clear();

        int startIndex = (roundNumber - 1) * PAIRS_PER_ROUND;
        ArrayList<WordEntry> roundWords = new ArrayList<>();
        for (int i = 0; i < PAIRS_PER_ROUND; i++) {
            int poolIndex = (startIndex + i) % sessionWordPool.size();
            roundWords.add(sessionWordPool.get(poolIndex));
        }

        // Sağ sütundaki (Öğrenilecek Dil) kelimelerin sırasını Collections.shuffle ile rastgele karıştırır
        ArrayList<WordEntry> shuffledRightWords = new ArrayList<>(roundWords);
        Collections.shuffle(shuffledRightWords);

        for (int i = 0; i < PAIRS_PER_ROUND; i++) {
            WordEntry leftWord = roundWords.get(i);
            String leftText = leftWord.getTextForLanguage(mainLanguage, useArabicLatin);
            leftBoxes.add(new WordBox(leftWord.getId(), leftText, true));

            WordEntry rightWord = shuffledRightWords.get(i);
            String rightText = rightWord.getTextForLanguage(targetLanguage, useArabicLatin);
            rightBoxes.add(new WordBox(rightWord.getId(), rightText, false));
        }

        updateBoxCoordinates();
    }

    // Pencere boyutuna göre sol ve sağ sütundaki 5'er kelime kutusunu (320x72 px) ekranın ortasına simetrik olarak yerleştirir.
    private void updateBoxCoordinates() {
        int panelW = Math.max(getWidth(), 960);
        int panelH = Math.max(getHeight(), 640);

        int boxWidth = 320;
        int boxHeight = 72;
        int rowSpacing = 18;
        int columnGap = 64;

        int totalGridHeight = (PAIRS_PER_ROUND * boxHeight) + ((PAIRS_PER_ROUND - 1) * rowSpacing);
        int topStart = Math.max(130, (panelH + 92 - totalGridHeight) / 2);

        int leftColumnX = (panelW / 2) - boxWidth - (columnGap / 2);
        int rightColumnX = (panelW / 2) + (columnGap / 2);

        for (int i = 0; i < leftBoxes.size(); i++) {
            int y = topStart + i * (boxHeight + rowSpacing);
            leftBoxes.get(i).setBounds(leftColumnX, y, boxWidth, boxHeight);
        }
        for (int i = 0; i < rightBoxes.size(); i++) {
            int y = topStart + i * (boxHeight + rowSpacing);
            rightBoxes.get(i).setBounds(rightColumnX, y, boxWidth, boxHeight);
        }
    }

    // Fare hareketlerini (hover) ve sol tıklamaları dinleyen MouseAdapter yapılandırması.
    private void setupMouseListeners() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHoverStates(e.getX(), e.getY());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    handleMouseClick(e.getX(), e.getY());
                }
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    // Yardımcı metot: Fare imlecinin konumuna göre butonların ve kelime kutularının üzerine gelme (hover) durumunu günceller.
    private void updateHoverStates(int mx, int my) {
        boolean anyHandCursor = false;

        hoverBackButton = backButtonRect.contains(mx, my);
        if (hoverBackButton) {
            anyHandCursor = true;
        }

        if (sessionFinished) {
            hoverPlayAgainButton = playAgainButtonRect.contains(mx, my);
            hoverMainMenuButton = mainMenuButtonRect.contains(mx, my);
            if (hoverPlayAgainButton || hoverMainMenuButton) {
                anyHandCursor = true;
            }
        } else if (roundTransitionTicks <= 0) {
            for (int i = 0; i < leftBoxes.size(); i++) {
                WordBox box = leftBoxes.get(i);
                box.hovered = (!box.matched && !box.fadingOut && box.contains(mx, my));
                if (box.hovered) {
                    anyHandCursor = true;
                }
            }
            for (int i = 0; i < rightBoxes.size(); i++) {
                WordBox box = rightBoxes.get(i);
                box.hovered = (!box.matched && !box.fadingOut && box.contains(mx, my));
                if (box.hovered) {
                    anyHandCursor = true;
                }
            }
        }

        if (anyHandCursor) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    // Yardımcı metot: Sol fare tıklamasının hangi butona veya kelime kutusuna yapıldığını kontrol eder.
    private void handleMouseClick(int mx, int my) {
        // 1. Sol üstteki "← Main Menu" butonuna tıklandıysa ana menüye döner
        if (backButtonRect.contains(mx, my)) {
            playSoundClip(selectClip);
            stopGameLoop();
            mainFrame.showMainMenu();
            return;
        }

        // 2. Oyun bittiyse özet ekranındaki "Play Again" veya "Back to Main Menu" butonlarını kontrol eder
        if (sessionFinished) {
            if (playAgainButtonRect.contains(mx, my)) {
                playSoundClip(selectClip);
                startNewSession(mainLanguage, targetLanguage, mode, difficulty, useArabicLatin, sfxEnabled);
            } else if (mainMenuButtonRect.contains(mx, my)) {
                playSoundClip(selectClip);
                stopGameLoop();
                mainFrame.showMainMenu();
            }
            return;
        }

        // Tur geçiş animasyonu sırasındaki tıklamaları yoksayar
        if (roundTransitionTicks > 0) {
            return;
        }

        // 3. Sol sütundaki kelime kutularını kontrol eder
        for (int i = 0; i < leftBoxes.size(); i++) {
            WordBox box = leftBoxes.get(i);
            if (!box.matched && !box.fadingOut && box.contains(mx, my)) {
                handleBoxSelection(box);
                return;
            }
        }

        // 4. Sağ sütundaki kelime kutularını kontrol eder
        for (int i = 0; i < rightBoxes.size(); i++) {
            WordBox box = rightBoxes.get(i);
            if (!box.matched && !box.fadingOut && box.contains(mx, my)) {
                handleBoxSelection(box);
                return;
            }
        }
    }

    // Sol veya sağ sütundan bir kelime kutusu seçildiğinde çalışır; her iki sütundan birer kutu seçildiğinde eşleşmeyi kontrol eder.
    private void handleBoxSelection(WordBox clickedBox) {
        if (clickedBox.wrongTicks > 0) {
            return;
        }

        if (clickedBox.isLeftColumn) {
            // Aynı sol kutuya tekrar tıklandıysa seçimi kaldırır
            if (selectedLeftBox == clickedBox) {
                selectedLeftBox.selected = false;
                selectedLeftBox = null;
                playSoundClip(selectClip);
            } else {
                if (selectedLeftBox != null) {
                    selectedLeftBox.selected = false;
                }
                selectedLeftBox = clickedBox;
                selectedLeftBox.selected = true;
                if (selectedRightBox == null) {
                    playSoundClip(selectClip);
                }
            }
        } else {
            // Aynı sağ kutuya tekrar tıklandıysa seçimi kaldırır
            if (selectedRightBox == clickedBox) {
                selectedRightBox.selected = false;
                selectedRightBox = null;
                playSoundClip(selectClip);
            } else {
                if (selectedRightBox != null) {
                    selectedRightBox.selected = false;
                }
                selectedRightBox = clickedBox;
                selectedRightBox.selected = true;
                if (selectedLeftBox == null) {
                    playSoundClip(selectClip);
                }
            }
        }

        // Hem sol hem de sağ sütundan birer kutu seçildiyse eşleşmeyi değerlendirir
        if (selectedLeftBox != null && selectedRightBox != null) {
            evaluateSelectedPair(selectedLeftBox, selectedRightBox);
            selectedLeftBox = null;
            selectedRightBox = null;
        }
    }

    // Seçilen sol ve sağ kutunun kelime ID'lerini (wordId) karşılaştırır.
    // Eşleşme doğruysa +100 puan verir; yanlışsa wrongCount sayacını 1 artırır ve 10 hatada oyunu bitirir.
    private void evaluateSelectedPair(WordBox left, WordBox right) {
        left.selected = false;
        right.selected = false;

        if (left.wordId == right.wordId) {
            // ==================== DOĞRU EŞLEŞME (+100 PUAN) ====================
            totalScore += MATCH_POINTS;

            left.triggerCorrectMatch();
            right.triggerCorrectMatch();
            playSoundClip(matchClip);

            // Kutuların üzerinde yukarı doğru süzülen yeşil "+100" animasyonu ekler
            floatingTexts.add(new FloatingText(
                    "+" + MATCH_POINTS,
                    left.getCenterX(), left.y + 10,
                    new Color(22, 163, 74)
            ));
            floatingTexts.add(new FloatingText(
                    "+" + MATCH_POINTS,
                    right.getCenterX(), right.y + 10,
                    new Color(22, 163, 74)
            ));

            matchedPairsInRound++;

            // Turdaki 5 çiftin tamamı eşleştirildiğinde bir sonraki tura geçiş sayacını başlatır
            if (matchedPairsInRound >= PAIRS_PER_ROUND) {
                this.roundTransitionTicks = 32;
            }
        } else {
            // ==================== YANLIŞ EŞLEŞME ====================
            wrongCount++;

            left.triggerWrongMatch();
            right.triggerWrongMatch();
            playSoundClip(wrongClip);

            // Yanlış sayısı 10'a ulaştığında oturumu anında sonlandırır ("GAME OVER!")
            if (wrongCount >= MAX_WRONG_COUNT) {
                finishSession(true);
            }
        }
    }

    // Mevcut tur tamamlandığında bir sonraki tura geçer veya 10. tur bittiyse oyunu tamamlar.
    private void advanceToNextRoundOrFinish() {
        if (currentRound < TOTAL_ROUNDS) {
            loadRound(currentRound + 1);
        } else {
            finishSession(false);
        }
    }

    // Oyun oturumunu sonlandırır, elde edilen skoru ve süreyi DataManager üzerinden "records.txt" ile karşılaştırıp kaydeder.
    private void finishSession(boolean failedByMistakes) {
        this.sessionFinished = true;
        this.endedByMistakes = failedByMistakes;
        this.roundTransitionTicks = 0;

        int previousBestScore = dataManager.getHighScore(configKey);
        double previousFastestTime = dataManager.getFastestTime(configKey);

        boolean completedAllRounds = !failedByMistakes;
        this.achievedNewHighScore = (totalScore > previousBestScore && totalScore > 0);
        this.achievedNewFastestTime = completedAllRounds
                && totalSessionSeconds > 0
                && (previousFastestTime < 0 || totalSessionSeconds < previousFastestTime);

        dataManager.updateRecord(configKey, totalScore, totalSessionSeconds, completedAllRounds);

        this.savedBestScore = dataManager.getHighScore(configKey);
        this.savedFastestTime = dataManager.getFastestTime(configKey);
    }

    // =========================================================================
    // 60 FPS ANA OYUN DÖNGÜSÜ (MASTER UPDATE LOOP)
    // =========================================================================

    // gameLoopTimer tarafından her 16 milisaniyede bir (~60 FPS) otomatik çağrılır.
    // Geçen süreyi, kelime kutusu animasyonlarını ve uçan "+100" yazılarını güncelleyip ekranı yeniden çizer.
    private void onFrameTick() {
        long now = System.nanoTime();
        double deltaSeconds = (now - lastTickNanoTime) / 1_000_000_000.0;
        lastTickNanoTime = now;

        if (deltaSeconds < 0 || deltaSeconds > 0.1) {
            deltaSeconds = 0.016;
        }

        updateBoxCoordinates();

        // Oyun devam ediyorsa toplam süreyi artırır veya tur geçiş sayacını ilerletir
        if (!sessionFinished) {
            if (roundTransitionTicks <= 0) {
                totalSessionSeconds += deltaSeconds;
            } else {
                roundTransitionTicks--;
                if (roundTransitionTicks == 0) {
                    advanceToNextRoundOrFinish();
                }
            }
        }

        // Sol ve sağ sütundaki tüm kelime kutularının animasyonlarını günceller
        for (int i = 0; i < leftBoxes.size(); i++) {
            leftBoxes.get(i).updateAnimation();
        }
        for (int i = 0; i < rightBoxes.size(); i++) {
            rightBoxes.get(i).updateAnimation();
        }

        // Uçan "+100" yazılarını günceller; tamamen şeffaflaşanları sondan başa döngü ile ArrayList'ten siler
        for (int i = floatingTexts.size() - 1; i >= 0; i--) {
            FloatingText ft = floatingTexts.get(i);
            ft.update();
            if (ft.alpha <= 0.02f) {
                floatingTexts.remove(i);
            }
        }

        repaint();
    }

    // =========================================================================
    // GRAPHICS2D İLE ÖZEL EKRAN ÇİZİMİ (PAINT COMPONENT)
    // =========================================================================

    // Swing tarafından her karede çağrılan çizim metodu; Graphics2D ve Anti-Aliasing ile pürüzsüz çizim yapar.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // Kenar yumuşatma (Anti-Aliasing) ve yüksek metin kalitesi ayarlarını etkinleştirir
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        // 1. Saf beyaz (#FFFFFF) arka planı çizer
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);

        // 2. Üst bilgi çubuğunu (← Main Menu, Round N, SCORE, Wrong Count ve Total Time) çizer
        drawTopHeader(g2, w);

        // 3. Sol ve sağ sütundaki kelime kutularını (WordBox) çizer
        for (int i = 0; i < leftBoxes.size(); i++) {
            leftBoxes.get(i).draw(g2);
        }
        for (int i = 0; i < rightBoxes.size(); i++) {
            rightBoxes.get(i).draw(g2);
        }

        // 4. Yukarı doğru süzülen "+100" puan yazılarını çizer
        for (int i = 0; i < floatingTexts.size(); i++) {
            floatingTexts.get(i).draw(g2);
        }

        // 5. Oyun sona erdiyse ekranın ortasına sonuç özeti kartını çizer
        if (sessionFinished) {
            drawSessionSummaryOverlay(g2, w, h);
        }

        g2.dispose();
        Toolkit.getDefaultToolkit().sync();
    }

    // Üst bilgi çubuğunu (HUD) çizer:
    // Sol: "← Main Menu" butonu ve "Round N" | Orta: "SCORE: X" ve "Wrong Count: -" | Sağ: "Total Time: 0.0s"
    private void drawTopHeader(Graphics2D g2, int w) {
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, 92);
        g2.setColor(new Color(226, 232, 240));
        g2.drawLine(0, 92, w, 92);

        // Sol Üst: "← Main Menu" Butonu (150x44 px)
        if (hoverBackButton) {
            g2.setColor(new Color(226, 232, 240));
        } else {
            g2.setColor(new Color(241, 245, 249));
        }
        g2.fill(backButtonRect);
        g2.setColor(new Color(203, 213, 225));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(backButtonRect);
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.setColor(new Color(30, 41, 59));
        drawCenteredString(g2, "\u2190 Main Menu", backButtonRect);

        // "Round N" Göstergesi
        String roundText = "Round " + currentRound;
        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2.setColor(new Color(30, 41, 59));
        g2.drawString(roundText, 198, 54);

        // Üst Merkez: Koyu Gri "SCORE: 0" (24pt Bold)
        String scoreTitle = "SCORE: " + totalScore;
        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2.setColor(new Color(51, 65, 85));
        FontMetrics fmScore = g2.getFontMetrics();
        g2.drawString(scoreTitle, (w - fmScore.stringWidth(scoreTitle)) / 2, 42);

        // Skorun Hemen Altı: Açık Gri "Wrong Count: -" (Hata yapıldıkça 1, 2, ... 10 olarak güncellenir)
        String wrongValueText = (wrongCount == 0) ? "-" : String.valueOf(wrongCount);
        String wrongTitle = "Wrong Count: " + wrongValueText;
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.setColor(new Color(148, 163, 184));
        FontMetrics fmWrong = g2.getFontMetrics();
        g2.drawString(wrongTitle, (w - fmWrong.stringWidth(wrongTitle)) / 2, 68);

        // Sağ Üst: Geçen Toplam Süre Kronometresi ("Total Time: 0.0s")
        String totalTimeText = String.format(Locale.US, "Total Time: %.1fs", totalSessionSeconds);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.setColor(new Color(51, 65, 85));
        FontMetrics fmRight = g2.getFontMetrics();
        g2.drawString(totalTimeText, w - fmRight.stringWidth(totalTimeText) - 28, 52);
    }

    // 10 tur tamamlandığında ("COMPLETED!") veya 10 hata yapıldığında ("GAME OVER!") gösterilen özet penceresini çizer.
    private void drawSessionSummaryOverlay(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(15, 23, 42, 170));
        g2.fillRect(0, 0, w, h);

        int cardW = 500;
        int cardH = 340;
        int cardX = (w - cardW) / 2;
        int cardY = (h - cardH) / 2;

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 24, 24);
        g2.setColor(new Color(203, 213, 225));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 24, 24);

        int cx = w / 2;
        int yCursor = cardY + 56;

        // Başlık: 10 hataya ulaşıldıysa kırmızı "GAME OVER!", 10 tur bittiyse "COMPLETED!"
        String headerText = endedByMistakes ? "GAME OVER!" : "COMPLETED!";
        g2.setFont(new Font("SansSerif", Font.BOLD, 30));
        if (endedByMistakes) {
            g2.setColor(new Color(220, 38, 38));
        } else {
            g2.setColor(new Color(15, 23, 42));
        }
        drawHorizontallyCenteredText(g2, headerText, cx, yCursor);

        // Toplam Skor Satırı
        yCursor += 48;
        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2.setColor(new Color(22, 163, 74));
        String scoreLine = "Final Score: " + totalScore + " pts";
        if (achievedNewHighScore) {
            scoreLine += "  (NEW RECORD!)";
        }
        drawHorizontallyCenteredText(g2, scoreLine, cx, yCursor);

        // Toplam Süre Satırı
        yCursor += 38;
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.setColor(new Color(2, 132, 199));
        String timeSuffix = achievedNewFastestTime ? "  (NEW FASTEST!)" : "";
        String timeLine = String.format(Locale.US, "Total Session Time: %.1f sec%s", totalSessionSeconds, timeSuffix);
        drawHorizontallyCenteredText(g2, timeLine, cx, yCursor);

        // Yanlış Sayısı Satırı
        yCursor += 36;
        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        g2.setColor(new Color(100, 116, 139));
        String wrongDisplay = (wrongCount == 0) ? "-" : String.valueOf(wrongCount);
        drawHorizontallyCenteredText(g2, "Wrong Count: " + wrongDisplay, cx, yCursor);

        // Kaydedilmiş Rekorlar (Highest Score & Fastest Time)
        yCursor += 38;
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.setColor(new Color(51, 65, 85));
        String fastestStr = "--";
        if (savedFastestTime > 0) {
            fastestStr = String.format(Locale.US, "%.1f sec", savedFastestTime);
        }
        String bestRecordLine = "Highest Score: " + savedBestScore + " pts   |   Fastest Time: " + fastestStr;
        drawHorizontallyCenteredText(g2, bestRecordLine, cx, yCursor);

        // Alt Butonlar ("Play Again" ve "Back to Main Menu")
        int btnY = cardY + cardH - 68;
        playAgainButtonRect.setRoundRect(cx - 215, btnY, 200, 46, 14, 14);
        mainMenuButtonRect.setRoundRect(cx + 15, btnY, 200, 46, 14, 14);

        // "Play Again" Butonunu Çizer
        if (hoverPlayAgainButton) {
            g2.setColor(new Color(59, 130, 246));
        } else {
            g2.setColor(new Color(37, 99, 235));
        }
        g2.fill(playAgainButtonRect);
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.setColor(Color.WHITE);
        drawCenteredString(g2, "Play Again", playAgainButtonRect);

        // "Back to Main Menu" Butonunu Çizer
        if (hoverMainMenuButton) {
            g2.setColor(new Color(226, 232, 240));
        } else {
            g2.setColor(new Color(241, 245, 249));
        }
        g2.fill(mainMenuButtonRect);
        g2.setColor(new Color(203, 213, 225));
        g2.draw(mainMenuButtonRect);
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.setColor(new Color(30, 41, 59));
        drawCenteredString(g2, "Back to Main Menu", mainMenuButtonRect);
    }

    // Metni verilen X koordinatına göre yatayda ortalayarak çizen yardımcı metot.
    private void drawHorizontallyCenteredText(Graphics2D g2, String text, int centerX, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, centerX - fm.stringWidth(text) / 2, y);
    }

    // Metni verilen RoundRectangle2D dikdörtgeninin hem yatay hem dikey olarak tam ortasına çizen yardımcı metot.
    private void drawCenteredString(Graphics2D g2, String text, RoundRectangle2D rect) {
        FontMetrics fm = g2.getFontMetrics();
        int x = (int) (rect.getX() + (rect.getWidth() - fm.stringWidth(text)) / 2);
        int y = (int) (rect.getY() + ((rect.getHeight() - fm.getHeight()) / 2) + fm.getAscent());
        g2.drawString(text, x, y);
    }

    // =========================================================================
    // DAHİLİ SINIF 1 (INNER CLASS): WordBox
    // Ekrandaki her bir 320x72 px boyutlu etkileşimli kelime kartını ve animasyon durumunu temsil eder.
    // =========================================================================

    private static class WordBox {
        final int wordId;
        final String text;
        final boolean isLeftColumn;

        int x;
        int y;
        int width;
        int height;
        boolean hovered;
        boolean selected;
        boolean fadingOut;
        boolean matched;

        // 60 FPS animasyon değişkenleri (ölçek, şeffaflık ve yanlış eşleşmede sağa-sola titreme)
        float scale = 1.0f;
        float alpha = 1.0f;
        int wrongTicks = 0;
        double shakeOffsetX = 0.0;

        WordBox(int wordId, String text, boolean isLeftColumn) {
            this.wordId = wordId;
            this.text = text;
            this.isLeftColumn = isLeftColumn;
        }

        void setBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        // Fare koordinatlarının (mx, my) bu kelime kutusunun sınırları içinde olup olmadığını kontrol eder.
        boolean contains(int mx, int my) {
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }

        double getCenterX() {
            return x + width / 2.0;
        }

        // Doğru eşleşme olduğunda yeşil parlama ve yavaşça kaybolma (fade-out) animasyonunu başlatır.
        void triggerCorrectMatch() {
            this.fadingOut = true;
            this.hovered = false;
            this.selected = false;
            this.scale = 1.06f;
        }

        // Yanlış eşleşme olduğunda kırmızı yanıp sönme ve yatay titreme (shake) animasyonunu başlatır.
        void triggerWrongMatch() {
            this.wrongTicks = 28;
        }

        // Her 16ms'lik karede kutunun ölçek, şeffaflık ve sarsıntı değerlerini günceller.
        void updateAnimation() {
            if (matched) {
                return;
            }

            if (fadingOut) {
                alpha -= 0.045f;
                scale -= 0.006f;
                if (alpha <= 0.0f) {
                    alpha = 0.0f;
                    matched = true;
                    fadingOut = false;
                }
                return;
            }

            float targetScale = 1.0f;
            if (selected) {
                targetScale = 1.03f;
            } else if (hovered) {
                targetScale = 1.02f;
            }
            scale += (targetScale - scale) * 0.25f;

            if (wrongTicks > 0) {
                wrongTicks--;
                shakeOffsetX = Math.sin(wrongTicks * 0.75) * 11.0;
            } else {
                shakeOffsetX = 0.0;
            }
        }

        // Kelime kutusunu o anki durumuna (Normal, Üzerinde, Seçili, Doğru/Yeşil veya Yanlış/Kırmızı) göre ekrana çizer.
        void draw(Graphics2D g2) {
            if (matched || alpha <= 0.01f) {
                return;
            }

            Graphics2D gBox = (Graphics2D) g2.create();
            if (alpha < 1.0f) {
                gBox.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.0f, alpha)));
            }

            int scaledW = (int) (width * scale);
            int scaledH = (int) (height * scale);
            int drawX = (int) (x - (scaledW - width) / 2.0 + shakeOffsetX);
            int drawY = (int) (y - (scaledH - height) / 2.0);

            Color fillColor;
            Color borderColor;
            Color textColor;
            float strokeWidth = 2.0f;

            if (fadingOut) {
                // Doğru eşleşme: Yeşil kutu
                fillColor = new Color(220, 252, 231);
                borderColor = new Color(34, 197, 94);
                textColor = new Color(22, 101, 52);
                strokeWidth = 3.0f;
            } else if (wrongTicks > 0) {
                // Yanlış eşleşme: Kırmızı kutu
                fillColor = new Color(254, 226, 226);
                borderColor = new Color(239, 68, 68);
                textColor = new Color(153, 27, 27);
                strokeWidth = 3.0f;
            } else if (selected) {
                // Seçili aktif kutu: Mavi çerçeveli kutu
                fillColor = new Color(239, 246, 255);
                borderColor = new Color(59, 130, 246);
                textColor = new Color(30, 58, 138);
                strokeWidth = 2.8f;
            } else if (hovered) {
                // Fare üzerindeyken: Hafif vurgulu kutu
                fillColor = new Color(241, 245, 249);
                borderColor = new Color(148, 163, 184);
                textColor = new Color(15, 23, 42);
                strokeWidth = 2.3f;
            } else {
                // Varsayılan (bekleme) durumu: Açık gri-beyaz kutu (#F8FAFC)
                fillColor = new Color(248, 250, 252);
                borderColor = new Color(203, 213, 225);
                textColor = new Color(30, 41, 59);
            }

            // Yuvarlatılmış kutu gövdesi ve çerçevesini çizer
            gBox.setColor(fillColor);
            gBox.fillRoundRect(drawX, drawY, scaledW, scaledH, 16, 16);

            gBox.setColor(borderColor);
            gBox.setStroke(new BasicStroke(strokeWidth));
            gBox.drawRoundRect(drawX, drawY, scaledW, scaledH, 16, 16);

            // 22pt Bold font ile metni çizer; uzun ifadeler (Phrase) kutuya sığmazsa fontu otomatik küçültür
            int fontSize = 22;
            Font font = new Font("SansSerif", Font.BOLD, fontSize);
            FontMetrics fm = gBox.getFontMetrics(font);
            while (fm.stringWidth(text) > scaledW - 26 && fontSize > 12) {
                fontSize--;
                font = new Font("SansSerif", Font.BOLD, fontSize);
                fm = gBox.getFontMetrics(font);
            }

            gBox.setFont(font);
            gBox.setColor(textColor);
            int textX = drawX + (scaledW - fm.stringWidth(text)) / 2;
            int textY = drawY + ((scaledH - fm.getHeight()) / 2) + fm.getAscent();
            gBox.drawString(text, textX, textY);

            gBox.dispose();
        }
    }

    // =========================================================================
    // DAHİLİ SINIF 2 (INNER CLASS): FloatingText
    // Doğru eşleşme yapıldığında kutunun üzerinden yukarı doğru süzülen "+100" puan yazısını temsil eder.
    // =========================================================================

    private static class FloatingText {
        final String text;
        final double x;
        double y;
        float alpha = 1.0f;
        final Color color;

        FloatingText(String text, double x, double y, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
        }

        void update() {
            y -= 1.35;
            alpha -= 0.018f;
        }

        void draw(Graphics2D g2) {
            if (alpha <= 0.02f) {
                return;
            }
            Graphics2D gText = (Graphics2D) g2.create();
            gText.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.0f, alpha)));
            gText.setFont(new Font("SansSerif", Font.BOLD, 18));
            FontMetrics fm = gText.getFontMetrics();
            int drawX = (int) (x - fm.stringWidth(text) / 2.0);
            int drawY = (int) y;

            gText.setColor(color);
            gText.drawString(text, drawX, drawY);
            gText.dispose();
        }
    }
}
