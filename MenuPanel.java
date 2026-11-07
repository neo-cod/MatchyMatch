import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;

// MenuPanel sınıfı: Oyunun minimalist Ana Menü ekranını (Swing JPanel) oluşturur.
// 1. Sağ üst köşede havuz mavisi (#38BDF8) dairesel Bilgi ('i') ve geometrik Ayarlar (dişli çark) butonları yer alır.
// 2. Üst merkezde "matchy-logo.png" dosyası Graphics2D BICUBIC interpolasyon ile yüksek kalitede (~500x360 px) gösterilir.
// 3. Ortada tıklayarak geçiş yapılan 3 adet (210x95 px) seçim kutusu (LANGUAGE, MODE, DIFFICULTY) ve Arapça alfabe seçimi bulunur.
// 4. Alt merkezde yüksek kontrastlı "Start" (300x75 px) butonu yer alır.
// 5. Menü tıklama sesi için başlangıçta bir kez oluşturulan yeniden kullanılabilir (reusable) Clip nesnesi kullanılır.
public class MenuPanel extends JPanel {

    // Desteklenen 6 dil, 2 oyun modu ve 3 zorluk seviyesi sabit dizileri
    private static final String[] LANGUAGES = {
        "English", "Turkish", "French", "Italian", "Hungarian", "Arabic"
    };

    private static final String[] MODES = {
        "Word", "Phrase"
    };

    private static final String[] DIFFICULTIES = {
        "Easy", "Medium", "Hard"
    };

    // Sağ üstteki Bilgi ('i') ve Ayarlar (dişli) ikonları için havuz mavisi renk sabiti (#38BDF8)
    private static final Color POOL_BLUE = new Color(56, 189, 248);

    // Ana pencere (Main) ve veri yöneticisi (DataManager) referansları
    private final Main mainFrame;
    private final DataManager dataManager;

    // Ayarlar (Settings) penceresinden kontrol edilen durum değişkenleri
    private int appLangIndex = 0;         // Varsayılan Uygulama Dili (Bilinen Dil): "English"
    private boolean sfxEnabled = true;    // Ses efektleri (SFX) açık/kapalı durumu
    private boolean musicEnabled = false; // Oyun müziği açık/kapalı durumu

    // Ana ekrandaki 3 tıklanabilir kutunun (LANGUAGE, MODE, DIFFICULTY) aktif indeksleri
    private int targetLangIndex = 1;      // Varsayılan Öğrenilecek Hedef Dil: "Turkish"
    private int modeIndex = 0;            // Varsayılan Mod: "Word"
    private int difficultyIndex = 0;      // Varsayılan Zorluk: "Easy"

    // 3 tıklanabilir seçim kutusu bileşenleri
    private CycleBoxPanel languageBox;
    private CycleBoxPanel modeBox;
    private CycleBoxPanel difficultyBox;

    // Arapça Alfabe Kuralı (Arap Alfabesi veya Latin Okunuşu) radyo butonları
    private JRadioButton arabicScriptRadio;
    private JRadioButton arabicLatinRadio;

    // Çok sayıda tıklamada işletim sistemi ses kanallarının dolmasını önlemek için tek sefer yüklenen Clip nesnesi
    private final Clip menuClickClip;

    // Kurucu metot (Constructor): Ana menü yerleşimini (BorderLayout) ve tüm görsel bölümleri oluşturur.
    public MenuPanel(Main mainFrame, DataManager dataManager) {
        this.mainFrame = mainFrame;
        this.dataManager = dataManager;

        // Tıklama sesini başlangıçta belleğe (RAM) bir kez yükler
        this.menuClickClip = createPreloadedClickClip();

        setLayout(new BorderLayout(0, 6));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(12, 36, 22, 36));

        // 1. ÜST BÖLÜM: Sağ üst ikonlar ('i' ve Ayarlar) + Ortalanmış büyük logo
        add(createTopLogoSection(), BorderLayout.NORTH);

        // 2. ORTA BÖLÜM: 3 tıklanabilir kutu (LANGUAGE, MODE, DIFFICULTY) + Arapça Alfabe Kuralı
        add(createCenterInteractiveSection(), BorderLayout.CENTER);

        // 3. ALT BÖLÜM: "Start" butonu
        add(createBottomStartSection(), BorderLayout.SOUTH);
    }

    // Dış ses dosyasına ihtiyaç duymadan matematiksel sinüs dalgası (PCM) ile 25 ms'lik temiz bir tıklama sesi üretir.
    // javax.sound.sampled.Clip nesnesi bellekte açık tutularak her tıklamada başa sarılıp tekrar çalınır.
    private Clip createPreloadedClickClip() {
        try {
            float sampleRate = 22050f;
            int durationMs = 25;
            int totalSamples = (int) (durationMs * (sampleRate / 1000f));
            byte[] buffer = new byte[totalSamples];

            for (int i = 0; i < totalSamples; i++) {
                double progress = (double) i / totalSamples;
                double freq = 850.0 - (430.0 * progress);
                double angle = (i / sampleRate) * freq * 2.0 * Math.PI;
                double envelope = Math.pow(1.0 - progress, 1.8);
                buffer[i] = (byte) (Math.sin(angle) * 58.0 * envelope);
            }

            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
            Clip clip = AudioSystem.getClip();
            clip.open(format, buffer, 0, buffer.length);
            return clip;
        } catch (Exception ignored) {
            return null;
        }
    }

    // Bellekte hazır bekleyen tıklama sesini sıfırıncı kareye (frame 0) sararak anında çalar.
    public void playMenuClickSound() {
        if (!sfxEnabled || menuClickClip == null) {
            return;
        }
        try {
            if (menuClickClip.isRunning()) {
                menuClickClip.stop();
            }
            menuClickClip.setFramePosition(0);
            menuClickClip.start();
        } catch (Exception ignored) {
        }
    }

    // Uygulama kapatılırken açık olan ses kanalını işletim sistemine iade eder.
    public void closeAudioResources() {
        if (menuClickClip != null) {
            try {
                menuClickClip.stop();
                menuClickClip.close();
            } catch (Exception ignored) {
            }
        }
    }

    // Üst bölümü oluşturur: Sağ üstte Bilgi ('i') ve Ayarlar butonları, ortada ise yüksek kaliteli oyun logosu yer alır.
    private JPanel createTopLogoSection() {
        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setOpaque(false);

        // Sağ üst köşedeki dairesel ikon butonları için FlowLayout paneli
        JPanel topRightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        topRightBar.setOpaque(false);

        JButton infoButton = createCircularInfoButton("Game Rules & Info");
        JButton settingsButton = createCircularSettingsButton("Settings & Personal Records");

        infoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playMenuClickSound();
                showInfoDialog();
            }
        });

        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playMenuClickSound();
                showSettingsDialog();
            }
        });

        topRightBar.add(infoButton);
        topRightBar.add(settingsButton);
        topWrapper.add(topRightBar, BorderLayout.NORTH);

        // Ortalanmış büyük logo kapsayıcısı
        JPanel logoContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logoContainer.setOpaque(false);
        logoContainer.add(createHighQualityLogoLabel());
        topWrapper.add(logoContainer, BorderLayout.CENTER);

        return topWrapper;
    }

    // "matchy-logo.png" dosyasını proje klasöründen (veya Masaüstünden) okur, kenar boşluklarını kırpar
    // ve Graphics2D BICUBIC interpolasyon ile ~500x360 px boyutunda net bir şekilde ölçeklendirir.
    private JLabel createHighQualityLogoLabel() {
        String userHome = System.getProperty("user.home");
        File[] candidateFiles = new File[]{
            new File("./matchy-logo.png"),
            new File("matchy-logo.png"),
            new File(userHome + "/Desktop/matchy-logo.png"),
            new File(userHome + "/OneDrive/Desktop/matchy-logo.png")
        };

        int containerW = 500;
        int containerH = 355;

        for (int i = 0; i < candidateFiles.length; i++) {
            File file = candidateFiles[i];
            if (file.exists() && file.isFile()) {
                try {
                    BufferedImage rawImage = ImageIO.read(file);
                    if (rawImage != null) {
                        BufferedImage trimmed = trimExcessWhitespace(rawImage);

                        // En-boy oranını (aspect ratio) koruyarak ölçek katsayısını hesaplar
                        double scale = Math.min(
                                (double) containerW / trimmed.getWidth(),
                                (double) containerH / trimmed.getHeight()
                        );
                        int targetW = Math.max(1, (int) Math.round(trimmed.getWidth() * scale));
                        int targetH = Math.max(1, (int) Math.round(trimmed.getHeight() * scale));

                        BufferedImage highQualityScaled = scaleImageBicubic(trimmed, targetW, targetH);
                        JLabel label = new JLabel(new ImageIcon(highQualityScaled), SwingConstants.CENTER);
                        label.setPreferredSize(new Dimension(containerW, containerH));
                        return label;
                    }
                } catch (Exception ignored) {
                    // Okuma hatası olursa sıradaki dosya yolunu dener
                }
            }
        }

        // Logo dosyası hiçbir konumda bulunamazsa yedek olarak metin başlık gösterir
        JLabel fallback = new JLabel("MATCHY MATCH", SwingConstants.CENTER);
        fallback.setFont(new Font("SansSerif", Font.BOLD, 42));
        fallback.setForeground(new Color(30, 41, 59));
        fallback.setPreferredSize(new Dimension(containerW, containerH));
        return fallback;
    }

    // Görüntü kalitesini korumak için Graphics2D ve BICUBIC interpolasyon kullanarak aşamalı ölçeklendirme yapar.
    private BufferedImage scaleImageBicubic(BufferedImage source, int targetW, int targetH) {
        BufferedImage current = source;
        int curW = source.getWidth();
        int curH = source.getHeight();

        // Görüntü yarıdan fazla küçültülüyorsa pikselleşmeyi önlemek için adım adım küçültür
        while (curW / 2 >= targetW && curH / 2 >= targetH) {
            curW /= 2;
            curH /= 2;
            BufferedImage step = new BufferedImage(curW, curH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = step.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(current, 0, 0, curW, curH, null);
            g2.dispose();
            current = step;
        }

        BufferedImage result = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(current, 0, 0, targetW, targetH, null);
        g2.dispose();
        return result;
    }

    // Logo resminin etrafındaki gereksiz beyaz/şeffaf boşlukları tespit edip kırpar.
    private BufferedImage trimExcessWhitespace(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int minX = w;
        int minY = h;
        int maxX = 0;
        int maxY = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                if (alpha > 15 && (r < 248 || g < 248 || b < 248)) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (minX >= maxX || minY >= maxY) {
            return img;
        }

        int pad = 10;
        minX = Math.max(0, minX - pad);
        minY = Math.max(0, minY - pad);
        maxX = Math.min(w - 1, maxX + pad);
        maxY = Math.min(h - 1, maxY + pad);

        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    // Orta bölümü oluşturur: 3 adet (210x95 px) tıklanabilir seçim kutusu ve altında ortalanmış Arapça Alfabe Kuralı.
    private JPanel createCenterInteractiveSection() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // --- 1. Satır: 3 Tıklanabilir Seçim Kutusu (LANGUAGE, MODE, DIFFICULTY) ---
        JPanel boxesRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 38, 4));
        boxesRow.setOpaque(false);

        // Sol Kutu: LANGUAGE (Öğrenilecek Hedef Dil)
        languageBox = new CycleBoxPanel(
                "LANGUAGE",
                LANGUAGES[targetLangIndex],
                new Color(239, 246, 255),
                new Color(59, 130, 246),
                new Color(30, 58, 138),
                new Runnable() {
                    @Override
                    public void run() {
                        cycleTargetLanguage();
                    }
                }
        );

        // Orta Kutu: MODE (Word / Phrase)
        modeBox = new CycleBoxPanel(
                "MODE",
                MODES[modeIndex],
                new Color(254, 249, 195),
                new Color(234, 179, 8),
                new Color(113, 63, 18),
                new Runnable() {
                    @Override
                    public void run() {
                        cycleMode();
                    }
                }
        );

        // Sağ Kutu: DIFFICULTY (Easy / Medium / Hard)
        difficultyBox = new CycleBoxPanel(
                "DIFFICULTY",
                DIFFICULTIES[difficultyIndex],
                new Color(240, 253, 244),
                new Color(34, 197, 94),
                new Color(20, 83, 45),
                new Runnable() {
                    @Override
                    public void run() {
                        cycleDifficulty();
                    }
                }
        );

        boxesRow.add(languageBox);
        boxesRow.add(modeBox);
        boxesRow.add(difficultyBox);

        // --- 2. Satır: Ortalanmış Arapça Alfabe Kuralı Radyo Butonları ---
        JPanel optionsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        optionsRow.setOpaque(false);

        JPanel arabicGroupPanel = new JPanel();
        arabicGroupPanel.setLayout(new BoxLayout(arabicGroupPanel, BoxLayout.Y_AXIS));
        arabicGroupPanel.setOpaque(false);

        JLabel arabicTitle = new JLabel("Arabic Alphabet Rule");
        arabicTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        arabicTitle.setForeground(new Color(30, 41, 59));
        arabicTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel radioRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        radioRow.setOpaque(false);

        arabicScriptRadio = new JRadioButton("Arabic Script (مرحبا)", true);
        arabicLatinRadio = new JRadioButton("Latin Alphabet (Marhaba)", false);
        Font radioFont = new Font("SansSerif", Font.PLAIN, 15);
        arabicScriptRadio.setFont(radioFont);
        arabicLatinRadio.setFont(radioFont);
        arabicScriptRadio.setForeground(new Color(51, 65, 85));
        arabicLatinRadio.setForeground(new Color(51, 65, 85));
        arabicScriptRadio.setOpaque(false);
        arabicLatinRadio.setOpaque(false);
        arabicScriptRadio.setFocusPainted(false);
        arabicLatinRadio.setFocusPainted(false);
        arabicScriptRadio.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        arabicLatinRadio.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ActionListener radioSoundListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playMenuClickSound();
            }
        };
        arabicScriptRadio.addActionListener(radioSoundListener);
        arabicLatinRadio.addActionListener(radioSoundListener);

        // ButtonGroup: İki radyo butonundan aynı anda yalnızca birinin seçilebilmesini sağlar
        ButtonGroup arabicBtnGroup = new ButtonGroup();
        arabicBtnGroup.add(arabicScriptRadio);
        arabicBtnGroup.add(arabicLatinRadio);

        radioRow.add(arabicScriptRadio);
        radioRow.add(arabicLatinRadio);

        arabicGroupPanel.add(arabicTitle);
        arabicGroupPanel.add(Box.createVerticalStrut(5));
        arabicGroupPanel.add(radioRow);

        optionsRow.add(arabicGroupPanel);

        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(boxesRow);
        centerPanel.add(Box.createVerticalStrut(14));
        centerPanel.add(optionsRow);
        centerPanel.add(Box.createVerticalGlue());

        return centerPanel;
    }

    // LANGUAGE kutusuna tıklandığında bir sonraki dile geçer; Uygulama Dili (appLangIndex) ile aynı dili otomatik atlar.
    private void cycleTargetLanguage() {
        playMenuClickSound();
        do {
            targetLangIndex = (targetLangIndex + 1) % LANGUAGES.length;
        } while (targetLangIndex == appLangIndex);
        languageBox.setValueText(LANGUAGES[targetLangIndex]);
    }

    // MODE kutusuna tıklandığında "Word" ve "Phrase" modları arasında geçiş yapar.
    private void cycleMode() {
        playMenuClickSound();
        modeIndex = (modeIndex + 1) % MODES.length;
        modeBox.setValueText(MODES[modeIndex]);
    }

    // DIFFICULTY kutusuna tıklandığında "Easy", "Medium" ve "Hard" zorluk seviyeleri arasında geçiş yapar.
    private void cycleDifficulty() {
        playMenuClickSound();
        difficultyIndex = (difficultyIndex + 1) % DIFFICULTIES.length;
        difficultyBox.setValueText(DIFFICULTIES[difficultyIndex]);
    }

    // Alt bölümü oluşturur: Oyunu başlatan büyük (300x75 px, 28pt Bold) mavi "Start" butonu.
    private JPanel createBottomStartSection() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        bottomPanel.setOpaque(false);

        JButton startButton = new JButton("Start") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(new Color(29, 78, 216)); // Tıklanırken koyu mavi
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(59, 130, 246)); // Fare üzerindeyken parlak mavi
                } else {
                    g2.setColor(getBackground()); // Normal durumda canlı mavi (#2563EB)
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        startButton.setFont(new Font("SansSerif", Font.BOLD, 28));
        startButton.setBackground(new Color(37, 99, 235));
        startButton.setForeground(Color.WHITE);
        startButton.setOpaque(true);
        startButton.setContentAreaFilled(false);
        startButton.setBorderPainted(false);
        startButton.setFocusPainted(false);
        startButton.setPreferredSize(new Dimension(300, 75));
        startButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playMenuClickSound();
                handleStartSession();
            }
        });

        bottomPanel.add(startButton);
        return bottomPanel;
    }

    // Sağ üst köşedeki havuz mavisi (#38BDF8) dairesel Bilgi ('i') butonunu Graphics2D ile özel olarak çizer.
    private JButton createCircularInfoButton(String tooltip) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight()) - 4;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // Fare üzerine geldiğinde açık mavi arka plan dolgusu
                if (getModel().isRollover()) {
                    g2.setColor(new Color(224, 242, 254));
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fillOval(x, y, size, size);

                // Havuz mavisi dairesel dış çerçeve
                g2.setColor(POOL_BLUE);
                g2.setStroke(new BasicStroke(2.6f));
                g2.drawOval(x, y, size, size);

                // Dairenin tam ortasına klasik serif fontuyla 'i' harfini çizer
                g2.setFont(new Font("Georgia", Font.BOLD | Font.ITALIC, 23));
                FontMetrics fm = g2.getFontMetrics();
                String text = "i";
                int tx = (getWidth() - fm.stringWidth(text)) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() - 1;
                g2.drawString(text, tx, ty);

                g2.dispose();
            }
        };
        btn.setToolTipText(tooltip);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFocusable(false);
        btn.setPreferredSize(new Dimension(42, 42));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // Sağ üst köşedeki havuz mavisi (#38BDF8) dairesel Ayarlar (8 dişli çark) butonunu geometrik olarak tam merkezde çizer.
    private JButton createCircularSettingsButton(String tooltip) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight()) - 4;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                Color bgColor = Color.WHITE;
                if (getModel().isRollover()) {
                    bgColor = new Color(224, 242, 254);
                }
                g2.setColor(bgColor);
                g2.fillOval(x, y, size, size);

                // Havuz mavisi dairesel dış çerçeve
                g2.setColor(POOL_BLUE);
                g2.setStroke(new BasicStroke(2.6f));
                g2.drawOval(x, y, size, size);

                // Butonun tam merkez koordinatları (cx, cy) etrafında 45'er derece açıyla 8 diş çizer
                double cx = getWidth() / 2.0;
                double cy = getHeight() / 2.0;

                AffineTransform oldTransform = g2.getTransform();
                for (int i = 0; i < 8; i++) {
                    AffineTransform toothTransform = new AffineTransform(oldTransform);
                    toothTransform.translate(cx, cy);
                    toothTransform.rotate(i * (Math.PI / 4.0));
                    g2.setTransform(toothTransform);
                    g2.fill(new RoundRectangle2D.Double(-2.1, -10.4, 4.2, 4.2, 1.5, 1.5));
                }
                g2.setTransform(oldTransform);

                // Dişli çarkın gövde halkası ve ortasındaki boşluk
                double bodyRadius = 7.6;
                g2.fill(new Ellipse2D.Double(cx - bodyRadius, cy - bodyRadius, bodyRadius * 2, bodyRadius * 2));

                double holeRadius = 3.5;
                g2.setColor(bgColor);
                g2.fill(new Ellipse2D.Double(cx - holeRadius, cy - holeRadius, holeRadius * 2, holeRadius * 2));

                g2.dispose();
            }
        };
        btn.setToolTipText(tooltip);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFocusable(false);
        btn.setPreferredSize(new Dimension(42, 42));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // Bilgi ('i') butonuna tıklandığında oyun kurallarını (10 Tur, +100 Puan, 10 Hata Kuralı) gösteren pencereyi açar.
    private void showInfoDialog() {
        String infoHtml = "<html><body style='width: 370px; font-family: SansSerif; font-size: 11pt; color: #1E293B;'>"
                + "<h2 style='margin-top:0; color:#0284C7;'>How to Play MatchyMatch</h2>"
                + "<p><b>• Session & Round Structure:</b><br>"
                + "Each session consists of <b>10 Rounds</b> with <b>5 Pairs per Round</b> "
                + "(App Language on the left, Target Language on the right). "
                + "Advance to the next round by matching all 5 pairs.</p>"
                + "<p><b>• Scoring:</b><br>"
                + "Every correct match awards a flat <b>+100 points</b>.</p>"
                + "<p><b>• Wrong Count Rule:</b><br>"
                + "Each mismatched pair increases your <b>Wrong Count</b> by 1. "
                + "Reaching <b>10 wrong attempts</b> ends the session immediately and returns you to the main menu!</p>"
                + "</body></html>";

        JOptionPane.showMessageDialog(
                this,
                infoHtml,
                "Game Rules & Info",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // Ayarlar (Settings) butonuna tıklandığında açılan diyalog penceresi:
    // Uygulama Dili (App Language), Ses Efektleri (SFX), Müzik ve en altta Kişisel Rekorları (Highest Score & Fastest Time) gösterir.
    private void showSettingsDialog() {
        final JDialog dialog = new JDialog(mainFrame, "Settings", true);
        dialog.setLayout(new BorderLayout(12, 12));
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(20, 26, 12, 26));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        Font labelFont = new Font("SansSerif", Font.BOLD, 14);
        Font controlFont = new Font("SansSerif", Font.PLAIN, 14);

        // 1. Satır: Uygulama Dili (App Language - Bilinen Dil)
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel appLangLabel = new JLabel("App Language (Known):");
        appLangLabel.setFont(labelFont);
        content.add(appLangLabel, gbc);

        gbc.gridx = 1;
        final JComboBox<String> appLangCombo = new JComboBox<>(LANGUAGES);
        appLangCombo.setFont(controlFont);
        appLangCombo.setSelectedIndex(appLangIndex);
        content.add(appLangCombo, gbc);

        // 2. Satır: Ses Efektleri (In-Game Sound Effects - SFX)
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel sfxLabel = new JLabel("In-Game Sound Effects (SFX):");
        sfxLabel.setFont(labelFont);
        content.add(sfxLabel, gbc);

        gbc.gridx = 1;
        final JCheckBox sfxCheck = new JCheckBox("Enabled", sfxEnabled);
        sfxCheck.setFont(controlFont);
        sfxCheck.setOpaque(false);
        content.add(sfxCheck, gbc);

        // 3. Satır: Oyun Müziği (Game Audio / Music)
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel musicLabel = new JLabel("Game Audio / Music:");
        musicLabel.setFont(labelFont);
        content.add(musicLabel, gbc);

        gbc.gridx = 1;
        final JCheckBox musicCheck = new JCheckBox("Enabled", musicEnabled);
        musicCheck.setFont(controlFont);
        musicCheck.setOpaque(false);
        content.add(musicCheck, gbc);

        // 4. Satır: Ayarlar penceresinin en altında Kişisel Rekorlar (Highest Score & Fastest Time) alanı
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 8, 4, 8);

        JPanel recordsPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        recordsPanel.setBackground(new Color(248, 250, 252));
        recordsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        final JLabel settingsHighScoreLabel = new JLabel("Highest Score: --", SwingConstants.CENTER);
        settingsHighScoreLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        settingsHighScoreLabel.setForeground(new Color(22, 101, 52));

        final JLabel settingsFastestTimeLabel = new JLabel("Fastest Time: --", SwingConstants.CENTER);
        settingsFastestTimeLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        settingsFastestTimeLabel.setForeground(new Color(30, 64, 175));

        // İlk açılışta ve dil değiştirildiğinde rekor etiketlerini yardımcı metot ile günceller
        updateSettingsRecordsLabels(appLangCombo.getSelectedIndex(), settingsHighScoreLabel, settingsFastestTimeLabel);

        appLangCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSettingsRecordsLabels(appLangCombo.getSelectedIndex(), settingsHighScoreLabel, settingsFastestTimeLabel);
            }
        });

        recordsPanel.add(settingsHighScoreLabel);
        recordsPanel.add(settingsFastestTimeLabel);
        content.add(recordsPanel, gbc);

        dialog.add(content, BorderLayout.CENTER);

        // Alt Kısım: "Save & Close" (Kaydet ve Kapat) Butonu
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        footer.setBackground(new Color(248, 250, 252));

        JButton saveBtn = new JButton("Save & Close") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(59, 130, 246));
                } else {
                    g2.setColor(new Color(37, 99, 235));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        saveBtn.setBackground(new Color(37, 99, 235));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setOpaque(true);
        saveBtn.setContentAreaFilled(false);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setPreferredSize(new Dimension(135, 36));
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        saveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                appLangIndex = appLangCombo.getSelectedIndex();
                sfxEnabled = sfxCheck.isSelected();
                musicEnabled = musicCheck.isSelected();
                playMenuClickSound();

                // Eğer seçilen Uygulama Dili ile ana ekrandaki Hedef Dil aynı olduysa, Hedef Dili otomatik değiştirir
                if (targetLangIndex == appLangIndex) {
                    targetLangIndex = (appLangIndex + 1) % LANGUAGES.length;
                    languageBox.setValueText(LANGUAGES[targetLangIndex]);
                }

                dialog.dispose();
            }
        });

        footer.add(saveBtn);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // Yardımcı metot: Ayarlar penceresindeki Highest Score ve Fastest Time etiketlerini seçili kombinasyona göre günceller.
    private void updateSettingsRecordsLabels(int previewAppIdx, JLabel highScoreLabel, JLabel fastestTimeLabel) {
        int previewTargetIdx = targetLangIndex;
        if (previewTargetIdx == previewAppIdx) {
            previewTargetIdx = (previewAppIdx + 1) % LANGUAGES.length;
        }
        String configKey = DataManager.buildConfigKey(
                LANGUAGES[previewAppIdx],
                LANGUAGES[previewTargetIdx],
                MODES[modeIndex],
                DIFFICULTIES[difficultyIndex]
        );
        int hs = dataManager.getHighScore(configKey);
        double ft = dataManager.getFastestTime(configKey);

        if (hs > 0) {
            highScoreLabel.setText("Highest Score: " + hs + " pts");
        } else {
            highScoreLabel.setText("Highest Score: --");
        }

        if (ft > 0) {
            fastestTimeLabel.setText(String.format(Locale.US, "Fastest Time: %.1f sec", ft));
        } else {
            fastestTimeLabel.setText("Fastest Time: --");
        }
    }

    // "Start" butonuna basıldığında seçilen dil, mod, zorluk ve Arapça alfabe ayarlarıyla 10 turluk oyunu başlatır.
    private void handleStartSession() {
        String appLang = LANGUAGES[appLangIndex];
        String targetLang = LANGUAGES[targetLangIndex];
        String mode = MODES[modeIndex];
        String difficulty = DIFFICULTIES[difficultyIndex];

        // Arapça Alfabe Kuralı yalnızca dillerden biri Arapça olduğunda etkinleşir
        boolean isArabicInvolved = "ARABIC".equals(appLang.toUpperCase(Locale.ROOT))
                || "ARABIC".equals(targetLang.toUpperCase(Locale.ROOT));
        boolean useArabicLatin = isArabicInvolved && arabicLatinRadio.isSelected();

        mainFrame.startGameSession(appLang, targetLang, mode, difficulty, useArabicLatin, sfxEnabled);
    }

    // =========================================================================
    // DAHİLİ SINIF (INNER CLASS): CycleBoxPanel
    // Ana menüdeki 210x95 px boyutlu, tıkladıkça değeri değişen özel çizimli (Graphics2D) seçim kutusu.
    // =========================================================================

    private static class CycleBoxPanel extends JPanel {

        private String valueText;
        private final Color boxFillColor;
        private final Color boxBorderColor;
        private final Color textColor;
        private boolean hovered = false;
        private boolean pressed = false;
        private final JPanel cardRectPanel;

        CycleBoxPanel(String titleText, String initialValue,
                      Color boxFillColor, Color boxBorderColor, Color textColor,
                      final Runnable onCycleAction) {
            this.valueText = initialValue;
            this.boxFillColor = boxFillColor;
            this.boxBorderColor = boxBorderColor;
            this.textColor = textColor;

            setLayout(new BorderLayout(0, 10));
            setOpaque(false);

            // Kutunun üstündeki 17pt Bold büyük harfli kategori başlığı (LANGUAGE, MODE, DIFFICULTY)
            JLabel titleLabel = new JLabel(titleText, SwingConstants.CENTER);
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
            titleLabel.setForeground(new Color(15, 23, 42));
            add(titleLabel, BorderLayout.NORTH);

            // Graphics2D ile özel çizilen 210x95 px dikdörtgen kutu
            cardRectPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth();
                    int h = getHeight();
                    int inset = pressed ? 3 : 2;

                    // Yuvarlatılmış kutu arka planını doldurur
                    if (hovered) {
                        g2.setColor(brighter(CycleBoxPanel.this.boxFillColor, 0.96f));
                    } else {
                        g2.setColor(CycleBoxPanel.this.boxFillColor);
                    }
                    g2.fillRoundRect(inset, inset, w - inset * 2, h - inset * 2, 18, 18);

                    // Kutu kenarlığını çizer
                    g2.setColor(CycleBoxPanel.this.boxBorderColor);
                    g2.setStroke(new BasicStroke(hovered ? 3.0f : 2.4f));
                    g2.drawRoundRect(inset, inset, w - inset * 2 - 1, h - inset * 2 - 1, 18, 18);

                    // Seçili değeri 22pt Bold font ile kutunun tam ortasına yazar
                    g2.setFont(new Font("SansSerif", Font.BOLD, 22));
                    g2.setColor(CycleBoxPanel.this.textColor);
                    FontMetrics fm = g2.getFontMetrics();
                    int textX = (w - fm.stringWidth(CycleBoxPanel.this.valueText)) / 2;
                    int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(CycleBoxPanel.this.valueText, textX, textY);

                    g2.dispose();
                }
            };

            cardRectPanel.setOpaque(false);
            cardRectPanel.setPreferredSize(new Dimension(210, 95));
            cardRectPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Fare etkileşimlerini (üzerine gelme ve tıklama) dinler
            cardRectPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    cardRectPanel.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    pressed = false;
                    cardRectPanel.repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        pressed = true;
                        cardRectPanel.repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        boolean wasPressed = pressed;
                        pressed = false;
                        if (wasPressed && cardRectPanel.contains(e.getPoint())) {
                            onCycleAction.run();
                        }
                        cardRectPanel.repaint();
                    }
                }
            });

            add(cardRectPanel, BorderLayout.CENTER);
        }

        // Kutu içindeki metni günceller ve ekranı yeniden çizer
        void setValueText(String newValue) {
            this.valueText = newValue;
            cardRectPanel.repaint();
        }

        // Fare kutunun üzerine geldiğinde rengi hafifçe tonlamak için kullanılan yardımcı metot
        private static Color brighter(Color c, float factor) {
            int r = Math.min(255, Math.max(0, (int) (c.getRed() * factor)));
            int g = Math.min(255, Math.max(0, (int) (c.getGreen() * factor)));
            int b = Math.min(255, Math.max(0, (int) (c.getBlue() * factor)));
            return new Color(r, g, b);
        }
    }
}
