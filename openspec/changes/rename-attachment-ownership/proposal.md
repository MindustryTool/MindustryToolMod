## Why

`ParentStack` (WHERE an Element attaches) and `ComponentContext` (WHO owns a Disposable) are the two ambient stacks behind every Solim UI, but their names both suggest generic "parent / component" concepts with inconsistent `Stack` vs `Context` suffixes. New readers confuse visual placement with lifecycle ownership, which slows down work on `BaseComponent`, `StructuralReconciler`, and layout containers.

## What Changes

- Rename `solim.runtime.ParentStack` to `solim.runtime.AttachmentStack` (same API, same `Stack` suffix).
- Rename `solim.runtime.ComponentContext` to `solim.runtime.OwnershipContext` (same API, same `Context` suffix).
- Rename `solim.runtime.DebugParentStack` to `solim.runtime.DebugAttachmentStack` (same profiling/tracing behavior).
- Update all call sites, imports, nested-type references (`Entry`, `Attacher`, `CellConfigurator`), tests, `SolimEnv` teardown messages, `PerfSpan` component label, docs (`docs/solim_internals.md`), and spec text that names the old types.
- Keep all method names unchanged (`push/pop/current/isolate/capture/attachToParent/register/withoutAutoOwnership`, etc.).
- No behavior, lifecycle, threading, or public `solim.UI` facade changes.

## Capabilities

### New Capabilities

- None — pure rename, no new behavior.

### Modified Capabilities

- None — spec-level requirements are unchanged; existing specs only need mechanical name updates in text, not requirement changes.

## Impact

- Affected code: `solim-runtime` (3 types + `StructuralReconciler`, `ElementResolver`, `ReactiveContext` reference), `solim-core` main (~30 files: layouts, reactive primitives, `BaseComponent`, `LeafComponent`, modifiers, overlays), `solim` facade (`UI.java`), `solim-test` (`SolimEnv`, leak test), ~50 test files.
- `mod/` has zero references (verified) — no mod breakage. `SolimEncapsulationGuardTest` checks only the `solim.runtime` package, so it stays green.
- Docs/specs referencing old names must be updated in text to avoid drift (`docs/solim_internals.md`, ~7 spec files plus the `solim-flame-graph` change history, which stays historical).
