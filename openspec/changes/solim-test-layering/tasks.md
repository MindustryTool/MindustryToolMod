## 1. Remove dead test-support wiring

- [x] 1.1 Remove `testImplementation project(':solim-test')` from `solim-runtime/build.gradle`
- [x] 1.2 Remove `testImplementation project(':solim-test')` from `solim-mcp/build.gradle`
- [x] 1.3 Confirm `solim-runtime` and `solim-mcp` test sources contain no `solim.test` references

## 2. Make solim-test runtime-only

- [x] 2.1 Remove `implementation project(':solim-core')` from `solim-test/build.gradle`
- [x] 2.2 Remove the `PendingCellConfig` import and its `install()` call from `SolimEnv.resetAmbientState()`
- [x] 2.3 Add a `protected` reset hook to `SolimEnv`, invoked by its setup and teardown after the static ambient resets
- [x] 2.4 Verify `solim-test` compiles with only `solim-runtime`/`solim-api` on its project classpath

## 3. Add the solim-core env layer

- [x] 3.1 Create a solim-core-owned test env (e.g. `solim-core/src/test/java/solim/test/SolimCoreEnv.java`) extending `SolimEnv` that overrides the reset hook to call `PendingCellConfig.install()`
- [x] 3.2 Re-parent the solim-core tests that replace the default cell configurator (`ModifiersAndGapRegressionTest`, `SchedulerAndContextsRegressionTest`) to the core env
- [x] 3.3 Decide and apply whether the remaining solim-core tests also move to the core env for uniformity

## 4. Verification

- [x] 4.1 Run `gradlew :solim-test:test :solim-core:test :solim:test :mod:test` and confirm behavior parity
- [x] 4.2 Confirm the solim-core configurator restore still works (no configurator leak between tests)
- [x] 4.3 Run `gradlew check` (checkstyle) on touched modules
- [ ] 4.4 Reopen the workspace in Eclipse/JDT and confirm the `mod` build-path cycle warning is gone

## 5. Mod drops explicit solim-runtime dependency

- [x] 5.1 Add `public static void flushEffects()` to `SolimEnv` wrapping `SignalDispatcher.flush()`
- [x] 5.2 Replace direct `SignalDispatcher.flush()` calls with `flushEffects()` in `ChatMessageGrouperAndHeightTest`, `ChatChannelsAndMembersTest`, `QuickAccessClickDelegationTest`, `TeamResourcePositionTest`
- [x] 5.3 Remove `testImplementation project(':solim-runtime')` from `mod/build.gradle`
- [x] 5.4 Run `gradlew :mod:test` — confirm compile without runtime types and no behavior change