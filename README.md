# Advanced Multilingual Android Keyboard

Production-oriented Android IME scaffold for English + Bangla with phonetic transliteration, Probhat layout mapping, local AI suggestions, and monetization-ready modular architecture.

## Architecture

```
ime/
  KeyboardService.kt
  KeyboardView.kt
  KeyboardViewModel.kt
languages/
  EnglishEngine.kt
  BanglaPhoneticEngine.kt
  BanglaLayoutEngine.kt
prediction/
  SuggestionEngine.kt
  Trie.kt
  AutoCorrect.kt
data/
  DictionaryDatabase.kt
  WordDao.kt
ui/
  CandidateView.kt
  KeyboardLayout.xml
theme/
  ThemeManager.kt
```

## Key Features Implemented

- English typing engine.
- Bangla phonetic parser (rule-based tokenization, vowel signs, halant insertion, basic reph logic, conjunct handling).
- Bangla fixed layout key mapper (Probhat-style baseline map).
- Trie-backed suggestions with local autocorrect and next-word ranking.
- Room dictionary with user-learning frequency updates.
- Prediction auto-disabled in password input fields.
- InputMethodService + method.xml subtype setup.
- Material 3 day/night compatible theme manager.
- Local dictionary assets for English and Bangla.

## Build Instructions

1. Open project in Android Studio Hedgehog+.
2. Sync Gradle.
3. Build `app` module.
4. Install on Android 10-14 test devices.
5. Enable IME in Settings → System → Languages & input.

## Performance Strategy

- Lazy-load dictionaries into Trie on first use.
- Keep suggestion path in memory and O(prefix) for Trie traversal.
- Avoid main-thread DB work (suspend DAO calls).
- Keep keyboard view lightweight to target <200ms start.

## Privacy Policy Template (Starter)

- No keystrokes are sent to cloud services.
- No password/secure-field text is stored or predicted.
- User-learned words remain local on device.
- Network calls (if enabled later) must be HTTPS-only and opt-in.

## Monetization-Ready Design

- Add premium theme packs via in-app products.
- Add optional AI subscription tier for advanced offline/on-device models.
- Keep core typing free without intrusive ads.

## Play Store Deployment Checklist

- [ ] Fill Data Safety form with local-processing disclosures.
- [ ] Provide privacy policy URL in Play Console.
- [ ] Add instrumentation + macrobenchmark tests.
- [ ] Verify password-field suppression behavior.
- [ ] Validate low-RAM performance and startup latency.
- [ ] Add localized store listing for English/Bangla users.
- [ ] Sign release with Play App Signing.

## Testing Matrix

- Android 10, 11, 12, 13, 14.
- Low RAM device (2-3 GB).
- Bangla heavy typing and mixed-script sentence input.
- Cursor edits mid-word + rapid backspace over conjuncts.
- Multi-line and long copy/paste scenarios.

## Next Milestones

- Gesture typing.
- Voice input with `SpeechRecognizer`.
- TFLite on-device transformer/language model reranker.
- Smart bilingual transliteration fallback.
