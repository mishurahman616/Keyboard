# Keyboard App Review and Upgrade Plan

## Snapshot Review

### Strengths
- Clear modular package structure (`ime`, `languages`, `prediction`, `data`, `ui`, `theme`).
- Good baseline multilingual intent (English, Bangla phonetic, Bangla layout).
- Privacy-aware password suppression logic exists in both IME and suggestion layers.
- Room + Trie combination is a strong foundation for on-device suggestions.

### Gaps Identified
1. **Prediction pipeline is not wired into UI events yet**
   - `KeyboardService` defines `suggestionEngine()` but never invokes it.
   - `KeyboardViewModel` exists but is not instantiated/connected to the service.
   - `CandidateView` renders tokens, but there is no suggestion click handling to commit text.

2. **Input behavior is incomplete for production typing**
   - `onKeyPressed()` only commits literal text; no handling for backspace, enter, shift state, caps lock, symbols, or cursor movement.
   - No visible language toggle input flow between English/Bangla modes.

3. **Dictionary bootstrap/learning lifecycle is partial**
   - Asset dictionaries exist, but there is no explicit loader into Trie/DB at startup or first-run migration path.
   - Learning path exists (`learnWord`) but is not called from commit events.

4. **UX/accessibility hardening missing**
   - `CandidateView` is basic text chips without theme integration, touch feedback, accessibility labels, or truncation/overflow strategy.
   - Keyboard key rendering, long-press alternatives, and haptic/audio feedback are not yet implemented.

5. **Quality gates are light**
   - JUnit dependency is present but there are no unit/instrumentation/benchmark tests.
   - Existing README checklist calls out tests/benchmarking as pending.

---

## Upgrade Plan

## Phase 1 (High Priority, 1-2 sprints): Make Core Typing Production-Ready

### 1. Wire prediction end-to-end
- Instantiate a single `SuggestionEngine` in `KeyboardService` and reuse it.
- Track current token + previous token in service state.
- On each character commit / delete, request `suggest()` (debounced) and update `CandidateView.render()`.
- Add suggestion tap callback in `CandidateView` to commit selected candidate and update learning frequency.

### 2. Complete key action handling
- Extend keyboard dispatch to special key codes (`DELETE`, `ENTER`, `SPACE`, `SHIFT`, `LANG`, `SYMBOLS`).
- Implement deletion-aware token recomputation.
- Add language-mode toggle interaction and visible state feedback.

### 3. Dictionary bootstrap
- Add startup dictionary loader from assets into Trie.
- Seed Room DB once (idempotent marker in shared prefs).
- Move all IO and DB bootstrap work to coroutine background dispatcher.

### 4. Basic regression tests
- Unit tests for:
  - `BanglaPhoneticEngine` transliteration cases.
  - `SuggestionEngine` merge/rank logic and password suppression.
  - `AutoCorrect` and Trie prefix search behavior.

**Exit criteria**
- Suggestions appear while typing.
- Candidate tap replaces/commits expected token.
- Password fields show no suggestions.
- Unit tests run green in CI/local.

---

## Phase 2 (Medium Priority, 1 sprint): Polish UX + Reliability

### 1. Candidate row and key UX improvements
- Themed suggestion pills with selected/pressed states.
- Accessibility labels and minimum touch target sizing.
- Haptic/audio feedback toggle with sane defaults.

### 2. Better language experience
- Improve Bangla conjunct and reph edge cases with golden tests.
- Add mixed-script heuristics (keep acronyms/URLs in English).
- Persist last-used language mode per input context when appropriate.

### 3. Telemetry-free local diagnostics
- Add in-app debug panel (build-type gated) for latency and suggestion timings.
- Add structured logs for IME lifecycle + crash triage without collecting user text.

**Exit criteria**
- No major UX blockers in multi-day dogfooding.
- Acceptable suggestion latency on low-end hardware.

---

## Phase 3 (Strategic, 2+ sprints): Scale Features and Release Readiness

### 1. Performance and startup optimization
- Cache frequently used trie branches and top next-word pairs.
- Reduce cold-start work in IME process; lazy-init non-critical modules.
- Add Macrobenchmark scenarios for cold/warm start and keypress latency.

### 2. Release engineering hardening
- Add CI checks for lint, unit tests, instrumentation smoke tests.
- Expand Data Safety/Privacy docs in repo with release checklist ownership.
- Add crash-safe migration policy for Room schema upgrades.

### 3. Optional advanced capabilities
- Gesture typing prototype.
- On-device reranker (TFLite) behind feature flag.
- Downloadable language packs with local-only processing guarantees.

**Exit criteria**
- Play Store internal testing build is stable across Android 10-14.
- Measured startup + typing latency targets met.

---

## Suggested Immediate Backlog (first 10 tickets)
1. Create `KeyAction` sealed model and switch IME dispatch to structured events.
2. Wire `KeyboardService` -> `SuggestionEngine` -> `CandidateView` update loop.
3. Add `CandidateView` click callback API and commit selected suggestion.
4. Implement dictionary asset loader + one-time DB seed.
5. Add token tracker utility (current/previous token parsing from input connection text).
6. Add backspace-aware suggestion refresh logic.
7. Add language toggle key and state persistence.
8. Add unit tests for SuggestionEngine merge/sort/filter behavior.
9. Add transliteration golden tests for Bangla phonetic edge cases.
10. Add GitHub Actions workflow for `./gradlew test lint`.
