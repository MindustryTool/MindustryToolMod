## 1. Core Reactive Primitives Refactoring

- [x] 1.1 Add `Effect.of(Readable<T>, Cons<T>)` static overload in `Effect.java` with test coverage
- [x] 1.2 Remove `listeners` list, `subscribe()`, and `listenerCount()` from `Signal.java`
- [x] 1.3 Remove `listeners` list, `subscribe()`, `listenerCount()`, and eager recomputation branch from `Computed.java`
- [x] 1.4 Delete `Subscription.java` and `Binding.java` in `solim-core`

## 2. Solim-Core Internal Migration

- [x] 2.1 Migrate internal `solim-core` usages of `Binding` in widgets and display (`Icon`, `NetworkImage`, `Badge`, `Button`, `SolimTextField`, `ElementConfig`) to `Effect.of(...)`
- [x] 2.2 Migrate `solim-core` tests in `test/java/solim/reactive/` and `test/java/solim/config/` removing `subscribe()` and replacing with `Effect.of(...)` or testing pure reactive observers
- [x] 2.3 Verify and update `ReflectAccessTest` or introspection tests in `solim-mcp` if impacted

## 3. Mod Call Sites Migration

- [x] 3.1 Migrate `Binding.*` usages in `mod` (`PrettyChatFeature`, `TranslationFeature`, `ModDesktopInput`) to `Effect.of(...)` or component modifiers
- [x] 3.2 Migrate feature lifecycle and settings `.subscribe(...)` calls across all features in `mod/src/mindustrytool/features/` to `Effect.of(...)`
- [x] 3.3 Migrate tests in `mod/src/test/java/mindustrytool/` using `.subscribe()` to `Effect.of(...)`

## 4. Verification and Validation

- [x] 4.1 Run `:solim-api`, `:solim-core`, and `:solim-runtime` unit and regression tests
- [x] 4.2 Run `:mod` test suite to verify no compile errors or regression in game mod features
- [x] 4.3 Verify no dangling references to `subscribe()` or `Binding` remain across the entire codebase
