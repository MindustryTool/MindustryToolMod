## Context

Map Rules state is spread across `RuleDiff` (per-field predicates + manual counts), `RuleMutator` (100-branch string dispatch in `setFloatField`/`setIntField`/`setBoolField` plus mirrored `currentValueOf`/`currentIntOf` readers), `MapRulesFeature.resetToDefaults` (two ~60-line copy blocks), and `MapRulesView` (five bespoke section renderers, `sliderRow`/`intRow` twins, block/unit grid twins). Project constraints apply: Java 8 runtime (no `java.util.function` — use `arc.func.*`), Solim declarative UI (no `.get()` in `build()`, `WebStyles` styling), and unchanged bundle keys.

## Goals / Non-Goals

**Goals:**
- One descriptor per rule as the single source of truth; diff, dispatch, reset, rows, and search all derive from it.
- Delete the stringly-typed dispatch chains and duplicated copy blocks.
- Collapse the five section renderers and three twin-pairs into generic renderers.

**Non-Goals:**
- No new rules, no UI redesign, no bundle changes.
- Reactive ban chips without full refresh and file-level view splitting are follow-ups, not commitments.
- No reflection over `Rules` fields.

## Decisions

### Decision 1: Lambda accessor table over reflection
- **Choice**: Each descriptor carries typed `arc.func`-style getter/setter lambdas (e.g. float getter + float setter for sliders, bool getter + setter for toggles, int pair for stepped sliders), alongside id, category, control kind, and range.
- **Rationale**: Type-safe under rename (compiler breaks loudly), checkstyle-clean, and consistent with the codebase's `arc.func` mandate. Reflection would be ~10 lines but fails silently on Mindustry field renames.
- **Alternatives Considered**:
  - *Reflection over `Rules` fields*: compact, but silent failures and `ModClassLoader` concerns argue against it; precedent (`ToggleRenderingFeature`) uses reflection only for inaccessible internals, not for plain public fields.

### Decision 2: Bundle keys by convention, not by table
- **Choice**: Labels resolve as `feature.map-rules.rule.<id>` / `.desc` at render time; the table stores only the id.
- **Rationale**: Zero bundle churn and zero per-rule label wiring; search can match against resolved strings exactly as today.
- **Alternatives Considered**:
  - *Storing keys explicitly per descriptor*: redundant when the convention already holds for all ~60 rules.

### Decision 3: Generic section renderer + shared card wrapper
- **Choice**: One `section(headerKey, descriptors, filterState)` renderer and one `ruleCard(label, modified, descKey, content)` wrapper; `sliderRow`/`intRow` become thin skins over a shared card (kept separate only because `SolimSlider` binds `Signal<Float>` vs `Signal<Integer>` distinctly).
- **Rationale**: Removes the five ~40-line section methods and the header/gap/card boilerplate while keeping the distinct slider bindings the framework requires.
- **Alternatives Considered**:
  - *Unifying int into float rows with rounding*: fights the framework's typed bindings; more clever, less clear.

### Decision 4: Generic ban helper over shared content supertype
- **Choice**: One grid + one toggle parameterized over the common surface Block and UnitType share (`name`, `localizedName`, ban-set add/remove), with call-site lambdas for the set-specific parts.
- **Rationale**: The block/unit twins differ only in content type and set reference; generics remove both pairs at once.
- **Alternatives Considered**:
  - *Leaving bans bespoke*: they hold the trickiest logic (search caps, optimistic `/js` toggle); but that logic is identical for both, so sharing reduces the chance of the two drifting apart again.

### Decision 5: Reset as a loop over descriptors
- **Choice**: `resetToDefaults` iterates the table copying snapshot/default values through the typed setters, including ban-set restore as a descriptor of set kind.
- **Rationale**: Deletes both ~60-line copy blocks and makes future fields reset-safe by construction.
- **Alternatives Considered**:
  - *Whole-object assignment (`state.rules = snapshot.copy()`)*: would drop live object identity and skip host broadcast plumbing; the loop preserves current mutation paths.

## Risks / Trade-offs

- **[Risk] Descriptor table itself drifts from `Rules` (new Mindustry fields)** → Mitigation: table lives next to the feature; a compile-time exhaustive test is out of scope, but the table is the only place to update now instead of six.
- **[Risk] Large diff touching every maprules file at once** → Mitigation: land `maprules-cleanup` first; verify with existing gates (compile, checkstyle, bundles) plus in-game spot checks per spec scenarios.
- **[Risk] Lambda verbosity (~60 accessors) looks noisy** → Mitigation: one line per rule in a static table; accepted explicitly over reflection in Decision 1.
