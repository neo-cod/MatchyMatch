# MatchyMatch — UML Class Diagram & Object-Oriented Architecture

This document presents the complete Object-Oriented architecture and UML Class Diagram for **MatchyMatch**, covering all packages (`com.matchymatch`, `com.matchymatch.domain`, `com.matchymatch.service`, `com.matchymatch.persistence`, and `com.matchymatch.ui`), inheritance hierarchies, interface realizations, associations, compositions, and multiplicities.

---

## 1. Rendered UML Class Diagram Image

![MatchyMatch UML Class Diagram](uml_class_diagram.png)

*(PlantUML source is also available at [`docs/uml_class_diagram.puml`](uml_class_diagram.puml)).*

---

## 2. Interactive Mermaid Class Diagram

```mermaid
classDiagram
    direction TB

    class Main {
        -String MENU_CARD$
        -String GAME_CARD$
        -CardLayout cardLayout
        -JPanel mainContainer
        -DataManager dataManager
        -GameController gameController
        -MenuPanel menuPanel
        -GamePanel gamePanel
        +Main()
        +showMainMenu() void
        +startGameSession(String, String, String, String, boolean, boolean) void
        +main(String[] args)$ void
    }

    class VocabularyItem {
        <<abstract>>
        +int DEFAULT_MATCH_POINTS$
        -int id
        -String difficulty
        -String english
        -String turkish
        -String french
        -String italian
        -String hungarian
        -String arabicScript
        -String arabicLatin
        #VocabularyItem(int, String, String, String, String, String, String, String, String)
        +getModeType()* String
        +getBasePoints()* int
        +formatDisplayText(String)* String
        +getTextForLanguage(String, boolean) String
        +isSupportedLanguage(String)$ boolean
        +getLanguageCode(String)$ String
        +getId() int
        +getDifficulty() String
        +getEnglish() String
    }

    class WordItem {
        +WordItem(int, String, String, String, String, String, String, String, String)
        +getModeType() String
        +getBasePoints() int
        +formatDisplayText(String) String
    }

    class PhraseItem {
        +PhraseItem(int, String, String, String, String, String, String, String, String)
        +getModeType() String
        +getBasePoints() int
        +formatDisplayText(String) String
    }

    class SessionRecord {
        -String configKey
        -int highestScore
        -double fastestTimeSeconds
        +SessionRecord(String, int, double)
        +updateWithSessionResult(int, double, boolean) void
        +toFileValueString() String
        +compareTo(SessionRecord) int
        +getConfigKey() String
        +getHighestScore() int
        +getFastestTimeSeconds() double
    }

    class Comparable~SessionRecord~ {
        <<interface>>
        +compareTo(SessionRecord) int
    }

    class GameSession {
        +int TOTAL_ROUNDS$
        +int PAIRS_PER_ROUND$
        +int MAX_WRONG_COUNT$
        -String appLanguage
        -String targetLanguage
        -String mode
        -String difficulty
        -String configKey
        -List~VocabularyItem~ sessionPool
        -List~VocabularyItem~ currentRoundItems
        -int currentRound
        -int matchedPairsInRound
        -int totalScore
        -int wrongCount
        -double totalSessionSeconds
        -boolean sessionFinished
        -boolean endedByMistakes
        +loadRoundItems(int) void
        +recordCorrectMatch(VocabularyItem) void
        +recordWrongMatch() void
        +isCurrentRoundComplete() boolean
        +advanceRoundOrComplete() void
        +addElapsedSeconds(double) void
        +setTotalSessionSeconds(double) void
    }

    class InvalidLanguagePairException {
        +InvalidLanguagePairException(String)
        +InvalidLanguagePairException(String, Throwable)
    }

    class Exception {
        <<Java Standard Library>>
    }

    class LanguageDisplayStrategy {
        <<interface>>
        +resolveText(VocabularyItem, String) String
        +isArabicLatinMode() boolean
        +getStrategyName() String
    }

    class ArabicScriptDisplayStrategy {
        +resolveText(VocabularyItem, String) String
        +isArabicLatinMode() boolean
        +getStrategyName() String
    }

    class ArabicLatinDisplayStrategy {
        +resolveText(VocabularyItem, String) String
        +isArabicLatinMode() boolean
        +getStrategyName() String
    }

    class GameController {
        -DataManager dataManager
        -LanguageDisplayStrategy displayStrategy
        -GameSession activeSession
        +GameController(DataManager)
        +validateLanguagePair(String, String) void
        +configureDisplayStrategy(String, String, boolean) void
        +resolveDisplayText(VocabularyItem, String) String
        +startNewSession(String, String, String, String, boolean) GameSession
        +evaluateMatch(int, int) boolean
        +saveActiveSessionRecord() void
        +getSortedRecords() List~SessionRecord~
        +getSortedVocabulary(String, String) List~VocabularyItem~
    }

    class DataManager {
        -File wordsFile
        -File recordsFile
        -ArrayList~VocabularyItem~ easyWords
        -ArrayList~VocabularyItem~ mediumWords
        -ArrayList~VocabularyItem~ hardWords
        -ArrayList~VocabularyItem~ easyPhrases
        -ArrayList~VocabularyItem~ mediumPhrases
        -ArrayList~VocabularyItem~ hardPhrases
        -HashMap~String, SessionRecord~ recordsMap
        +DataManager()
        +DataManager(File, File)
        +loadWordsFromFile() void
        +isLoadedDataValid() boolean
        +getEntriesForSession(String, String, int) ArrayList~VocabularyItem~
        +getSortedVocabularyItems(String, String) List~VocabularyItem~
        +updateRecord(String, int, double, boolean) void
        +getSortedRecords() List~SessionRecord~
        +buildConfigKey(String, String, String, String)$ String
    }

    class MenuPanel {
        -Main mainFrame
        -DataManager dataManager
        -GameController gameController
        -int appLangIndex
        -int targetLangIndex
        -int modeIndex
        -int difficultyIndex
        -Clip menuClickClip
        +MenuPanel(Main, DataManager, GameController)
        +playMenuClickSound() void
        +closeAudioResources() void
    }

    class GamePanel {
        -Main mainFrame
        -DataManager dataManager
        -GameController gameController
        -ArrayList~VocabularyItem~ sessionWordPool
        -Timer gameLoopTimer
        +GamePanel(Main, DataManager, GameController)
        +startNewSession(String, String, String, String, boolean, boolean) void
        +stopGameLoop() void
        +closeAudioResources() void
    }

    class ConsoleUI {
        -GameController gameController
        -Scanner scanner
        +ConsoleUI(GameController)
        +start() void
    }

    VocabularyItem <|-- WordItem : extends
    VocabularyItem <|-- PhraseItem : extends
    Comparable~SessionRecord~ <|.. SessionRecord : implements
    Exception <|-- InvalidLanguagePairException : extends
    LanguageDisplayStrategy <|.. ArabicScriptDisplayStrategy : implements
    LanguageDisplayStrategy <|.. ArabicLatinDisplayStrategy : implements

    Main "1" *-- "1" DataManager : manages
    Main "1" *-- "1" GameController : coordinates
    Main "1" *-- "1" MenuPanel : displays
    Main "1" *-- "1" GamePanel : displays
    Main ..> ConsoleUI : launches (--console)

    GameController "1" o-- "1" DataManager : uses
    GameController "1" o-- "1" LanguageDisplayStrategy : delegates
    GameController "1" *-- "0..1" GameSession : controls
    GameController ..> InvalidLanguagePairException : throws

    GameSession "1" o-- "5..50" VocabularyItem : contains
    DataManager "1" *-- "180" VocabularyItem : loads & caches
    DataManager "1" *-- "0..*" SessionRecord : persists

    MenuPanel "1" --> "1" GameController : validates
    GamePanel "1" --> "1" GameController : resolves text
    ConsoleUI "1" --> "1" GameController : drives CLI
```

---

## 3. Summary of Key Relationships

| Relationship Type | Source Class | Target Class / Interface | Description |
| :--- | :--- | :--- | :--- |
| **Inheritance (`extends`)** | `WordItem`, `PhraseItem` | `VocabularyItem` (Abstract) | Both concrete vocabulary classes inherit 9 encapsulated language fields and override `getModeType()`, `getBasePoints()`, and `formatDisplayText(String)`. |
| **Inheritance (`extends`)** | `InvalidLanguagePairException` | `java.lang.Exception` | Custom checked exception thrown when the user or caller selects identical or unsupported languages. |
| **Interface Realization (`implements`)** | `ArabicScriptDisplayStrategy`, `ArabicLatinDisplayStrategy` | `LanguageDisplayStrategy` | Strategy pattern implementations for switching between Arabic script (`مرحبا`) and Romanized Latin script (`Marhaba`) at runtime. |
| **Interface Realization (`implements`)** | `SessionRecord` | `Comparable<SessionRecord>` | Natural ordering of personal records by highest score descending, then fastest completion time ascending. |
| **Composition (`*--`)** | `DataManager` | `VocabularyItem` (`1..180`), `SessionRecord` (`0..*`) | `DataManager` owns the 6 categorized `ArrayList<VocabularyItem>` collections and the `HashMap<String, SessionRecord>` map. |
| **Aggregation (`o--`)** | `GameSession` | `VocabularyItem` (`5..50`) | Each 10-round session aggregates 50 polymorphic `VocabularyItem` instances and exposes 5 items per round. |
| **MVC Association (`-->`)** | `MenuPanel`, `GamePanel`, `ConsoleUI` | `GameController` | All three UI views delegate validation, strategy selection, and session management to `GameController`. |
