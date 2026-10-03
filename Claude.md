# CLAUDE.md — Bingo Live (KMP, Client-Server Multiplayer)

> Persistent memory for Claude Code across all sessions. Read this before any task.

---

## App Identity

- **Name**: Bingo Live
- **Purpose**: A real-time multiplayer 90-ball bingo game. One host device calls numbers; any
  number of player devices hold a bingo ticket, see numbers as they're called, can only mark
  numbers that are actually on their ticket AND have been called by the server, and race to
  claim "Full House." The server is the single source of truth for the draw and for validating
  wins.
- **Target users**: A host running a live game, and players joining from their own phones.
- **Platform**: Kotlin Multiplatform — Android + iOS client (Compose Multiplatform), Ktor server
  (JVM), shared Kotlin module between all three.
- **Status**: Greenfield (evolving from an earlier single-device prototype — see Change Log)

---

## Tech Stack

### Client (composeApp)
- Framework: Compose Multiplatform (shared UI across Android + iOS)
- Language: Kotlin
- Architecture: MVVM — UI (Compose) → ViewModel → Repository (WebSocket client) → shared models
- State management: `StateFlow` / `MutableStateFlow` in ViewModels, single `UiState` sealed class
  per screen, consumed via `collectAsStateWithLifecycle()`
- Realtime transport: Ktor Client with the WebSockets plugin, connecting to the game server
- Design system: Custom (see Stitch UI prompt / `ui_prompt_stitch.md`)

### Server
- Framework: Ktor, WebSockets plugin for realtime, **`CIO` engine** (not Netty — CIO is pure
  Kotlin/coroutines, which is what makes it embeddable inside the host's own mobile app process
  rather than needing a separate standalone JVM process)
- Deployment model: **embedded** — the server runs inside the host's phone as part of the KMP
  app itself, started/stopped from the Caller screen, bound to the phone's local Wi-Fi IP so
  players on the same network/hotspot can reach it. Not deployed anywhere external.
- Language: Kotlin — shares the `shared` module with the client (same DTOs, same ticket/number
  generation code, so client and server can never disagree on rules)
- Game state: in-memory, single global game (one game at a time, no rooms)
- Persistence: optional, lightweight (e.g. a single JSON snapshot file) purely so an accidental
  server restart mid-game isn't catastrophic — not a hard requirement for v1
- **Platform caveat**: embedded Ktor/CIO servers are well-established on Android (JVM/ART). Kotlin/
  Native server support on iOS is newer and less proven — treat "host on iOS" as best-effort for
  v1; Claude Code should verify current Ktor/Kotlin-Native server support before implementing and
  flag it rather than assuming it works identically to Android.

### Discovery / Connection
- The host device displays a **QR code** encoding how to reach its embedded server
  (local IP + port, plus a short session token) so players join by scanning instead of typing
  an address
- QR generation: pure-Kotlin QR matrix generation in `shared/` (no platform image APIs needed —
  rendered directly as a Compose `Canvas`)
- QR scanning: platform-specific camera + barcode detection (`expect`/`actual`) — CameraX +
  ML Kit on Android, AVFoundation on iOS
- A manual fallback (plain-text IP:port shown under the QR) is always shown in case a device's
  camera can't scan it

### Shared module
- `kotlinx.serialization` for all DTOs / WebSocket messages
- Contains: `GameState`, `NumberGenerator`, `Ticket`, `TicketGenerator`, and the WebSocket
  message sealed classes — used unmodified by both `composeApp` and `server`

### Notifications
- In-app only, delivered live over the existing WebSocket connection while the app is open
  (no OS push / FCM / APNs in v1)

### Infra
- Build tool: Gradle (Kotlin DSL), multi-module KMP project (`shared`, `composeApp`, `server`)
- CI/CD: none required for v1

---

## Project Structure

```
BingoLive/
├── shared/
│   └── src/commonMain/kotlin/com/bingolive/
│       ├── model/
│       │   ├── GameState.kt          # calledNumbers, remainingPool, gameStatus
│       │   └── Ticket.kt             # 9x3 ticket model
│       ├── domain/
│       │   ├── NumberGenerator.kt    # pure draw logic (no platform deps)
│       │   └── TicketGenerator.kt    # standard 90-ball ticket generation
│       └── protocol/
│           └── GameMessages.kt       # sealed classes for all WebSocket messages
├── server/
│   └── src/main/kotlin/com/bingolive/server/
│       ├── Application.kt            # Ktor entry point, routing
│       ├── GameSessionManager.kt     # connected clients, broadcast, game lifecycle
│       └── GameEngine.kt             # wraps shared NumberGenerator, owns authoritative state
├── composeApp/
│   └── src/
│       ├── commonMain/kotlin/com/bingolive/
│       │   ├── network/
│       │   │   ├── GameSocketClient.kt   # Ktor Client WebSocket wrapper
│       │   │   └── EmbeddedGameServer.kt # embeddable start()/stop() wrapper around the Ktor/CIO server
│       │   ├── connection/
│       │   │   └── QrScanner.kt          # expect fun/composable, actual per platform
│       │   ├── viewmodel/
│       │   │   ├── CallerViewModel.kt    # host screen — starts server, shows QR
│       │   │   ├── JoinViewModel.kt      # player pre-join screen — scan QR, connect
│       │   │   └── AnswerSheetViewModel.kt  # player screen, once connected
│       │   └── ui/
│       │       ├── CallerScreen.kt            # host screen layout only; parts below
│       │       ├── CallerColors.kt            # shared palette + ball colours/letters
│       │       ├── CallerHeader.kt            # header bar, progress card
│       │       ├── HeroBallStage.kt           # current-ball stage, draw button
│       │       ├── CallHistory.kt             # history modes, column counts, 1-90 grid
│       │       ├── CallerCards.kt             # reset dialog, error card, winner card
│       │       ├── HostQrSection.kt           # start/stop hosting, QR canvas
│       │       ├── HostLobbyCards.kt          # lobby list, false-claim notice
│       │       ├── JoinScreen.kt
│       │       ├── AnswerSheetScreen.kt       # player screen layout only; parts below
│       │       ├── AnswerSheetBanners.kt      # header, connection/feedback/result banners
│       │       ├── CalledNumbersDisplay.kt    # current number, called-numbers strip
│       │       └── TicketCard.kt              # 9x3 ticket, cells, claim button
│       ├── androidMain/kotlin/.../connection/QrScanner.android.kt   # CameraX + ML Kit
│       │   .../network/LocalIpProvider.android.kt
│       └── iosMain/kotlin/.../connection/QrScanner.ios.kt           # AVFoundation
│           .../network/LocalIpProvider.ios.kt
└── CLAUDE.md
```

---

## Domain Ownership Map

| Domain | Directory / Module | Primary files |
|--------|--------------------|----------------|
| Ticket generation (90-ball rules) | `shared/domain/` | `TicketGenerator.kt`, `Ticket.kt` |
| Number draw logic | `shared/domain/` | `NumberGenerator.kt` |
| WebSocket protocol (shared contract) | `shared/protocol/` | `GameMessages.kt` |
| Game server (authoritative state, broadcast, win validation) | `server/` | `GameEngine.kt`, `GameSessionManager.kt`, `Application.kt` |
| Client networking | `composeApp/commonMain/network/` | `GameSocketClient.kt` |
| Embedded server lifecycle (host device) | `composeApp/commonMain/network/`, platform `actual` files | `EmbeddedGameServer.kt`, `LocalIpProvider` |
| QR generation (shared) + scanning (platform) | `shared/domain/`, `composeApp/commonMain/connection/` + platform `actual` files | `QrCodeGenerator.kt`, `QrScanner.kt` |
| Host (caller) UI + ViewModel | `composeApp/commonMain/{viewmodel,ui}/` | `CallerViewModel.kt`, `CallerScreen.kt` |
| Join / connect UI + ViewModel | `composeApp/commonMain/{viewmodel,ui}/` | `JoinViewModel.kt`, `JoinScreen.kt` |
| Player (answer sheet) UI + ViewModel | `composeApp/commonMain/{viewmodel,ui}/` | `AnswerSheetViewModel.kt`, `AnswerSheetScreen.kt` |

---

## Architecture Decisions

- **Server is the single source of truth.** The server owns `remainingPool`, `calledNumbers`,
  every issued `Ticket`, and game status. Clients never decide what's "called" or who "won" —
  they only render what the server tells them and send requests (draw / claim) for the server
  to validate.
- **Tickets are generated and held server-side**, then sent to each player on join. A player's
  UI can only ever mark a cell if it is BOTH present on that player's server-issued ticket AND
  present in the server's `calledNumbers` list — this is enforced in the ViewModel from server
  state, never from local guesses, and re-validated server-side on every claim.
- **Full House claims are validated server-side.** A client sends `ClaimFullHouse`; the server
  checks that every number on that client's ticket is in `calledNumbers`. Only then does it
  accept the claim, stop the game, and broadcast the winner. Client-side claim buttons are a
  convenience trigger only — never trust a client's self-reported "I won."
- **One global game, no rooms.** A single `GameEngine` instance holds all state; simplifies
  session management for v1.
- **`shared` module has zero platform-specific code** — this is what lets the exact same
  ticket/number logic run identically on the JVM server and the Android/iOS client, so there
  can never be a rules mismatch between them.
- **Draw and reset are host-privileged actions.** Only the connection that opened the host
  session may trigger `DrawNumber` / `NewGame`; player connections can only send `ClaimFullHouse`.
- **Host auth uses a private host secret, never the QR token.** The embedded server generates a
  256-bit `hostSecret` (SecureRandom) that stays in the host app; `JoinAsHost` must carry it.
  The host socket connects over loopback (`127.0.0.1`). An empty secret disables host joins.
  All WebSocket frames are capped at `ProtocolLimits.MAX_FRAME_BYTES` on both server and client.
- **Seats outlive sockets.** Each player gets a private `rejoinToken` (SecureRandom) in `Joined`;
  tickets are keyed by token, not connection. A dropped player rejoins with the token and gets the
  same ticket (the old socket is closed). Players who join after the first draw are seated with no
  ticket and receive one at the next new game. `JoinRejected` is terminal: clients must not retry.
- **Names, lobby, false claims.** Player names are cleaned by `PlayerNames` (shared) and made
  unique server-side; a rejoin keeps the original name. `LobbyUpdate` broadcasts `PlayerSummary`
  (public `playerId`, never the rejoin token). A claim while ticket numbers are uncalled sends the
  claimer `ClaimRejected` and the host `FalseClaim` with the uncalled numbers; a claim that lost a
  race after someone won (`ClaimOutcome.NotInProgress`) is not reported as false.
- **Line prize and missed wins.** `ClaimLine` wins if any one ticket row is fully called (rules in
  shared `WinRules`); first valid claim takes it, the game continues to Full House. `LineWon` /
  `GameOver` carry `missedBy`: other seats that also qualified but didn't claim first. Players learn
  their own `playerId` from `Joined` to recognise themselves. `GameStateUpdate.lineWinnerName` keeps
  late/rejoining clients in sync.
- **Connectivity.** Caller and answer-sheet screens keep the display on (`KeepScreenOn`); a locked
  phone throttles Wi-Fi. Server sends are parallel with a 5 s deadline and a stuck socket is
  cancelled (client rejoins and gets full state). Client connect is bounded (8 s) and reconnects
  immediately on app foreground.
- **Auto-call and voice are host-side conveniences.** `AutoCaller` only sends the same `DrawNumber`
  intent on a timer (3/5/8/12 s); it stops on a win, full board, new game or hosting ending, and
  pauses on a `FalseClaim`. Numbers are spoken (`SpeechAnnouncer`, phrases in `BingoCallPhrases`)
  only for server-confirmed draws, never for a rejected request.
- **The server is embedded in the host's own app process**, not a separately deployed service.
  Starting a game = starting the embedded server + binding it to the phone's local IP; closing
  the Caller screen or app should cleanly stop the server.
  On Android, hosting runs under `HostingService` (foreground service, type `connectedDevice`,
  Wi-Fi + partial wake lock) started/stopped by `EmbeddedGameServer`. Its notification's
  "Stop hosting" action and swiping the app from recents both end the game; the Caller screen
  follows via `GameServerHost.isRunning`.
- **QR is a connection-info carrier, not a trust mechanism.** The QR payload only tells a client
  where to connect (IP, port, a session token to avoid joining a stale/wrong server on the same
  network) — it grants no special privileges. All the same server-side validation from the
  "Server is the single source of truth" rule still applies to every client that connects,
  whether they joined via QR or the manual fallback code.

---

## Ticket Layout Rules (from reference images — must match exactly)

Standard 90-ball bingo ticket: **9 columns × 3 rows, 15 numbers, 12 blanks.**

- Column ranges: col 0 → 1–9, col 1 → 10–19, col 2 → 20–29, ... col 7 → 70–79, col 8 → 80–90
- Exactly **5 filled numbers per row**, 4 blanks per row
- Each column holds **1–3 numbers total** across the 3 rows, summing to 15 across all 9 columns
- Numbers within a column are sorted **ascending top to bottom**
- Blanks render as empty cells, not zero or placeholder text

---

## Naming Conventions

| Entity | Convention | Example |
|--------|-----------|---------|
| Screens | `[Name]Screen.kt` | `AnswerSheetScreen.kt` |
| ViewModels | `[Name]ViewModel.kt` | `AnswerSheetViewModel.kt` |
| WebSocket messages | sealed class per direction, `[Verb][Noun]` | `DrawNumber`, `ClaimFullHouse` |
| Server classes | `[Name]Manager.kt` / `[Name]Engine.kt` | `GameSessionManager.kt` |
| Models | plain data classes in `shared/model/` | `GameState.kt`, `Ticket.kt` |
| UI state | `[Screen]UiState` sealed class | `AnswerSheetUiState` |

---

## Critical Rules for Claude Code

### Always
- Read relevant files before writing any code
- Keep all game-rule logic (draw, ticket generation, win validation) in `shared/` or `server/`
  — never re-implement or duplicate rule logic in `composeApp`
- Treat the server as authoritative; the client only renders server state and sends intents
- Guarantee mathematically that a number 1–90 can never be drawn twice in the same game
- Guarantee a player can never mark a ticket cell that isn't both on their ticket and called
- Use `StateFlow` + `collectAsStateWithLifecycle()`, not `collectAsState()`
- Show only changed/created files in output
- Keep explanations to 2–3 sentences max

- UI icons come from `ui/icons` (`BingoIcons`, `GameIcons`: Material Rounded path data, no icon
  library dependency). Never use emoji or text glyphs as icons. Use `CircleIconButton`/`BackButton`,
  `StatusBanner`, `CelebrationCard`, `IconLabel` and the `Spacing` scale from `ui/CommonComponents.kt`.

### Never
- Rewrite unrelated code
- Trust a client-reported win — always re-validate full house server-side against that
  client's actual issued ticket and the authoritative `calledNumbers`
- Let a client mark an arbitrary cell locally without server-confirmed state behind it
- Put platform-specific code (Android/iOS APIs) anywhere in `shared/`
- Allow more than one active game to accept draws at the same time
- Continue accepting draws or claims after the server has declared a winner

---

## Prompt File Index

| # | File | Domain | Status | Depends on |
|---|------|--------|--------|-----------|
| 1 | `prompt_1_number_generator_kmp.md` | Number draw logic (superseded scope — see note below) | [x] superseded | — |
| 2 | `prompt_2_shared_protocol_and_ticket.md` | Shared module: protocol + ticket generator | [x] completed | — |
| 3 | `prompt_3_game_server_ktor.md` | Ktor server: session mgmt, broadcast, win validation | [x] completed | prompt_2 |
| 4 | `prompt_4_client_answer_sheet.md` | Player answer sheet UI + networking | [x] completed | prompt_2, prompt_3 |
| 5 | `prompt_5_discovery_and_qr_connection.md` | Embedded server lifecycle, QR generation/scanning, join flow | [x] completed | prompt_2, prompt_3, prompt_4 |
| — | `ui_prompt_stitch.md` | Caller screen design brief for Stitch | [ ] pending | — |

> **Note on prompt_3**: `Application.kt`'s standalone `fun main()` entry point is superseded by
> `EmbeddedGameServer.kt` (prompt_5), which wraps the same routing/plugin setup as a class with
> `start()`/`stop()` the host app calls — the routing and `GameSessionManager` wiring from
> prompt_3 are otherwise unchanged.

> **Note on prompt_1**: `NumberGenerator.kt` and `GameState.kt` from prompt_1 move into
> `shared/domain/` and `shared/model/` unchanged — they're still pure Kotlin and still correct.
> The per-device `expect`/`actual` persistence section of prompt_1 is superseded: persistence
> now belongs to the server (see prompt_3), since the server — not any one device — owns the
> authoritative game state.

---

## Change Log

| Date | Change | Prompt |
|------|--------|--------|
| 2026-09-25 | KMP scaffold (single-device prototype) | prompt_1 |
| 2026-09-25 | Pivoted to client-server multiplayer: Ktor server, shared ticket/protocol module, player answer sheet, server-validated full-house win | prompt_2, prompt_3, prompt_4 |
| 2026-09-25 | Added embedded server + QR-code discovery: host's phone runs the server itself, players scan a QR to connect on the same network | prompt_5 |
| 2026-09-26 | Implemented Prompts 2-5: modularized (:shared, :server, :composeApp, :androidApp), Ktor CIO embedded server & client, 90-ball ticket generation & derived marking, QR code Canvas generation & CameraX/ML Kit scanner, tests | prompt_2, prompt_3, prompt_4, prompt_5 |