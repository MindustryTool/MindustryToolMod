## Why

Recent refactoring across the last two commits (`8db07f88` and `a2c818c1`) introduced several regressions and architectural smells in Solim: manual `own(...)` calls proliferated across widgets, ad-hoc fallback ownership classes (`ModifierOwnership`) were added, profiling measurement was embedded directly in normal UI execution (`AttachmentBridge` / `Component.element()`), disposal cycles swallowed/logged errors instead of failing loud, and test-only helper metrics (`listenerCount`, `observerCount`) leaked into public API contracts.

This change cleans up those regressions, ensuring Solim enforces declarative ownership via lazy `build()` initiation, strict fail-fast error handling, and clean separation between runtime UI and profiling hooks.

## What Changes

- **Lazy `build()` Widget Standard**: Convert eager widgets (`Button`, `SolimSlider`, `Row`, `Column`, `Card`, `Grid`, `SolimCollapser`, `Tabs`, `SolimStack`) to lazy `build()` components so effects and children register automatically in `OwnershipContext` during element creation.
- **Elimination of Manual Ownership**: Remove redundant manual `own(...)` calls across all widgets and delete `ModifierOwnership.java`.
- **Decouple Profiling from Core Engine**: Remove profiling/tracing hooks (`isTracing()`, `traceLeaf()`, and timestamp recording) from `Component.element()` and `AttachmentBridge`. Profiling must be isolated to debug tools and override servers.
- **Strict Fail-Fast / Fail-Loud**: Eliminate silent `catch` and catch-and-log blocks in `Component.dispose()`, `SessionRoot.dispose()`, and `QueryView`. Failures must propagate immediately to surface errors early during development.
- **Clean API & Test Isolation**: Remove unneeded test-only metrics (`listenerCount()` and `observerCount()`) from `Signal`, remove `SolimAssert.assertOwned()`, and eliminate unnecessary constructor overloads.
- **Purge Narrative Comments**: Strip narrative changelog comments explaining what/why changed, preserving only rules for AI agents and required spec documentation.

## Capabilities

### New Capabilities
<!-- No new capabilities; this is a cleanup and hardening refactor. -->

### Modified Capabilities
- `solim-component-ownership`: Strengthen auto-ownership rules, remove manual `ModifierOwnership` fallback and redundant `own()` calls, and require fail-fast disposal.
- `solim-session-root`: Retain permanent session root for unparented components while removing test-only helpers and silent error handling.

## Impact

- Affects `solim-api` (`Component`, `AttachmentBridge`, `SessionRoot`, `SolimAssert`).
- Affects `solim-core` (widgets, modifiers `CellConfig`/`ElementConfig`/`TableConfig`, reactive primitives `Signal`, `QueryView`).
- Affects `solim-runtime` (`AttachmentStack`, `DebugAttachmentStack`).
- Test suites relying on `listenerCount()` or `assertOwned()` updated to test observable behavior.
