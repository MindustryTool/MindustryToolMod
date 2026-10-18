## Why

Mod components currently mix multiple patterns for applying Solim layout and modifier properties: directly calling `solim.modifier.Values.<prop>()`, importing specific static methods from `solim.modifier.Values`, or importing `solim.UI.*`. This leaks Solim internal package structure (`solim.modifier.Values`) into mod code and produces inconsistent import styles. Enforcing `import static solim.UI.*` as the single declarative entry point in `:mod` eliminates direct references to `Values`, improves code consistency, and aligns with the project's declarative UI guidelines.

## What Changes

- Re-export all remaining modifier factory methods from `solim.modifier.Values` in `solim.UI`.
- Add a Gradle verification task `checkSolimModifierImports` that fails if `:mod` source or test files reference `solim.modifier.Values` or use explicit `Values.<prop>()` calls.
- Wire `checkSolimModifierImports` into the Gradle `check` lifecycle.
- Update `AGENTS.md` guidelines to instruct AI agents and developers to use `import static solim.UI.*` rather than static imports from `solim.modifier.Values`.
- Migrate existing `:mod` usages from `solim.modifier.Values` to `solim.UI.*`.

## Capabilities

### New Capabilities
- `solim-ui-static-imports`: Enforces the use of `import static solim.UI.*` for all Solim modifier and layout properties in `:mod`, disallowing direct references to `solim.modifier.Values`.

### Modified Capabilities
<!-- No existing spec-level requirement changes -->

## Impact

- `solim/src/solim/UI.java`: Extended to expose all modifier factories from `solim.modifier.Values`.
- `build.gradle.kts`: New verification task `checkSolimModifierImports` added and linked to `check`.
- `mod/src` and `mod/src/test/java`: Cleaned up to replace `solim.modifier.Values` imports and explicit `Values.` qualifications with `import static solim.UI.*`.
- `AGENTS.md`: Updated documentation rules.
