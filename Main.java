import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// Main sınıfı: Uygulamanın başlangıç noktası (entry point) ve ana pencere (JFrame) yöneticisidir.
// CardLayout kullanarak Ana Menü (MenuPanel) ve Oyun Ekranı (GamePanel) arasında tek pencerede geçiş sağlar.
public class Main extends JFrame {

    // CardLayout içinde ekranları tanımlamak için kullanılan sabit anahtar isimler
    private static final String MENU_CARD = "MENU";
    private static final String GAME_CARD = "GAME";

    // Ekran geçişlerini yöneten CardLayout ve ana kapsayıcı panel (JPanel)
    private final CardLayout cardLayout;
    private final JPanel mainContainer;

    // Veri yöneticisi (File I/O işlemleri) ve uygulamanın iki ana ekran paneli
    private final DataManager dataManager;
    private final MenuPanel menuPanel;
    private final GamePanel gamePanel;

    // Kurucu metot (Constructor): Ana pencereyi oluşturur, veri yönetimini başlatır ve panelleri hazırlar.
    public Main() {
        // JFrame pencere başlığını ayarlar
        super("MatchyMatch -- Matching Game");

        // DataManager nesnesini oluşturur (words.csv dosyasını doğrular/yükler ve records.txt verilerini okur)
        this.dataManager = new DataManager();

        // Başlangıçta tüm dil, mod ve zorluk kombinasyonlarının veri bütünlüğünü otomatik kontrol eder
        DataManager.runDiagnostics(this.dataManager);

        // Pencere boyutunu, minimum boyutunu ve kapatma davranışını ayarlar
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(1200, 820));
        setMinimumSize(new Dimension(1060, 760));
        setLocationRelativeTo(null); // Pencereyi ekranın tam ortasında açar

        // Ekranlar arası geçiş için CardLayout ve kapsayıcı JPanel oluşturur
        this.cardLayout = new CardLayout();
        this.mainContainer = new JPanel(cardLayout);

        // Ana menü ve oyun ekranı panellerini oluşturup CardLayout kapsayıcısına ekler
        this.menuPanel = new MenuPanel(this, dataManager);
        this.gamePanel = new GamePanel(this, dataManager);

        mainContainer.add(menuPanel, MENU_CARD);
        mainContainer.add(gamePanel, GAME_CARD);

        // Pencere kapatıldığında açık olan ses kaynaklarını (Clip) ve oyun zamanlayıcısını (Timer) güvenli şekilde kapatır
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                menuPanel.closeAudioResources();
                gamePanel.closeAudioResources();
            }
        });

        setContentPane(mainContainer);
        showMainMenu();
    }

    // Aktif ekranı Ana Menü (MenuPanel) olarak değiştirir.
    public final void showMainMenu() {
        cardLayout.show(mainContainer, MENU_CARD);
    }

    // Aktif ekranı Oyun Ekranı (GamePanel) olarak değiştirir ve seçilen ayarlarla yeni bir 10 turluk oyun başlatır.
    public void startGameSession(String appLang, String targetLang, String mode, String difficulty,
                                 boolean useArabicLatin, boolean sfxEnabled) {
        cardLayout.show(mainContainer, GAME_CARD);
        gamePanel.startNewSession(appLang, targetLang, mode, difficulty, useArabicLatin, sfxEnabled);
    }

    // Standart main metodu: Swing arayüzünü güvenli çalışma parçacığı (Event Dispatch Thread - EDT) üzerinde başlatır.
    public static void main(String[] args) {
        try {
            // İşletim sisteminin doğal pencere görünümünü (System Look and Feel) uygular
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Sistem görünümü yüklenemezse varsayılan Swing görünümü ile devam eder
        }

        // Swing bileşenlerinin thread-safe çalışması için pencereyi invokeLater içinde başlatır
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Main app = new Main();
                app.setVisible(true);
            }
        });
    }
}
