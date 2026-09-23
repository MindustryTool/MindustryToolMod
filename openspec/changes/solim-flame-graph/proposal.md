## Why

Solim can report *which* structural spans were slow (flat `PerfSpan` list) but cannot show *why* a build was slow: no parent-child nesting, no timing for fast nodes, and no view of leaf `BaseComponent.build()` cost. Page-change jank in collection views (e.g. schematic browser) is therefore hard to attribute. A build-lane flame trace closes this gap with hierarchy-first diagnostics.

## What Changes

- Adds opt-in hierarchical build tracing for Solim component construction (containers via `ParentStack` push/pop plus leaf `BaseComponent.element()` builds).
- Records full nested spans (no slow threshold) into a bounded rolling buffer while tracing is enabled; zero cost when disabled via the existing base/debug singleton swap.
- Exposes trace control and snapshots through the public `Perf` facade without exposing `solim-runtime` types.
- Adds headless self-contained HTML flame export embedding both Chrome Trace Events and Speedscope-native payloads, with export-time `minMs` pruning.
- Keeps existing slow-span `PerfSpan` list, `UiProfiler` frame aggregates, and MCP read-only introspection untouched.

## Capabilities

### New Capabilities

- `solim-flame-trace`: Hierarchical build-span capture (TraceSpan model, trace variant stack, rolling buffer, Perf facade control).
- `flame-graph-export`: Headless HTML flame export with dual-format embedded data and export-time filtering.

### Modified Capabilities

- None. Existing `slow-component-tracking`, `solim-ui-perf-testing`, and `solim-mcp-debug-server` requirements are unchanged; flame trace is additive and isolated.

## Impact

- Affects `solim-api` (new `TraceSpan` value type), `solim-runtime` (new trace stack variant + leaf hook via `ComponentContext`), `solim-core` (`Perf` facade additions), and a small headless exporter (core or dedicated module, no new runtime dependency).
- No breaking API changes; no changes to `PerfSpan`, `ParentStack` base class hot path, or MCP tool contracts.
- Android-sensitive: bounded 4096-span ring (~256KB), `System.nanoTime` only while enabled, no per-frame allocation when disabled.
