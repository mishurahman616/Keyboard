# Feature Documentation

Detailed documentation of implemented features and their technical implementation.

## 1. Multilingual Input Support

### 1.1 English Mode
- **File**: `languages/EnglishEngine.kt`
- Standard QWERTY keyboard layout
- Shift key toggles uppercase for next character only
- Auto-reset shift after character input (like mobile keyboards)

### 1.2 Bangla Phonetic Mode
- **File**: `languages/BanglaPhoneticEngine.kt`
- Romanized input transliteration
- Rule-based ordered matching (longer matches first)
- Smart conjunct handling with halant (্) insertion
- Reph initiation detection for 'র' combinations
- Vowel sign attachment after consonants

**Example transliterations:**
```
"amar"     → "আমার"
"bangla"   → "বাংলা"
"kemon"    → "কেমন"
"school"   → "স্কুল"
"ekshathe" → "একসাথে"
```

### 1.3 Bangla Probhat Layout Mode
- **File**: `languages/BanglaLayoutEngine.kt`
- Fixed key mapping following Probhat standard
- Direct character input without transliteration
- Dedicated Bangla numerals (০-৯)

## 2. Keyboard UI Features

### 2.1 Layout Structure
- **File**: `res/layout/keyboard_layout.xml`
- 4-row layout with dedicated number row
- Function keys: Shift, Backspace, Enter, Language, Symbols
- Spacebar displays current language mode
- Material 3 ripple effects on key press

### 2.2 Key Styles
- **File**: `res/values/themes.xml`
- `KeyboardKey`: Standard keys with #FFFFFF text on #333333 background
- `KeyboardKey.Function`: Secondary keys (shift, backspace, etc.)
- `KeyboardKey.Space`: Extended spacebar with language label

### 2.3 Visual Assets
- `bg_key.xml`: Standard key background with ripple
- `bg_key_function.xml`: Function key background
- `bg_keyboard.xml`: Keyboard container background (#1E1E1E)

### 2.4 Symbols Mode
- **File**: `ime/KeyboardView.kt` (setSymbolsMode)
- Secondary layout accessed via ?123 key
- Special characters: brackets, currency, math symbols
- ABC key returns to letter layout

## 3. Suggestion System

### 3.1 Trie-Based Autocomplete
- **File**: `prediction/Trie.kt`
- Prefix tree data structure
- O(m) lookup where m = prefix length
- Frequency-based ranking for word popularity
- Returns top 8 matches by default

### 3.2 Autocorrect
- **File**: `prediction/AutoCorrect.kt`
- Levenshtein distance algorithm
- Maximum edit distance: 2
- Only triggers for words 3+ characters
- Searches candidates from first character prefix

### 3.3 Next-Word Prediction
- **File**: `prediction/SuggestionEngine.kt`
- Uses Room database with `nextWordHint` tracking
- Suggests words that commonly follow previous word
- Limited to top 3 candidates for diversity

### 3.4 Suggestion UI
- **File**: `ui/CandidateView.kt`
- Horizontal scrollable bar above keyboard
- Click to replace current word
- Disabled in password fields
- Debounced updates (50ms) during typing

## 4. User Dictionary Learning

### 4.1 Database Schema
- **File**: `data/WordEntity.kt`
```kotlin
@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val word: String,
    val language: String,
    val frequency: Int = 1,
    val nextWordHint: String? = null
)
```

### 4.2 Learning Triggers
- Word committed on: Space, Enter, or candidate selection
- Minimum word length: 2 characters
- Frequency incremented in Room database
- Trie updated immediately for current session

### 4.3 DAO Operations
- `incrementFrequency()`: Atomic frequency update
- `findByPrefix()`: Suggestion lookup
- `getNextWordCandidates()`: Next-word prediction
- `upsert()`: Batch dictionary import

## 5. Setup Activity

### 5.1 MainActivity
- **File**: `ui/MainActivity.kt`
- Launcher activity with app icon
- Two-step setup process:
  1. Opens system IME settings
  2. Shows input method picker

### 5.2 Benefits
- Users can easily find and enable the keyboard
- No need to manually navigate system settings
- Clear instructions in native UI

## 6. Security Features

### 6.1 Password Field Detection
- Checks `InputType.TYPE_TEXT_VARIATION_PASSWORD`
- Checks `InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD`
- Checks `InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`
- Disables suggestions and autocorrect
- No text stored or learned from password fields

### 6.2 Privacy Guarantees
- No network calls for typing data
- No keystroke logging
- All ML/prediction runs on-device
- No cloud dictionary sync

## 7. Performance Optimizations

### 7.1 Coroutine Usage
- All database operations use `Dispatchers.IO`
- Suggestion requests debounced (50ms)
- Lifecycle-aware coroutine scopes
- No blocking on main thread

### 7.2 Memory Management
- Single `SuggestionEngine` instance
- Trie shared across suggestions
- View recycling in CandidateView
- Lazy database initialization

### 7.3 Startup Time
- Theme applied before view inflation
- Database initialized on first suggestion request
- Minimal work in `onCreateInputView()`

## 8. Theme System

### 8.1 Day/Night Support
- **File**: `theme/ThemeManager.kt`
- Respects system theme setting
- Manual override via SharedPreferences
- Material 3 dynamic theming support

### 8.2 Color Scheme
```xml
Standard keys:     #333333 background, #FFFFFF text
Function keys:     #444444 background
Keyboard bg:       #1E1E1E
Candidate bar:     #121212
Ripple effect:     #4DFFFFFF (30% white)
```

## 9. State Management

### 9.1 KeyboardService State
```kotlin
languageMode: LanguageMode    // Current input language
isShifted: Boolean            // Shift active for next char
currentWord: StringBuilder     // Word being typed
previousWord: String?          // Last committed word
```

### 9.2 KeyboardView State
```kotlin
isShifted: Boolean            // Visual shift state
isSymbolsMode: Boolean        // Symbols layout active
currentLanguage: LanguageMode // For label updates
keyButtons: Map<Int, Button>  // Cached key references
```

### 9.3 Language Cycling
```
English → Bangla Phonetic → Bangla Layout → English
```
- Triggered by 🌐 key
- Spacebar label reflects current mode
- Clears current word on switch

## 10. Event Flow

### 10.1 Character Input
1. User taps key → `KeyboardView.dispatchKey()`
2. `KeyAction.Character` sent to `KeyboardService`
3. Character translated based on `languageMode`
4. Previous text deleted (for phonetic updates)
5. Translated text committed via `InputConnection`
6. Current word updated, suggestions requested

### 10.2 Candidate Selection
1. User taps suggestion in `CandidateView`
2. `onCandidateClickListener` invoked
3. `commitCandidate()` deletes typed text
4. Selected word committed to input field
5. Word learned (frequency updated)
6. Previous word updated for next-word prediction

### 10.3 Backspace Flow
1. `KeyAction.Backspace` dispatched
2. Last character removed from `currentWord`
3. `InputConnection.deleteSurroundingText(1, 0)`
4. Suggestions refreshed if word remains

## Configuration

### Supported Android Versions
- Minimum: API 24 (Android 7.0)
- Target: API 34 (Android 14)
- Tested: API 29-34

### Permissions Required
```xml
<uses-permission android:name="android.permission.INTERNET" />      <!-- For future voice input -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />  <!-- For future voice input -->
```

### IME Subtypes Declared
- English (en_US)
- Bangla Phonetic (bn_BD)
- Bangla Probhat (bn_BD)

## Future Enhancements

See [UPGRADE_PLAN.md](UPGRADE_PLAN.md) for:
- Gesture typing implementation
- TFLite integration
- Voice input architecture
- Performance benchmarking
