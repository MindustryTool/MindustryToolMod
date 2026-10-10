## 1. Descriptor Foundation

- [ ] 1.1 Create rule-descriptor type (id, category, control kind/range, typed `arc.func` getter/setter lambdas) in `mindustrytool.features.maprules`.
- [ ] 1.2 Build the static descriptor table covering all ~60 current rules with bundle-key convention `feature.map-rules.rule.<id>`.

## 2. Derive Behavior

- [ ] 2.1 Reimplement `RuleDiff` (predicates + modified counts) over the descriptor table and delete per-field methods.
- [ ] 2.2 Reimplement `RuleMutator` dispatch over descriptors and delete `setFloatField`/`setIntField`/`setBoolField` chains and `currentValueOf`/`currentIntOf` mirrors.
- [ ] 2.3 Reimplement `resetToDefaults` as a descriptor loop (snapshot + defaults branches, including ban-set restore).

## 3. Derive UI

- [ ] 3.1 Replace the five section methods with one generic section renderer.
- [ ] 3.2 Extract shared `ruleCard` wrapper; thin `sliderRow`/`intRow` skins over it.
- [ ] 3.3 Unify banned-block/unit grids and toggles over a generic content helper.

## 4. Verification

- [ ] 4.1 Verify compilation and checks via `./gradlew :mod:compileJava :mod:checkstyleMain checkNoFullyQualifiedNames`.
- [ ] 4.2 Verify bundles via `./gradlew checkBundles` (12/12 pass, 1102 keys each).
- [ ] 4.3 Verify in-game parity: labels, ordering, filters, editing modes, save flow, and reset (including bans).
