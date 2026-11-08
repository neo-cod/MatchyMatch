# MatchyMatch — User Stories, Acceptance Criteria, Design Decisions & Known Limitations

This document outlines the functional user stories, verifiable acceptance criteria, core object-oriented design decisions, and known technical limitations of the **MatchyMatch** application.

---

## 1. User Stories & Acceptance Criteria

### User Story 1: Multi-Language Pair Selection & Validation
> **As a** language learner,  
> **I want to** select my known Application Language and a different Target Language to learn from 6 supported languages (English, Turkish, French, Italian, Hungarian, and Arabic),  
> **so that** I can practice vocabulary translations between any two distinct languages.

**Acceptance Criteria:**
- The user can select any of the 6 supported languages (`English`, `Turkish`, `French`, `Italian`, `Hungarian`, `Arabic`) as the Application Language and Target Language.
- Clicking the `LANGUAGE` cycle box on the Main Menu automatically skips the currently selected Application Language so the two languages never collide in the GUI.
- At the controller layer (`GameController.validateLanguagePair`), attempting to start a session with identical or unsupported languages throws the custom checked exception [`InvalidLanguagePairException`](../src/main/java/com/matchymatch/domain/InvalidLanguagePairException.java).

---

### User Story 2: Mode (`Word` vs. `Phrase`) & Difficulty (`Easy`, `Medium`, `Hard`) Customization
> **As a** student at a specific proficiency level,  
> **I want to** switch between single `Word` mode and conversational `Phrase` mode across `Easy`, `Medium`, and `Hard` difficulty tiers,  
> **so that** the matching cards match my current learning stage.

**Acceptance Criteria:**
- Clicking the `MODE` box cycles between `Word` (represented by [`WordItem`](../src/main/java/com/matchymatch/domain/WordItem.java)) and `Phrase` (represented by [`PhraseItem`](../src/main/java/com/matchymatch/domain/PhraseItem.java)).
- Clicking the `DIFFICULTY` box cycles through `Easy`, `Medium`, and `Hard`.
- [`DataManager`](../src/main/java/com/matchymatch/persistence/DataManager.java) loads 180 curated entries from `words.csv` into 6 dedicated polymorphic `ArrayList<VocabularyItem>` lists (35 items per Word difficulty and 25 items per Phrase difficulty) and supplies 50 randomized pairs (10 rounds × 5 pairs) per session.

---

### User Story 3: Dynamic Arabic Alphabet Display Rule (Strategy Pattern)
> **As a** learner studying Arabic,  
> **I want to** choose whether Arabic items are displayed in native **Arabic Script (`مرحبا`)** or **Romanized Latin Alphabet (`Marhaba`)**,  
> **so that** I can practice either reading authentic Arabic script or learning pronunciation through transliteration.

**Acceptance Criteria:**
- On the Main Menu (and in Console UI settings), the user can select between `Arabic Script (مرحبا)` and `Latin Alphabet (Marhaba)`.
- [`GameController`](../src/main/java/com/matchymatch/service/GameController.java) configures the active [`LanguageDisplayStrategy`](../src/main/java/com/matchymatch/service/LanguageDisplayStrategy.java) at runtime using either [`ArabicScriptDisplayStrategy`](../src/main/java/com/matchymatch/service/ArabicScriptDisplayStrategy.java) or [`ArabicLatinDisplayStrategy`](../src/main/java/com/matchymatch/service/ArabicLatinDisplayStrategy.java).
- If neither the Application Language nor the Target Language is Arabic, non-Arabic languages always display their standard text regardless of the Arabic radio toggle.

---

### User Story 4: Interactive 10-Round Matching Gameplay with `+100` Scoring & 10-Mistake Game Over
> **As a** player,  
> **I want to** match 5 pairs per round across 10 rounds, earning `+100` points per correct match while having a strict limit of 10 wrong attempts per session,  
> **so that** gameplay is rewarding, clear, and challenging.

**Acceptance Criteria:**
- Each session consists of **10 Rounds** with **5 Pairs per Round** (left column = Application Language, right column = shuffled Target Language).
- Every correct match awards a flat **`+100` points** (`VocabularyItem.getBasePoints()`), plays an ascending confirmation tone, and triggers a green fade-out animation with floating `+100` text in the Swing GUI.
- Every mismatched pair increments `Wrong Count` by `1`, plays a low error tone, and shakes the cards in red.
- Reaching **10 wrong attempts (`Wrong Count == 10`)** immediately terminates the session with a **`GAME OVER!`** summary screen.

---

### User Story 5: Persistent Personal Records & Leaderboards (`records.txt`)
> **As a** returning player,  
> **I want** my Highest Score and Fastest Completion Time for each language, mode, and difficulty configuration to be saved automatically to disk and sortable,  
> **so that** I can track my personal progress across sessions.

**Acceptance Criteria:**
- When a session ends, [`DataManager`](../src/main/java/com/matchymatch/persistence/DataManager.java) updates the [`SessionRecord`](../src/main/java/com/matchymatch/domain/SessionRecord.java) in its `HashMap<String, SessionRecord>` using a normalized key (e.g., `EN-TR-WORD-EASY`) and writes all records to `records.txt` using `try-with-resources` and `BufferedWriter`.
- `Fastest Time` is recorded or updated only when all 10 rounds are completed (`completedAllRounds == true`).
- [`SessionRecord`](../src/main/java/com/matchymatch/domain/SessionRecord.java) implements `Comparable<SessionRecord>`, allowing records to be sorted by highest score descending and fastest completion time ascending.

---

### User Story 6: Dual User Interface Support (60 FPS Swing GUI & Interactive Console CLI)
> **As a** university student demonstrating the project in class or grading via terminal,  
> **I want to** run the full graphical Swing interface by default or launch an interactive Console UI via `--console`,  
> **so that** both visual gameplay and command-line inspection of sorted records and vocabulary lists are supported.

**Acceptance Criteria:**
- Running `mvn exec:java` (or running [`Main`](../src/main/java/com/matchymatch/Main.java) without arguments) opens the `1200x820` Swing desktop window (`MenuPanel` and `GamePanel` managed by `CardLayout`).
- Running `mvn exec:java -Dexec.args="--console"` launches [`ConsoleUI`](../src/main/java/com/matchymatch/ui/ConsoleUI.java), offering a numbered terminal menu to play rounds, configure settings, view `Comparable`-sorted records, and browse `Comparator`-sorted vocabulary items.

---

## 2. Main Design Decisions

1. **Abstract Base Class (`VocabularyItem`) with Concrete Subclasses (`WordItem` & `PhraseItem`):**
   - Both single words and multi-word conversational phrases share the same 6-language translation attributes (`id`, `difficulty`, `english`, `turkish`, `french`, `italian`, `hungarian`, `arabicScript`, `arabicLatin`), which are strictly encapsulated as `private final` fields in [`VocabularyItem`](../src/main/java/com/matchymatch/domain/VocabularyItem.java) with constructor validation.
   - Making `VocabularyItem` abstract and overriding `getModeType()`, `getBasePoints()`, and `formatDisplayText(String)` in `WordItem` and `PhraseItem` enables polymorphic storage in `ArrayList<VocabularyItem>` and clean extensibility (for example, adding a `SentenceItem` or `IdiomItem` in the future without changing `GameSession` or `DataManager`).

2. **Strategy Design Pattern (`LanguageDisplayStrategy`) for the Arabic Alphabet Rule:**
   - Instead of scattering conditional `if (useArabicLatin)` checks across UI rendering code, text resolution is encapsulated behind the [`LanguageDisplayStrategy`](../src/main/java/com/matchymatch/service/LanguageDisplayStrategy.java) interface with two concrete strategies: [`ArabicScriptDisplayStrategy`](../src/main/java/com/matchymatch/service/ArabicScriptDisplayStrategy.java) and [`ArabicLatinDisplayStrategy`](../src/main/java/com/matchymatch/service/ArabicLatinDisplayStrategy.java).
   - [`GameController`](../src/main/java/com/matchymatch/service/GameController.java) selects and swaps the strategy at runtime based on user selection, adhering to the **Open/Closed Principle (OCP)**.

3. **Model-View-Controller (MVC) Architectural Separation:**
   - **Model & Persistence (`com.matchymatch.domain`, `com.matchymatch.persistence`):** Holds domain entities (`VocabularyItem`, `WordItem`, `PhraseItem`, `GameSession`, `SessionRecord`) and file I/O (`DataManager`) with zero dependency on Swing or console I/O.
   - **Controller (`com.matchymatch.service.GameController`):** Coordinates language-pair validation (`InvalidLanguagePairException`), display strategy configuration, match evaluation, and sorting.
   - **View (`com.matchymatch.ui`):** Both the Swing views (`MenuPanel`, `GamePanel`) and the terminal view (`ConsoleUI`) share the exact same `GameController` and `DataManager` backend.

4. **Procedural PCM Audio Synthesis (`javax.sound.sampled.Clip`):**
   - To guarantee zero external audio file dependencies while providing responsive click, match, and mismatch sound effects, short sine-wave PCM buffers are generated mathematically at startup and preloaded into reusable `Clip` instances.

---

## 3. Known Limitations

1. **Single-Player Local File Persistence (`records.txt`):**
   - Personal records are stored locally in a flat key-value file (`records.txt`) rather than a remote multi-user database. If multiple users play on the same machine, they share the same local high-score table.
2. **Fixed 6-Language CSV Schema (`words.csv`):**
   - The starter dataset supports 6 languages (`English`, `Turkish`, `French`, `Italian`, `Hungarian`, `Arabic`) across 180 entries. Adding a 7th language requires extending the CSV header and `VocabularyItem` fields.
3. **Synthesized Sound Effects Only:**
   - In-game audio uses procedurally generated 8-bit PCM sine waves rather than external `.wav` or `.mp3` voice recordings, keeping the repository lightweight and self-contained.
