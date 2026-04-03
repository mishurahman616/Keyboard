# Advanced Multilingual Android Keyboard

Production-ready Android IME supporting English + Bangla (Phonetic & Probhat Layout) with local AI suggestions, autocorrect, and a polished Material 3 UI.

## Features

### Input Methods
- **English**: Standard QWERTY with shift/caps support
- **Bangla Phonetic**: Romanized transliteration (e.g., "ami" → "আমি")
- **Bangla Probhat Layout**: Fixed key mappings matching popular Bangla keyboard standards

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
  KeyboardService.kt       # IME service lifecycle & input handling
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
  BanglaPhoneticEngine.kt # Phonetic transliteration
  BanglaLayoutEngine.kt   # Probhat key mappings
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
| Input | Output | Input | Output |
|-------|--------|-------|--------|
| a | অ | k | ক |
| i | ই | kh | খ |
| u | উ | g | গ |
| e | এ | gh | ঘ |
| o | ও | ch | চ |
| ou | ঔ | chh | ছ |

Full rules in `BanglaPhoneticEngine.kt`

### Probhat Layout
| Key | Bangla | Key | Bangla |
|-----|--------|-----|--------|
| g | ্ (halant) | j | ক |
| f | া | d | ি |
| h | ব | k | ত |

See `BanglaLayoutEngine.kt` for complete mapping.

## Privacy Policy

- **No cloud transmission**: All processing happens on-device
- **No password storage**: Predictions disabled in password fields
- **Local learning**: User dictionary never leaves the device
- **No analytics**: No usage statistics collected

## Performance Strategy

- Lazy-load dictionaries into Trie on first use
- O(prefix) Trie traversal for suggestions
- Background coroutines for all DB operations
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
- Heavy Bangla typing with conjuncts
- Mixed English-Bangla sentences
- Rapid backspace over complex characters
- Password fields (should show no predictions)
- Low RAM devices (2-3 GB)

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
