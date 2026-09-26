# CLAUDE.md — Bingo Number Caller (KMP)

> Persistent memory for Claude Code across all sessions. Read this before any task.

---

## App Identity

- **Name**: Bingo Number Caller
- **Purpose**: Draw random numbers from 1–90, each number exactly once per game, and show the host the full history of numbers already called.
- **Target users**: A single host running a live bingo game on their phone.
- **Platform**: Kotlin Multiplatform — Android + iOS, shared UI via Compose Multiplatform
- **Status**: Greenfield

---

## Tech Stack

### Frontend
- Framework: Compose Multiplatform (shared UI across Android + iOS)
- Language: Kotlin
- Architecture: MVVM with clean architecture layers — UI (Compose) → ViewModel → Repository → Data Source
- State management: `StateFlow` / `MutableStateFlow` in ViewModels, single `UiState` sealed class
  for the caller screen, consumed via `collectAsStateWithLifecycle()`
- Navigation: single-screen app — no NavHost needed for v1
- Design system: Custom (see Stitch UI prompt / `ui_prompt_stitch.md`)

### Backend
- None. Fully client-side, no server, no auth.

### Shared / Infra
- Local persistence: `multiplatform-settings` (or DataStore via `androidx.datastore` multiplatform)
  to save the in-progress game so it survives an app restart, shared from `commonMain`
- Build tool: Gradle (Kotlin DSL), standard KMP module layout
- CI/CD: none required for v1

---

## Project Structure

```
BingoCaller/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/
│   │   │   └── kotlin/com/bingocaller/
│   │   │       ├── model/
│   │   │       │   └── GameState.kt         # calledNumbers, remainingPool
│   │   │       ├── data/
│   │   │       │   └── GameSettingsDataSource.kt  # expect/actual persistence
│   │   │       ├── domain/
│   │   │       │   └── NumberGenerator.kt   # pure draw logic (no platform deps)
│   │   │       ├── viewmodel/
│   │   │       │   └── CallerViewModel.kt   # StateFlow<UiState>, drawNumber(), newGame()
│   │   │       └── ui/
│   │   │           └── CallerScreen.kt      # Compose UI
│   │   ├── androidMain/
│   │   │   └── kotlin/.../GameSettingsDataSource.android.kt  # actual impl
│   │   └── iosMain/
│   │       └── kotlin/.../GameSettingsDataSource.ios.kt      # actual impl
└── CLAUDE.md
```

---

## Domain Ownership Map

| Domain | Directory / Module | Primary files |
|--------|--------------------|----------------|
| Number generation + persistence | `commonMain/domain/`, `commonMain/data/`, platform `actual` files | `NumberGenerator.kt`, `GameSettingsDataSource.kt` |
| ViewModel / state | `commonMain/viewmodel/` | `CallerViewModel.kt` |
| UI | `commonMain/ui/` | `CallerScreen.kt` |

---

## Architecture Decisions

- All draw logic and "already called" tracking lives in `commonMain/domain/NumberGenerator.kt` —
  pure Kotlin, zero platform (`expect`/`actual`) dependencies, zero Compose imports.
- The pool of remaining numbers (1–90) is the single source of truth. A number is removed from
  the pool the moment it's drawn, guaranteeing no repeats without a separate "seen" check.
- Persistence uses `expect`/`actual` so `commonMain` defines the interface and Android/iOS each
  provide their native storage implementation — the ViewModel never touches platform code directly.
- Game state is persisted after every draw, so an accidental app close mid-game doesn't lose
  progress.
- A manual "New Game" action in the ViewModel is the only way state resets — no silent auto-reset.

---

## Naming Conventions

| Entity | Convention | Example |
|--------|-----------|---------|
| Screens | `[Name]Screen.kt` | `CallerScreen.kt` |
| ViewModels | `[Name]ViewModel.kt` | `CallerViewModel.kt` |
| Data sources | `[Name]DataSource.kt` (+ `.android.kt` / `.ios.kt` for actuals) | `GameSettingsDataSource.kt` |
| Models | plain data classes in `commonMain/model/` | `GameState.kt` |
| UI state | `[Screen]UiState` sealed class | `CallerUiState` |

---

## Critical Rules for Claude Code

### Always
- Read relevant files before writing any code
- Keep draw/persistence logic in `commonMain`, with `expect`/`actual` only for the storage I/O
- Guarantee mathematically that a number 1–90 can never be drawn twice in the same game
- Use `StateFlow` + `collectAsStateWithLifecycle()`, not `collectAsState()`
- Show only changed/created files in output
- Keep explanations to 2–3 sentences max

### Never
- Rewrite unrelated code
- Put randomization logic inside a `@Composable` function
- Put platform-specific code (Android/iOS APIs) anywhere in `commonMain`
- Reset the pool automatically without the user tapping "New Game"
- Allow the drawn-numbers list to exceed 90 entries or contain duplicates

---

## Prompt File Index

| # | File | Domain | Status | Depends on |
|---|------|--------|--------|-----------|
| 1 | `prompt_1_number_generator_kmp.md` | Domain logic / state (number draw + persistence) | [ ] pending | — |
| — | `ui_prompt_stitch.md` | UI (design brief for Stitch, not Claude Code) | [ ] pending | — |

---

## Change Log

| Date | Change | Prompt |
|------|--------|--------|
| 2026-09-25 | KMP scaffold (ported from Flutter version) | prompt_1 |