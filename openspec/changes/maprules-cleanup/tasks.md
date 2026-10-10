## 1. Source Fixes

- [ ] 1.1 Replace NUL separators in `MapRulesView.viewKey()` with visible delimiters.
- [ ] 1.2 Collapse `bannedBlocksGrid` search to a single pass over content.
- [ ] 1.3 Delete `RuleMutator.sync()`, `RuleMutator.cancelPending()`, and the `retainContentFields` call in `captureSnapshot()`.

## 2. Verification

- [ ] 2.1 Verify compilation and checks via `./gradlew :mod:compileJava :mod:checkstyleMain checkNoFullyQualifiedNames`.
- [ ] 2.2 Verify bundles via `./gradlew checkBundles` (12/12 pass, 1102 keys each).
- [ ] 2.3 Verify in-game: search results, ban chips, reset (including bans), and save flow behave identically.
