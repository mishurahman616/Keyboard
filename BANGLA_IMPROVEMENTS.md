# Bangla Typing Improvements

## Current Issues Analysis

### 1. Phonetic Re-transliteration Problem
**File**: `KeyboardService.kt:150-156`

The current implementation deletes the entire word on every keystroke and re-commits it. This causes visual flicker and poor performance.

**Current Flow**:
```
User types "a" → commits "অ"
User types "m" → deletes "অ", commits "আম" 
User types "a" → deletes "আম", commits "আমা"
User types "r" → deletes "আমা", commits "আমার"
```

**Improved Flow** (Diff-based):
```
User types "a" → commits "অ"
User types "m" → deletes "অ", commits "আম" (only 2 chars changed)
User types "a" → deletes "ম", commits "মা" (only 1 char changed)
User types "r" → commits "র" (only append)
```

### 2. Missing Phonetic Rules

#### Common Conjuncts Missing:
| Input | Output | Status |
|-------|--------|--------|
| ngm | ঙ্ম | Missing |
| ngg | ঙ্গ | Missing |
| nd | ন্দ | Missing |
| ndh | ন্ধ | Missing |
| nt | ন্ত | Missing |
| nth | ন্থ | Missing |
| nn | ন্ন | Missing |
| bd | ব্দ | Missing |
| bdh | ব্ধ | Missing |
| mp | ম্প | Missing |
| mb | ম্ব | Missing |
| mm | ম্ম | Missing |
| y | য্ (antara) | Partial |

#### Vowel + Consonant Combinations:
Current implementation doesn't handle:
- `ri`/`ree` → রি (as in ক্রি)
- `rri` → ঋ (rri-kar)
- `ng` ending → ং (anusvara) should bind to previous vowel

### 3. Probhat Layout Issues

**Current Problems**:
- Only 26 characters mapped
- Missing double-key combinations for compound letters
- No visual feedback for shifted/alternate characters
- Spacebar shows "বাংলা (Layout)" but keys show Latin letters

**Probhat Standard Layout**:
```
Row 1:  ং  ঃ   র্   জ্ঞ   ত্‍   ষ   ঔ   ঐ   ঊ   ঊ
        [  ]    {   }     #   %   ^   *   +   =
Row 2:  ঢ়   ৃ    ি     ্     ব    ক    ত    দ    চ    ছ
        _   \   |    ~     <   >   $   €   £   •
Row 3:  ও   ে   া     ি     ূ     ব      ভ      ন      ম     স
        :   "    '     ;      /      ?       ,       ।       ল      য়
```

## Recommended Improvements

### Phase 1: Fix Phonetic Typing Smoothness

#### A. Implement Diff-Based Updates
```kotlin
// In KeyboardService.kt
private fun commitPhoneticUpdate(oldTransliteration: String, newTransliteration: String) {
    val commonPrefix = oldTransliteration.commonPrefixWith(newTransliteration)
    val deleteCount = oldTransliteration.length - commonPrefix.length
    val newSuffix = newTransliteration.substring(commonPrefix.length)
    
    if (deleteCount > 0) {
        currentInputConnection.deleteSurroundingText(deleteCount, 0)
    }
    if (newSuffix.isNotEmpty()) {
        currentInputConnection.commitText(newSuffix, 1)
    }
}
```

#### B. Expand Phonetic Rules
```kotlin
// Priority order matters - longer matches first
private val orderedRules = listOf(
    // Three-letter conjuncts
    "ksh" to "ক্ষ", "ngk" to "ঙ্ক", "ngm" to "ঙ্ম", 
    "ngg" to "ঙ্গ", "ndh" to "ন্ধ", "nth" to "ন্থ",
    "rri" to "ঋ",
    
    // Two-letter conjuncts
    "kh" to "খ", "gh" to "ঘ", "ng" to "ঙ",
    "ch" to "চ", "chh" to "ছ", "jh" to "ঝ",
    "th" to "থ", "dh" to "ধ", "nh" to "ন্হ",
    "ph" to "ফ", "bh" to "ভ", "mh" to "ম্হ",
    "sh" to "শ", "ss" to "ষ", "s" to "স",
    "h" to "হ", "nn" to "ন্ন", "dd" to "দ্দ",
    "tt" to "ত্ত", "bb" to "ব্ব", "mm" to "ম্ম",
    "kk" to "ক্ক", "gg" to "গ্গ", "jj" to "জ্জ",
    "pp" to "প্প", "ll" to "ল্ল", "yy" to "য়্য",
    
    // Vowels
    "aa" to "আ", "i" to "ই", "ii" to "ঈ",
    "u" to "উ", "uu" to "ঊ", "ri" to "ঋ",
    "e" to "এ", "oi" to "ঐ", "o" to "ও", "ou" to "ঔ",
    "rri" to "ৠ", "ae" to "অ্য",
    
    // Consonants
    "k" to "ক", "g" to "গ", "c" to "চ", 
    "j" to "জ", "t" to "ত", "d" to "দ",
    "n" to "ন", "p" to "প", "b" to "ব",
    "m" to "ম", "y" to "য", "r" to "র",
    "l" to "ল", "w" to "ও", "h" to "হ",
    "sh" to "শ", "s" to "স", "f" to "ফ",
    "v" to "ভ", "z" to "জ", "x" to "ক্স",
    "q" to "ক"
)
```

### Phase 2: Advanced Bangla Features

#### A. Automatic Hasanta (্) Insertion
```kotlin
// After transliteration, post-process for conjuncts
private fun insertHasanta(text: String): String {
    // Pattern: Consonant + Consonant (not at end)
    // Insert hasanta between them
}
```

#### B. Reph (র্) Handling
Current basic implementation at line 23-25 in BanglaPhoneticEngine:
```kotlin
token == "r" && tokens.getOrNull(index + 1)?.firstOrNull()?.isLetter() == true 
    && prev?.isConsonantToken() == false -> {
    out.append("র্") // Reph initiation
}
```

**Improved Reph Logic**:
- `r` at start of syllable + following consonant → র্ (reph)
- Example: "rk" → র্ক, "rkh" → র্খ
- Example: "ram" → রাম (not reph, because vowel follows)

#### C. Yantara (্য) and Antasta Ya (য়)
```kotlin
// Yantara (্য) - after consonant
"ky" to "ক্য", "gy" to "গ্য", etc.

// Antasta Ya (য়) - standalone
"y" at end or after vowel → "য়"
```

### Phase 3: Visual Improvements

#### A. Key Preview Popup
Show enlarged key preview on press:
```kotlin
// In KeyboardView
private fun showKeyPreview(button: Button, text: String) {
    // PopupWindow with larger text
}
```

#### B. Bangla Labels in Layout Mode
When in Bangla Layout mode, keys should display Bangla characters:
```kotlin
// In KeyboardView.updateKeyLabels()
LanguageMode.BANGLA_LAYOUT -> {
    // Show Bangla characters on keys
    key_q.text = "ঙ"
    key_w.text = "য"
    // ... etc
}
```

#### C. Composing Text State
Use `setComposingText()` instead of `commitText()` during phonetic typing:
```kotlin
// While typing a word
currentInputConnection.setComposingText(partialTransliteration, 1)

// On word completion (space/enter)
currentInputConnection.finishComposingText()
currentInputConnection.commitText(finalText, 1)
```

## Implementation Priority

1. **High Priority** (Fix basic usability):
   - Fix diff-based update in KeyboardService
   - Expand phonetic rules list
   - Add automatic hasanta for common conjuncts

2. **Medium Priority** (Polish):
   - Implement setComposingText for visual feedback
   - Add key preview popups
   - Fix Probhat key labels

3. **Low Priority** (Advanced):
   - Complex conjuncts (3+ consonants)
   - Numerical input modes
   - Custom user-defined shortcuts

## Testing Checklist

Test these Bangla words:
- [ ] বাংলা (bangla)
- [ ] ক্ষমতা (khomota)
- [ ] শিক্ষা (shikkha)
- [ ] মানবতা (manobota)
- [ ] সংবিধান (songbidhan)
- [ ] উৎসব (utshob)
- [ ] রক্ত (rokto)
- [ ] স্কুল (school)
- [ ] স্বপ্ন (shopno)
- [ ] ক্ষণিক (khonik)
- [ ] জ্ঞান (ggan)
- [ ] দৃশ্য (drisho)
