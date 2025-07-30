# StoryForge Architecture Overview

## 📦 Layer Structure

### 1. Core Layer (`/core`)
Handles AI narration, dummy and real, and parsing structured deltas from narrator output.

| File | Purpose |
|------|---------|
| `AINarrator.kt` | Real API client (e.g. to DeepSeek) |
| `SecretsLoader.kt` | Loads stored API key from context |
| `NarrationParser.kt` | Extracts narration and JSON deltas, converts to DeltaInstruction |

---

### 2. Model Layer (`/model`)
Holds game data, applies delta updates, handles persistence and logging.

| File | Purpose |
|------|---------|
| `GameState.kt` | Tracks HP, gold, narration, worldState (dynamic JsonObject) |
| `DeltaInstruction.kt` | Sealed class for Add, Assign, Declare, Delete operations |
| `GameStateStorage.kt` | Saves and loads main game state to file |
| `GameStateSlotStorage.kt` | Multi-slot save/load system |
| `WorldStateLog.kt` | JSONL log of each delta update with timestamp |

---

### 3. Prompt System (`/prompt`)
Manages user-pinned prompt cards and default templates.

| File | Purpose |
|------|---------|
| `PromptCard.kt` | Prompt definition (id, title, tags, etc.) |
| `PromptCardStorage.kt` | Load/save prompt cards to local file. Supports default seed data from assets. |

---

### 4. UI Screens (`/ui/screens`)
Composable screens for all features: narration, prompt management, etc.

| File | Purpose |
|------|---------|
| `NarratorScreen.kt` | Main interaction screen. Sends actions, shows dialogue, rolls dice. |
| `PromptScreen.kt` | UI for browsing, adding, and deleting prompt cards |
| `SettingsScreen.kt` | Experimental settings control |
| `SavedGamesScreen.kt` | Lists save slots and allows loading a saved game |

---

### 5. UI Shell & Theme (`/ui`)
Houses navigation and app-wide style.

| File | Purpose |
|------|---------|
| `AppNavigation.kt` | NavHostController and routes setup |
| `StoryForgeTheme.kt` | Enforces dark theme styling |

---

### 6. ViewModel
Top-level state holder and logic processor.

| File | Purpose |
|------|---------|
| `StoryForgeViewModel.kt` | Central logic hub: wraps narrator call, applies deltas, manages UI state |

---

## 🔄 Game Data Flow

```text
NarratorScreen input
    → viewModel.processAction(input)
        → AINarrator.generate()
            → NarrationParser.extractJsonAndCleanNarration(raw)
                → narration + Map<String, DeltaInstruction>
        → GameState.applyDeltas(deltas)
            → worldState += structured values
            → WorldStateLog.append()
        → turns += (action, narration)
        → notify UI observers
```

---

## 🧪 Testing Tips
- Use `useDummyNarrator = true` in `Settings` to test without API.
- Append structured delta block after narration:

```text
AI: You reach the city gates. A guard eyes you suspiciously.
```json
{ "!location": "CityGate", "+gold": 5, "-alerted": true }
```

---

## 📌 Future Goals
- Visual delta browser
- WorldState viewer/editor
- Robust error logging and sync system
- Prompt-to-worldState bindings

---

Last Updated: 2025-07-22

