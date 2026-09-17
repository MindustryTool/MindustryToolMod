## Context

Mindustry maps and servers configure custom gameplay via `mindustry.game.Rules`. Vanilla Mindustry restricts access to its built-in rule editor dialog behind `rules.allowEditRules && (net.server() || !net.active())`, rendering rules invisible and unchangeable in standard campaign, survival, and multiplayer sessions.

This design introduces a new `MapRulesFeature` under `mindustrytool.features.rules` that provides a complete, declarative Solim-based interface to view and modify game rules, tailored with a three-tier permission model and diffing against defaults.

## Goals / Non-Goals

**Goals:**
- Provide a unified, responsive Solim dialog (`MapRulesDialog`) to inspect all active game rules categorized logically (Multipliers, Waves & Spawns, Banned Content, Environment, Mechanics).
- Implement role-based authority:
  - Singleplayer: Direct mutation of `Vars.state.rules`.
  - Host: Mutation with immediate network broadcast via `Call.setRules(Rules)`.
  - Multiplayer Client (Non-Admin): Read-only view of gameplay rules + local client-side toggles for visual rendering rules (fog, lighting).
  - Multiplayer Client (Admin): Remote mutation via `/js` console chat invocation.
- Diff active rules against default rules (`new Rules()`) to enable a `[★ Modified Only]` filter chip.
- Provide real-time search filtering across rule names, descriptions, and banned content.
- Integrate access exclusively via the QuickAccess HUD bar (`quickAccess(true)`).

**Non-Goals:**
- Modifying vanilla pause dialog (`PausedDialog`) layouts or injecting buttons into vanilla pause menus.
- Allowing non-admin multiplayer clients to modify server-authoritative simulation rules that would cause network desyncs.
- Constructing an arbitrary custom wave spawner designer (vanilla spawn groups remain intact; rule-level wave properties like win wave and wave spacing are supported).

## Decisions

### Decision 1: Pure Solim Declarative Architecture over Vanilla Dialog Unhiding
- **Choice**: Implement `MapRulesDialog` and `MapRulesView` purely using Solim declarative components (`column()`, `row()`, `wrap()`, `slider()`, `checkbox()`, `WebStyles.filterChip()`).
- **Rationale**: Vanilla `CustomRulesDialog` is an Arc widget lacking read-only states, diffing filters, or cohesive styling with `MindustryTool`. A pure Solim component ensures adherence to the project's declarative UI architecture, reactive signals, and `AGENTS.md` guidelines.
- **Alternatives Considered**:
  - *Unhiding vanilla dialog*: Setting `rules.allowEditRules = true` would enable editing, but gives no read-only safety for multiplayer clients, no diffing, and poor responsive styling.

### Decision 2: Permission-Tier Handling
- **Choice**: The dialog determines editability reactively based on network state:
  ```java
  public enum RuleEditMode {
      SINGLE_PLAYER, // Direct edit
      HOST,          // Direct edit + Call.setRules
      CLIENT_ADMIN,  // Remote edit via /js
      CLIENT_READONLY // Read-only with local visual override toggles
  }
  ```
- **Rationale**: Clear separation prevents accidental multiplayer desyncs while giving server admins and singleplayer users full capability.
- **Alternatives Considered**:
  - *Blind local mutation for clients*: Modifying `Vars.state.rules` client-side for block health/damage leads to ghost health bars and desync bugs.

### Decision 3: "Modified Only" Snapshot & Comparison
- **Choice**: Instantiate `new Rules()` as the baseline and compare primitives and collection differences to flag non-default rules.
- **Rationale**: Players joining custom maps or community servers often just want to know "What is special about this map?". A dedicated filter chip solves this immediately.

### Decision 4: QuickAccess-Only Entry Point
- **Choice**: Expose the feature through the QuickAccess HUD bar via `FeatureMetadata.quickAccess(true)`.
- **Rationale**: Gives rapid 1-tap access at any moment during gameplay without hacking into or conflicting with `Vars.ui.paused` or mobile pause layouts.

## Risks / Trade-offs

- **[Risk] Admin `/js` disabled on server** → Mitigation: If a client admin attempts to change rules on a server where `/js` is disabled, the chat command will fail harmlessly or display server rejection; UI notes `/js` requirement.
- **[Risk] Heavy UI allocation for 70+ rule items** → Mitigation: Solim layout uses category chips and collapsible/filtered views so only relevant sections render in the active scroll hierarchy.
- **[Risk] Client-side visual desync if fog/lighting toggled** → Mitigation: Visual overrides affect client rendering explicitly (e.g. `state.rules.fog = false` locally); state reset restores server-sent rules on world reload.
