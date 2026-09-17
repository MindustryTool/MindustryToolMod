## Context

MindustryTool features are `Feature` subclasses registered in `Main`, each owning a `ConfigGroup` for persisted settings, a `FeatureMetadata` (id, icon, order, default state, quick-access), optional Solim settings dialog, and event subscriptions via `Events`. See `WavePreviewFeature` and `JoystickFeature` for the established pattern.

Explosive reactors are a hazard: `thoriumReactor` has `explosionRadius = 19` and `explosionDamage = 1250 * 4`, and a nearby core can be destroyed by a single detonation. Vanilla registers reactor blocks with the `BlockFlag.reactor` flag (`NuclearReactor`, `ImpactReactor`), but the flag is incomplete (`fluxReactor` and `neoplasiaReactor` also explode without it).

The feature runs client-side. Each client evaluates placement events locally from its own synced world state; no server broadcast or networking is added.

## Goals / Non-Goals

**Goals:**
- Alert when another player begins constructing an explosive reactor within a configurable distance of a core.
- Cover every explosive reactor, including modded ones, without hardcoding block names.
- Attribute the alert to the responsible player, degrading to the builder's team when no player is resolvable.
- Fire early (construction start) so teammates still have time to react.
- Keep the alert configurable (radius) and readable (player/team, distance, reactor name).

**Non-Goals:**
- Do not alert on the local player's own placements.
- Do not alert when a core is placed near an existing reactor.
- Do not alert on reactor deconstruction or repair.
- Do not add a polling/alternate detection fallback for multiplayer clients.
- Do not change enemy-threat or core-protection rules.

## Decisions

1. **Trigger on `BlockBuildBeginEvent` rather than `BlockBuildEndEvent`.**
   The begin event fires while the reactor is still a construct (`Build.java:157`), so the warning is actionable. Trade-off: it also fires for construction that may later be cancelled, and it lacks `config`. Chosen over `BlockBuildEndEvent` which fires only on completion.

2. **Resolve the target block from `ConstructBuild.current`.**
   At begin time `tile.block()` is the size-matched `ConstructBlock`, not the reactor (`Build.java:149-157`). The target is stored via `build.setConstruct(...)`, exposed as `ConstructBuild.current`. Deconstruction begin fires with `breaking = true` and is filtered out.

3. **Explosive-reactor predicate as a union.**
   `block.flags.contains(BlockFlag.reactor) || block.explosionRadius > 0f`. Chosen over `BlockFlag.reactor` alone (misses flux/neoplasia) and over an explicit block list (not mod-friendly).

4. **"Other players only".**
   Resolve `Player` via `event.unit.getPlayer()`. Skip when `player == Vars.player`. When `event.unit` is null or has no player, attribute the alert to `event.team` (team name) rather than dropping it. In singleplayer this makes the feature silent.

5. **Distance measured center-to-center.**
   `Mathf.dst(tile.worldx(), tile.worldy(), core.x, core.y) / tilesize` against `Vars.state.teams.cores(event.team)`. Uses the builder's team's cores only. Chosen over edge-gap and Chebyshev metrics for simplicity and predictability.

6. **Global 3-second cooldown.**
   A single monotonic timestamp gates all alerts. Simpler than per-tile tracking; the trade-off is that a second distinct reactor within the window is suppressed.

7. **Presentation via the chat fragment.**
   `Vars.ui.chatfrag.addMessage(String)` (`ChatFragment.java:311`) with red color markup (e.g. `[scarlet]...[]`). No color overload exists, so color is embedded in the string. Chosen by product decision over `hudfrag.showToast`/`showInfoToast`.

8. **Configuration.**
   `ConfigGroup` float value `radius`, default `10f`, exposed as a Solim slider from `1` to `30`. Rendered through `ReactorAlertSettingsView` / `ReactorAlertSettingsDialog`, following `JoystickSettingsView`.

9. **Feature metadata.**
   id `reactor-alert`, `FileIcon.of("triangle-alert.png")`, order `23`, `enabledByDefault = true`, `quickAccess = true`.

10. **Internationalization.**
    All user-visible strings (feature name/description, settings label, message template) live in `assets/bundles/bundle.properties` with descriptive comments and documented placeholders.

## Risks / Trade-offs

- **Multiplayer client delivery of `BlockBuildBeginEvent` is not verified.** → Accepted by decision; the event is the standard placement hook and the feature is valid in singleplayer/host. Revisit if multiplayer testing shows it is missing.
- **Global cooldown can suppress a legitimate second alert.** → Accepted trade-off for simplicity; 3s window is short.
- **Begin event may fire for cancelled constructions.** → Accepted; an early false-positive is preferable to a missed warning.
- **`event.unit` may be null.** → Handled by team-name attribution instead of dropping.
- **`ConstructBuild.current` must be read at begin.** → Guard by `event.tile.build instanceof ConstructBuild`; do not read `tile.block()`.
- **Missing icon asset.** → `FileIcon.of` falls back to `Icon.book` until `assets/icons/triangle-alert.png` is supplied.
- **Chat fragment availability.** → Guard `Vars.ui.chatfrag != null` before `addMessage`.

## Migration Plan

No migration. The feature is additive and enabled by default; disabling it removes the event listener. No persisted data beyond the radius config value.

## Open Questions

- None blocking. The "core placed near existing reactor" and "multiplayer client fallback" cases are explicit non-goals.
