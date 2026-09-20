# Solim Test Environment Layering Fix

## Why

The `solim-test` module declares main-source (`implementation`) dependencies on `solim-core` and `solim-runtime`, while those modules depend back on `solim-test` in their test configurations. Gradle tolerates this — test dependencies are not exported, so there is no real compile/runtime cycle — but Eclipse/JDT (redhat.java) flattens every project dependency, main and test, into a single build-path graph. It therefore reports build-path cycles `solim-test ⇄ solim-core`, `solim-test ⇄ solim-runtime`, and the transitive `solim-test → solim-core → solim-runtime → solim-test`, and flags `mod`, `solim`, and `solim-mcp` as paths into those cycles. The result is a persistent IDE warning and degraded incremental build ordering.

## What Changes

- Make the shared test environment strictly layered by module dependency level, so a test-support module never depends on a module that depends back on it.
- Keep `ArcTestEnv` (Arc-only) and a runtime-level `SolimEnv` in `solim-test`, but drop `solim-test`'s dependency on `solim-core`; the `PendingCellConfig` configurator reset moves to a solim-core-owned test environment.
- Remove the unused `testImplementation project(':solim-test')` wiring from `solim-runtime` and `solim-mcp` — their tests never reference `solim.test`.
- Remove `mod`'s explicit `testImplementation project(':solim-runtime')`: mod tests call `SignalDispatcher.flush()` directly (frame-loop pump workaround); a `SolimEnv.flushEffects()` facade in `solim-test` replaces those calls, leaving solim-runtime invisible to mod test compilation and transitive at runtime only.
- Update the `test-env` capability spec to encode the acyclic-layering invariant and to describe the real consumption pattern (not "every testable module consumes the same env").
- No production code changes and no changes to shipped jar contents.

## Capabilities

### New Capabilities

- (none)

### Modified Capabilities

- `test-env`: Replace the blanket "every testable module consumes `solim-test`" requirement with an acyclic-layering invariant; redefine the layered envs as Arc → solim-runtime → solim-core so each layer is owned by the lowest module whose types it needs; correct the "all tests migrated" requirement to reflect that `solim-runtime`/`solim-mcp` tests do not use solim envs.

## Impact

- **Build**: `solim-test/build.gradle` (drop `solim-core` dependency), `solim-runtime/build.gradle` and `solim-mcp/build.gradle` (drop unused `solim-test` test dependency), `mod/build.gradle` (drop explicit `solim-runtime` test dependency).
- **Test sources**: `solim-test/src/solim/test/SolimEnv.java` (remove the `PendingCellConfig` reference; add `flushEffects()` facade); new solim-core-owned test env under `solim-core/src/test/java/solim/test/`; re-parent the solim-core tests that override the cell configurator; 4 mod tests swap `SignalDispatcher.flush()` for the facade.
- **Unaffected**: `solim`, `mod`, and all production code; the mod's `MindustryTestEnv` still extends the runtime-level `SolimEnv`.
- **Verification**: Gradle `test` for all modules (behavior unchanged), plus confirmation that the Eclipse build-path cycle warning no longer appears.