# Advanced Multilingual Android Keyboard

Production-ready Android IME supporting English + Bangla (Phonetic & Probhat Layout) with smooth diff-based transliteration, local AI suggestions, autocorrect, and a polished Material 3 UI.

## Features

### Input Methods
- **English**: Standard QWERTY with shift/caps support
- **Bangla Phonetic**: Romanized transliteration with smooth diff-based updates (e.g., "ami" → "আমি")
- **Bangla Probhat Layout**: Full fixed-layout implementation with shift variants and symbols

### Bangla Typing Improvements
- **Smooth Phonetic Typing**: Diff-based updates prevent flickering - only changed characters are updated
- **50+ Conjunct Consonants**: Full support for complex Bangla conjuncts (ক্ষ, জ্ঞ, ঙ্গ, ন্ধ, etc.)
- **Case-Sensitive Input**: Use uppercase for alternate forms (T→ট, t→ত, D→ড, d→দ)
- **Smart Conjunct Handling**: Automatic reph (র্), yantara (্য), and hasanta insertion
- **Complete Probhat Layout**: All 4 rows with shifted variants and symbol mode

### Keyboard UI
- Full QWERTY layout with number row
- Dark theme with Material 3 design
- Visual feedback on key press (ripple effects)
- Dynamic language indicator on spacebar
- Symbol layer (?123) with brackets, currency, and special characters
- Candidate suggestion bar with tap-to-complete

### Smart Features
- Trie-based autocomplete with frequency ranking
- Levenshtein-distance autocorrect (max 2 edits)
- Next-word prediction based on usage patterns
- User dictionary learning (Room database)
- Password field detection (predictions disabled)

### Setup Activity
Built-in MainActivity guides users through:
1. Enabling the keyboard in system settings
2. Selecting it as the active input method

## Architecture

```
ime/
  KeyboardService.kt       # IME service with diff-based transliteration
  KeyboardView.kt          # Custom keyboard UI with key mapping
  KeyboardViewModel.kt     # State management for suggestions
ui/
  MainActivity.kt          # Setup helper activity
  CandidateView.kt         # Suggestion bar implementation
data/
  DictionaryDatabase.kt    # Room database for user dictionary
  WordDao.kt              # Dictionary queries & updates
  WordEntity.kt           # Word model with frequency
languages/
  EnglishEngine.kt        # English normalization
  BanglaPhoneticEngine.kt # Phonetic transliteration with 50+ rules
  BanglaLayoutEngine.kt   # Complete Probhat layout
model/
  KeyAction.kt            # Sealed class for key events
  LanguageMode.kt         # Language state enum
  Suggestion.kt           # Suggestion data class
prediction/
  SuggestionEngine.kt     # Suggestion orchestration
  AutoCorrect.kt          # Fuzzy matching
  Trie.kt                 # Prefix tree for fast lookup
theme/
  ThemeManager.kt         # Day/night theme handling
```

## Screenshots

| English Mode | Bangla Phonetic | Bangla Layout |
|-------------|-----------------|---------------|
| QWERTY with predictions | Romanized input | Probhat mapping |

## Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Android SDK API 24-34

### Build APK

1. **Clone and open in Android Studio**
   ```bash
   git clone <repository-url>
   ```

2. **Sync project** (Android Studio will download dependencies)

3. **Build Debug APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

4. **Or use command line**
   ```bash
   ./gradlew assembleDebug
   ```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

### Installation

1. Enable "Install from Unknown Sources" on your Android device
2. Transfer and install the APK
3. Open the Keyboard app and tap "1. Enable Keyboard"
4. Toggle "Advanced Bangla Keyboard" in system settings
5. Return to the app and tap "2. Select Keyboard"
6. Choose "Advanced Bangla Keyboard" from the input method picker

## Key Mappings

### Bangla Phonetic Rules

#### Basic Vowels
| Input | Output | Input | Output |
|-------|--------|-------|--------|
| a | আ | i | ই |
| u | উ | e | এ |
| o | অ | O | ও |
| ii | ঈ | uu | ঊ |

#### Consonants
| Input | Output | Input | Output |
|-------|--------|-------|--------|
| k | ক | kh | খ |
| g | গ | gh | ঘ |
| c | চ | ch | চ |
| chh | ছ | j | জ |
| jh | ঝ | T | ট |
| t | ত | Th | ঠ |
| th | থ | D | ড |
| d | দ | Dh | ঢ |
| dh | ধ | N | ণ |
| n | ন | p | প |
| ph | ফ | b | ব |
| bh | ভ | m | ম |
| y | য় | r | র |
| l | ল | s | স |
| sh | শ | S | ষ |
| h | হ | y | য |

#### Conjunct Consonants (Joforjo)
| Input | Output | Input | Output |
|-------|--------|-------|--------|
| ksh | ক্ষ | ngk | ঙ্ক |
| ngg | ঙ্গ | ndh | ন্ধ |
| nth | ন্থ | gn | জ্ঞ |
| kk | ক্ক | kt | ক্ত |
| gg | গ্গ | gdh | গ্ধ |
| jj | জ্জ | tt | ত্ত |
| dd | দ্দ | nn | ন্ন |
| nt | ন্ত | nd | ন্দ |
| pp | প্প | pt | প্ত |
| bb | ব্ব | bd | ব্দ |
| mm | ম্ম | mp | ম্প |
| mb | ম্ব | ss | ষ |
| st | স্ত | sk | স্ক |
| sp | স্প | sn | স্ন |
| sm | স্ম | sht | ষ্ট |
| shn | ষ্ণ | ll | ল্ল |
| lk | ল্ক | lp | ল্প |

### Probhat Layout

Full 4-row Probhat layout implementation:

| Row | Keys |
|-----|------|
| Number | ১ ২ ৩ ৪ ৫ ৬ ৭ ৮ ৯ ০ |
| Top | ঙ য ড প ট চ জ হ গ ড় |
| Home | ৃ ূ ি া ্ ব ক ত দ |
| Bottom | ো ে অ ভ ন ম স |

**Shift variants:**
- `e`→`ড`, `E`→`ঢ`
- `t`→`ট`, `T`→`ঠ`
- `d`→`দ`, `D`→`ধ`
- `b`→`ব`, `B`→`ভ`
- `n`→`ন`, `N`→`ণ`
- `m`→`ম`, `M`→`শ`

See `BanglaLayoutEngine.kt` for complete mapping.

## Technical Highlights

### Smooth Phonetic Typing
Unlike traditional transliteration that deletes and retypes the entire word on each keystroke, this implementation uses a **diff-based algorithm**:

- Tracks the last transliteration
- Calculates common prefix between old and new
- Only deletes/appends the changed portion
- Results in buttery-smooth typing without flicker

### Case-Sensitive Input
Use uppercase letters for alternate consonant forms:
- `t` → ত, `T` → ট
- `d` → দ, `D` → ড
- `n` → ন, `N` → ণ
- `s` → স, `S` → ষ

### Smart Conjunct Detection
Automatic insertion of:
- **Reph (র্)**: When `r` starts a syllable before a consonant
- **Yantara (্য)**: When `y` follows certain consonants
- **Hasanta (্)**: Between consecutive consonants

## Privacy Policy

- **No cloud transmission**: All processing happens on-device
- **No password storage**: Predictions disabled in password fields
- **Local learning**: User dictionary never leaves the device
- **No analytics**: No usage statistics collected

## Performance Strategy

- Lazy-load dictionaries into Trie on first use
- O(prefix) Trie traversal for suggestions
- Background coroutines for all DB operations
- Diff-based transliteration (minimal text manipulation)
- Target startup time: <200ms

## Upcoming Features

See [UPGRADE_PLAN.md](UPGRADE_PLAN.md) for detailed roadmap.

- [ ] Gesture/swipe typing
- [ ] Voice input (SpeechRecognizer)
- [ ] TFLite on-device transformer for reranking
- [ ] Smart bilingual detection
- [ ] Additional themes
- [ ] Customizable key sizes

## Testing

Tested on:
- Android 10 (API 29)
- Android 11 (API 30)
- Android 12 (API 31)
- Android 13 (API 33)
- Android 14 (API 34)

Recommended test scenarios:
- Heavy Bangla typing with conjuncts (ক্ষমতা, সংবিধান, সৃষ্টি)
- Mixed English-Bangla sentences
- Rapid backspace over complex characters
- Case-sensitive input (T vs t, D vs d)
- Password fields (should show no predictions)
- Low RAM devices (2-3 GB)

### Test Words for Bangla
Try typing these to verify conjunct support:
- `kshomota` → ক্ষমতা
- `songbidhan` → সংবিধান
- `sristi` → সৃষ্টি
- `utshob` → উৎসব
- `manobota` → মানবতা
- `projonmo` → প্রজন্ম

## License

MIT License - See LICENSE file for details

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Acknowledgments

- Material Design 3 for UI components
- Room Persistence Library for local database
- Kotlin Coroutines for async operations
- Probhat layout standard for Bangla input
