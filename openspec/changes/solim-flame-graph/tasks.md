## 1. Trace model and capture

- [x] 1.1 Add immutable `TraceSpan` value to `solim-api` (traceId, spanId, parentId, name, phase, startNs, endNs, depth, childCount)
- [x] 1.2 Add `TraceParentStack` debug variant with open-span stack, 4096 ring buffer, generation counter, and lockstep `isolate`/`capture`/`clear` handling
- [x] 1.3 Add leaf build hook on `BaseComponent.element()` path delegating to active trace variant as no-op when disabled
- [x] 1.4 Extend `Perf` facade with trace enablement, `isTracing()`, `traceSnapshot(int)`, `traceReset()`, capacity and depth queries

## 2. Export

- [x] 2.1 Implement span ordering, `minMs` pruning, and self-time computation at export time
- [x] 2.2 Implement Chrome Trace Events emitter (X phases, pid/tid per lane, ns-to-us conversion)
- [x] 2.3 Implement Speedscope-native emitter from same span list
- [x] 2.4 Implement self-contained HTML writer embedding both payloads with explicit-name-first labeling and fallback

## 3. Verification

- [x] 3.1 Add headless nested-build test asserting parent/child linkage, document order, and leaf coverage
- [x] 3.2 Add isolation test proving `isolate`/`capture` preserve parent chains
- [x] 3.3 Add disabled-by-default test proving zero spans and zero buffer allocation plus base singleton identity
- [x] 3.4 Add overflow test proving 4096 eviction reports dropped count
- [x] 3.5 Add export test proving dual payload agreement, minMs pruning, and non-empty HTML output
- [x] 3.6 Verify Java 8 runtime compat, `arc.util.Nullable` usage, no `solim-runtime` leakage to mod classpath, and i18n not required (no user-visible strings)
