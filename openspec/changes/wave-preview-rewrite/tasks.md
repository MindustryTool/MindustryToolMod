## 1. Verification spikes

- [ ] 1.1 Confirm which event fires on wave advance and wire recompute to it alongside world load.
- [ ] 1.2 Verify the `getSpawned` index posture against live waves, including campaign multiplier truncation for bosses.
- [ ] 1.3 Confirm the `UnitType` flying/naval classification flags used for domain grouping.

## 2. Feature shell and parity computation

- [ ] 2.1 De-dev `WavePreviewFeature` into a real enable-capable feature defaulting to enabled, keeping id, icon, order, and QuickAccess membership.
- [ ] 2.2 Implement the parity composition computation (null/zero skips, campaign scaling, per-type accumulation) behind a reactive state holder.
- [ ] 2.3 Cover the parity computation (skips, scaling, accumulation) with tests.

## 3. Injected panel and refresh

- [ ] 3.1 Inject the panel into vanilla `waves/editor` → `waves` on enable and world load with defensive null handling and error logging.
- [ ] 3.2 Detach and dispose the panel on disable.
- [ ] 3.3 Gate visibility on `hudfrag.shown`, `isGame`, and `rules.waves`; recompute event-driven with no polling loop.

## 4. Domains, lookahead, and dialog

- [ ] 4.1 Render wave-major sections with ground/air/naval rows, unmatched units in ground, health-sorted icon×count rows.
- [ ] 4.2 Persist lookahead depth (default 1, max 5) and render that many consecutive wave sections.
- [ ] 4.3 Provide the minimal settings dialog with opacity, scale, and lookahead-depth rows bound to persisted entries.
- [ ] 4.4 Rewrite name/description/help bundle copy with translator comments, removing the cannot-be-enabled wording.
- [ ] 4.5 Update stub-registry and count-sensitive expectations for WavePreview leaving the development set.

## 5. Verification

- [ ] 5.1 Verify live-wave parity against the legacy formula across normal and campaign maps.
- [ ] 5.2 Verify injection, detachment, gating, depth changes, and restart persistence end to end.
- [ ] 5.3 Verify Java 8 runtime compatibility, `arc.util.Nullable` usage, no hardcoded display text, and `old/` untouched.
