## 1. Expose Modifier Methods in solim.UI

- [ ] 1.1 Review all public static methods in `solim.modifier.Values`
- [ ] 1.2 Add corresponding delegating static methods in `solim.UI` for missing layout and styling modifiers (sizing, margins, wrap, borders, rounded, tooltips, click handlers, styling)
- [ ] 1.3 Verify `:solim` compiles and tests pass

## 2. Gradle Verification Rule

- [ ] 2.1 Implement `checkSolimModifierImports` task in `build.gradle.kts`
- [ ] 2.2 Wire `checkSolimModifierImports` into `tasks.named("check")` and `:mod:check`
- [ ] 2.3 Verify `checkSolimModifierImports` flags existing `Values` usages in `:mod`

## 3. Migrate Mod Usages

- [ ] 3.1 Replace explicit `Values.<method>()` and `solim.modifier.Values.<method>()` references in `mod/src` with bare method calls
- [ ] 3.2 Replace `import static solim.modifier.Values.<method>;` and `import solim.modifier.Values;` with `import static solim.UI.*;` across `mod/src`
- [ ] 3.3 Replace any `Values` references in `mod/src/test` with `solim.UI.*`
- [ ] 3.4 Run `./gradlew checkSolimModifierImports` and confirm no violations in `:mod`

## 4. Documentation & Encapsulation Guards

- [ ] 4.1 Update `AGENTS.md` to instruct using `import static solim.UI.*` instead of explicit `solim.modifier.Values` imports
- [ ] 4.2 Update `SolimEncapsulationGuardTest` if needed to maintain boundary assertions
- [ ] 4.3 Run full `./gradlew check` and verify all tests and verification tasks pass
