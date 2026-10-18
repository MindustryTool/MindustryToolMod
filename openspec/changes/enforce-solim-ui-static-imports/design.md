## Context

Currently, `solim-core` defines UI modifier factories in `solim.modifier.Values`. In `:mod`, code frequently imports static methods from `solim.modifier.Values` or references `solim.modifier.Values.<method>` directly. Furthermore, `AGENTS.md` previously instructed:
> "The factories come from solim.modifier.Values; import them explicitly (import static solim.modifier.Values.growX;). Do not rely on a wildcard solim.UI.* import to reach them..."

This conflicts with making `solim.UI` the sole high-level entry point for mod developers. By providing modifier methods in `solim.UI` and enforcing `import static solim.UI.*` via a Gradle check, mod code is kept clean, decoupled from internal modifier classes, and unified.

## Goals / Non-Goals

**Goals:**
- Expose all modifier methods from `Values` on `solim.UI` with direct delegation.
- Create a Gradle task (`checkSolimModifierImports`) to forbid `solim.modifier.Values` imports or explicit references in `:mod`.
- Migrate all `:mod` source files to use `import static solim.UI.*`.
- Update `AGENTS.md` and test guard assertions to match this architectural standard.

**Non-Goals:**
- Removing `Values.java` from `:solim-core` (it remains the implementation holder).
- Forbidding `Values` in `:solim-core` or `:solim` internal implementation where delegation is defined.

## Decisions

### 1. Re-exporting via `solim.UI` vs Subclassing
- **Decision**: Define static forwarder methods in `solim.UI` calling `Values.<method>`.
- **Rationale**: `solim.UI` is a utility facade class with a private constructor. Explicit static forwarding keeps contracts clean, prevents class hierarchy leaks, and maintains Java 8 runtime compatibility.

### 2. Gradle Verification Task Implementation
- **Decision**: Register `checkSolimModifierImports` in `build.gradle.kts` (or an included script) operating on `mod/src` and `mod/src/test/java`.
- **Detection**:
  - Regex scan stripping comments and string literals.
  - Reject `import (static )?solim\.modifier\.Values(\..*)?;`.
  - Reject token `\bValues\.`.
- **Rationale**: Follows the existing pattern of `checkNoFullyQualifiedNames` in `build.gradle.kts` and fails fast during `./gradlew check`.

### 3. Update `AGENTS.md` Guidelines
- **Decision**: Replace the section mandating explicit `solim.modifier.Values.*` imports with the requirement to use `import static solim.UI.*`.
- **Rationale**: Prevents AI pair-programming agents from reintroducing `Values` imports in future changes.

## Risks / Trade-offs

- **[Risk] Name collisions between `solim.UI` component factories and modifier helpers**:
  - *Mitigation*: Solim component helpers (e.g. `column()`, `row()`, `text()`, `button()`) and modifiers (e.g. `grow()`, `padding()`, `width()`, `size()`) have distinct method names and parameter types. Where ambiguity might exist with host methods (like in a class that already has a method named `left()`), standard Java static import shadowing rules apply.
- **[Risk] Large churn across mod source files**:
  - *Mitigation*: Systematic replacement of `import static solim.modifier.Values.<prop>;` with `import static solim.UI.*;` and removal of `solim.modifier.Values.` prefixes across `:mod`.
