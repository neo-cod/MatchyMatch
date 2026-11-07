# MatchyMatch — Multi-Language Vocabulary Matching Game

**MatchyMatch** is a self-contained desktop vocabulary and phrase matching game built in **pure Java (Swing, AWT, and `Graphics2D`)** with zero external library dependencies. Designed with clean Object-Oriented Programming (OOP) principles, Java Collections (`ArrayList`, `HashMap`), File I/O (`BufferedReader`, `BufferedWriter`), and a smooth 60 FPS custom-rendered gameplay canvas, it compiles and runs out-of-the-box in **IntelliJ IDEA** or from the terminal.

---

## Key Features

* **6 Supported Languages:** Practice vocabulary across **English**, **Turkish**, **French**, **Italian**, **Hungarian**, and **Arabic**.
  * Choose your known **App Language** inside the **Settings** dialog (top-right gear button) and cycle through your **Target Language to Learn** directly on the Main Menu.
* **Dual Arabic Alphabet Rule:** Whenever **Arabic** is selected (as either the App Language or Target Language), choose between two display modes on the Main Menu:
  * **Arabic Script:** Native Arabic script (e.g., `مرحبا`, `شكرا لك`).
  * **Latin Alphabet:** Romanized phonetic transcription (e.g., `Marhaba`, `Shukran lak`).
* **Word & Phrase Modes Across 3 Difficulty Levels:**
  * **Modes:** `Word` (single vocabulary words) and `Phrase` (everyday expressions, questions, and idioms).
  * **Difficulties:** `Easy`, `Medium`, and `Hard` — backed by 6 categorized `ArrayList<WordEntry>` collections (`easyWords`, `mediumWords`, `hardWords`, `easyPhrases`, `mediumPhrases`, `hardPhrases`) loaded from `words.csv`.
* **Session & Gameplay Rules:**
  * **10 Rounds per Session** with **5 Pairs per Round** (App Language on the left column, shuffled Target Language on the right column).
  * **Flat `+100 Points` Scoring:** Every correct match awards `+100 points` with a smooth green highlight and rising `+100` animation.
  * **10 Wrong Count Game-Over Rule:** Each mismatched pair increments `Wrong Count` by `1` (`Wrong Count: -` -> `1` -> ... -> `10`). Reaching **10 wrong attempts** immediately ends the session (`GAME OVER!`).
* **Persistent Personal Records (`records.txt`):**
  * Automatically tracks and saves your **Highest Score** and **Fastest Time** per language pair, mode, and difficulty configuration using `HashMap<String, String>` and `BufferedReader` / `BufferedWriter`.
* **Synthesized Sound Effects (`javax.sound.sampled.Clip`):**
  * Uses pre-synthesized, reusable PCM audio clips in memory for menu clicks, card selection, correct matches, and wrong attempts—requiring no external `.wav` files.

---

## Project Architecture (5-Class Breakdown)

| File | Responsibility & Core Java Concepts Used |
| :--- | :--- |
| **`Main.java`** | Application entry point (`main` method) and top-level `JFrame` window (`MatchyMatch -- Matching Game`). Uses a `CardLayout` to switch seamlessly between `MenuPanel` and `GamePanel`, runs startup diagnostics, and releases audio resources on window close. |
| **`WordEntry.java`** | Encapsulated OOP data model representing a single row from `words.csv` (`id`, `difficulty`, `type`, and translations for all 6 languages including `arabicScript` and `arabicLatin`). Uses `private final` fields and public getter methods. |
| **`DataManager.java`** | Handles all File I/O (`BufferedReader`, `BufferedWriter`, `File`) and in-memory collections. Auto-generates/validates `words.csv`, categorizes entries into 6 `ArrayList<WordEntry>` lists (`easyWords`, `mediumWords`, `hardWords`, `easyPhrases`, `mediumPhrases`, `hardPhrases`), and persists personal records (`records.txt`) via a `HashMap<String, String>`. |
| **`MenuPanel.java`** | Minimalist Swing Main Menu featuring high-quality `Graphics2D` bicubic scaling for `matchy-logo.png`, custom-painted pool-blue circular **Info (`i`)** and **Settings (gear)** buttons, 3 interactive click-to-cycle selection boxes (`LANGUAGE`, `MODE`, `DIFFICULTY`), the **Arabic Alphabet Rule** radio buttons, and the **Start** button. |
| **`GamePanel.java`** | Custom-rendered 60 FPS gameplay screen powered by a single `javax.swing.Timer` (16 ms tick) and `Graphics2D` with anti-aliasing and double buffering. Manages the 10-round session loop, `WordBox` animations, `+100` floating score indicators, the 10-mistake `Wrong Count` limit, and the end-of-session summary overlay. |

---

## How to Run

### Option 1: Run in IntelliJ IDEA (Recommended)
1. Open **IntelliJ IDEA** (JDK 17 or newer).
2. Click **File -> Open...** and select the `MatchyMatch` project folder.
3. Ensure the project root is marked as the Sources Root (or open `Main.java` directly).
4. Open `Main.java` and click the green **Run (`▶`)** button next to `public static void main(String[] args)`.

### Option 2: Compile & Run from Terminal
Open a terminal in the project root directory (`MatchyMatch`) and run:

```bash
javac -encoding UTF-8 *.java
java Main
```
