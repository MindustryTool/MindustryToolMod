## Context

Outbound presence (`PUT /chats/users/state`) currently exists only in the legacy `old/.../chat/global/ChatStateManager` (153 lines, imperative, unsynchronized `previousState`/`currentState`, drop-if-busy `AtomicBoolean`, 60s forced refresh, direct imports of legacy PlayerConnect events that no longer exist). The rewritten chat stack (`ChatStore` + 9 state modules, `ChatService`, `MindustryTool.updateChatState`) has zero presence callers. `PlayerConnectFeature` (rewritten) exposes no custom events — it only listens to Mindustry events. Precedent for decoupled custom events exists (`LoginUriEvent`, `FeatureStateChanged`). Project constraints: Java 8 runtime APIs only, `Request` facade for HTTP (reuse `MindustryTool.updateChatState`), `arc.util.Nullable`, i18n bundle keys for user-visible settings text.

## Goals / Non-Goals

**Goals:**
- Restore outbound presence sync in the new Signal architecture with byte-identical wire strings.
- Survive transient `menu` flicker (pause menu, dialogs) via sticky last-non-menu state.
- Decouple Chat from PlayerConnect via events; coalesce rapid changes (1s debounce, last-wins); heal drift (5-min heartbeat).
- Always-on while logged in, controlled only by a default-on opt-out setting.

**Non-Goals:**
- Inbound presence (roster is online-only; `ChatUser.state` display untouched).
- Wire-format changes or backend work.
- Touching `old/` legacy code.

## Decisions

### 1. Keep legacy wire strings verbatim
`menu`, `server: <name>`, `player-connect: <name>`, `campaign: <map>`, `editing: <map>`, `custom-game`. Rationale: zero backend coordination, mixed-version clients keep interoperating. Alternative (typed enum + mapper) rejected: adds a mapping layer with no consumer benefit since the server echoes free strings.

### 2. Presence signals live in `ChatSession` (Option A)
Add `presence: Signal<String>` (init `menu`) and `lastNonMenu: Signal<String>` (init `menu`), mirroring the existing `connected` signal pattern. Alternatives: new 10th store module (rejected — ceremony for two signals); separate presence store (rejected — splits session-adjacent state).

### 3. PlayerConnect decoupling via `PcRoomOpened` / `PcRoomClosed` events
`PlayerConnectFeature` fires immutable events carrying the **resolved room name at fire time** (`roomNameConfig` value on the success path); chat-side sync observes them. Rationale: matches `LoginUriEvent` precedent; no Chat→PC compile dependency; avoids reintroducing the legacy async `getRoomWithCache` lookup (and its race) inside Chat. Alternative (Chat reading PC signals directly) rejected: hard cross-feature dependency. Payload is name-only (alternative: include join link for future affordances — rejected to keep the contract minimal).

### 4. Sticky resolver instead of pure derivation
Intent rules (synchronous, game thread): non-menu intent sets both `presence` candidate and `lastNonMenu`; `menu` intent stashes current presence into `lastNonMenu` (unless already `menu`) before switching; `playing-without-map-yet` (`StateChangeEvent` → playing before `WorldLoadEndEvent`) resolves to `lastNonMenu`. Derivation priority when truths coincide: PC-hosting > net.client > campaign/editor/custom > menu (same as legacy). Rationale: pure derivation flickers through pause-menu `menu` events; sticky + debounce emits one correct PUT where legacy emitted stale-then-real. Alternative (debounce only) rejected: timing luck, not structure.

### 5. 1s debounce (last-wins) + distinct-until-changed + 5-min heartbeat
Debounce absorbs restore→real sequences; distinct check against last-sent suppresses duplicates; heartbeat re-PUTs current presence every 5 minutes. Rationale: user-chosen values; replaces legacy 60s refresh + drop-if-busy `AtomicBoolean` (which silently lost last-writes — debounce preserves them).

### 6. Generation counter for `pingHost` replies
Each `ClientServerConnectEvent` intent bumps a counter; ping callbacks apply only if their generation is still latest. Rationale: ~5 lines, kills the connect-A→connect-B stale-name race. Alternative (skip ping, show IP:port) rejected: loses server names users recognize.

### 7. Gate on logged-in + opt-in only (REST, no stream requirement)
PUTs need auth, not SSE, so `ChatSession.connected()` is not a gate. Rationale: always-on must survive stream outages. (Legacy required stream; that coupled presence to chat transport for no protocol reason.)

### 8. Always-on lifecycle, owned by `ChatFeature` but independent of enable/disable
The sync listener registers once at mod init and runs whenever logged in + opted in — even if the Chat feature/overlay is disabled. Rationale: user decision ("always work unless opt out"); `ChatFeature`/`ChatStore` instances exist regardless of enabled state, so signals have a home. Alternative (bind to `onEnable`/`onDisable`) rejected: contradicts always-on. Alternative (mod-level owner in `Main`) rejected: breaks feature-oriented ownership.

### 9. Opt-out / logout: stop PUTs, send nothing
Accepted consequence: server retains last-known presence (ghost) until overwritten by a later session. Rationale: user decision; cheapest option, no backend support needed. Alternatives (`menu` lie, clear-state payload) rejected per user choice; clear-state can be revisited if the backend adds support.

### 10. Threading and project conventions
Mindustry event handlers run on the game thread and set signals directly; async callbacks (`pingHost`) re-enter via `Core.app.post`. Network goes through `MindustryTool.updateChatState` (`Request` facade). Java 8 runtime APIs only, `arc.util.Nullable` for nullable values, ternary style for simple conditions, bundle keys + translator comments for the toggle text.

## Risks / Trade-offs

- [Ghost presence after opt-out/logout] → Accepted by decision; revisit via clear-state endpoint if backend adds one.
- [1s debounce delays presence visibility] → Acceptable for presence semantics; correctness (no flicker) outweighs freshness.
- [Heartbeat traffic every 5 min] → Negligible single PUT; heals silent server-side expiry.
- [Event listener never unregistered] → Codebase norm (`PlayerConnectFeature`, `TeamResourceFeature` same); mitigate by registering once and guarding handlers with opt-in/logged-in checks.
- [Pause-menu may fire `paused`, not `menu`] → Pre-implementation spike: log `StateChangeEvent.to` while opening pause/host/join dialogs; adjust sticky rules if the flicker source differs.
- [`pingHost` failure/timeout leaves server name unknown] → Hold last presence (emit nothing new) until resolution or superseding intent; never emit IP:port fallback (per decision 6).
- [Headless/minimized] → Never sync when headless; skip `menu` intents while graphics hidden (legacy guard retained).

## Migration Plan

1. Spike: confirm `StateChangeEvent` values for pause menu and dialog flows; confirm `PUT /chats/users/state` accepts legacy payloads.
2. Implement per tasks; legacy `old/` untouched and inert.
3. Rollout: ship behind the default-on toggle; verify PUT traffic and presence display from a second client.
4. Rollback: user-level (toggle off) or revert; no data migration, no backend dependency.

## Open Questions

- Exact heartbeat scheduler (`Timer` vs worker thread) — implementation detail, either is fine.
- Whether `PcRoomOpened` should re-fire on mid-host room rename (`updateRoomStats` path) or open/close only (lean: open/close only).
