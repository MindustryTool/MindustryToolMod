## Context

`FollowAssistTask.update()` currently resolves targets in three stages: named player, any `activelyBuilding()` teammate, then any alive teammate. The third stage claims idle players in `Any Active Builder` mode (empty `follow-assist.target-player`), preventing fall-through to `SelfBuild`, `Rebuild`, and `Mining` in `AutoplayFeature` priority order. The `TODO: Skip if there r no active builder` marks this. Constraints: Java 8 runtime APIs only, `arc.util.Nullable` for nullables, all user-visible status text via `Core.bundle`, ternary for simple conditions.

## Goals / Non-Goals

**Goals:**

- In `Any` mode, only claim a target when an active builder exists; otherwise yield so lower tasks run.
- Define active builder as `activelyBuilding() OR has queued build plans` to avoid flicker between placements.
- Preserve named-player loyalty while giving a clear `No active builders` status for the new yield path.
- Remove the stale TODO.

**Non-Goals:**

- No changes to priority order, `FollowAI` movement/mirroring, settings UI chips, or `AutoplayFeature` arbitration loop.
- No build-queue deduplication changes; no time-window/recency tracking.
- No new config keys or migration.

## Decisions

- **Split resolution by mode (Any vs named).** Any mode: search active builders only, yield if none. Named mode: follow named alive teammate even if idle; if named missing, search active builders then yield. Rationale: matches `Any Active Builder` label and user vote for loyalty in named mode. Alternative (strict in both modes) rejected because it breaks "stick with friend" expectation.
- **Active predicate = `unit != null && !dead && (activelyBuilding() || !plans.isEmpty())` with same-team and not-self guards.** Rationale: covers pause between placements without timer state. Alternative (strict `activelyBuilding()` only) rejected as flickery; alternative (N-second window) rejected as needing extra state for little gain.
- **Yield via `return false` with `ai.following = null`.** Rationale: lets existing arbitration fall through without special-casing; resetting `following` avoids stale mirror targets. Alternative (stay claimed but hold) rejected because it would still block productive tasks.
- **New bundle key `feature.autoplay.status.no-active-builders` with descriptive comment.** Rationale: `no-one-to-follow` is misleading when idle allies exist; i18n rules require comment + `Core.bundle.get`. Reuse rejected for clarity.

## Risks / Trade-offs

- [Risk] `unit.plans` may retain stale plans after build completes → Mitigation: still requires alive same-team unit; worst case follows one extra cycle until plans clear, then yields.
- [Risk] Named-missing fallback to active-then-yield may surprise users expecting team-grouping → Mitigation: documented in spec scenario; status distinguishes "no active builders" from "no ally".
- [Risk] Removing idle-follow reduces grouping/safety in PvP → Mitigation: scoped to Any mode only; named mode retains grouping; lower tasks (attack/repair) still run above follow.
