## Why

Every Map Rules field is currently declared in six places (`RuleDiff` + counts, `RuleMutator` dispatch, `currentValueOf`, `resetToDefaults` ×2, view section, bundle keys), so adding one rule is a six-site edit that invites drift — the 37-rule expansion proved the cost. A single rule-descriptor table collapses this to one line per rule and makes future coverage cheap.

## What Changes

- Introduce a rule-descriptor table (`mindustrytool.features.maprules`): one entry per rule with id, typed accessors (`arc.func`-style lambdas), control type/range, and category; bundle keys follow the `feature.map-rules.rule.<id>` convention.
- Derive from the table: diff checks + modified counts, mutator dispatch, reset copy loop, view rows, and search matching.
- Replace the five hand-written section methods with one generic section renderer and one shared `ruleCard` wrapper.
- Unify the banned-block/unit grids and ban toggles over a generic content helper.
- No user-visible behavior changes; all 60 rules keep identical labels, ordering, editing, filtering, saving, and reset semantics.

## Capabilities

### New Capabilities

- `maprules-descriptors`: Structural contract for the descriptor-driven Map Rules implementation — single-source rule definitions, derived UI/behavior, and preserved user-visible semantics. Creates `specs/maprules-descriptors/spec.md`.

### Modified Capabilities

<!-- None: no existing spec-level requirements change. -->

## Impact

- **Code**: `mod/src/mindustrytool/features/maprules/` — new descriptor type plus rewrites of `RuleDiff`, `RuleMutator` dispatch, `MapRulesFeature.resetToDefaults`, and `MapRulesView` sections. Net deletion expected (1100-line view shrinks substantially).
- **Bundles/locales**: untouched (key names and values unchanged).
- **Depends on**: `maprules-cleanup` should land first (clean ground, independent files overlap).
- **Follow-ups (out of scope)**: reactive ban chips without full refresh, splitting `MapRulesView` by area.
