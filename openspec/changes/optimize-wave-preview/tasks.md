## 1. Core Model & Calculation

- [ ] 1.1 Update `WaveSectionData` to include `public final float totalHealth`
- [ ] 1.2 Update `WavePreviewCalculator` to accumulate `totalHealth` across all domains during wave computation
- [ ] 1.3 Add unit tests in `WavePreviewCalculatorTest` covering total wave HP calculation for mixed domains, bosses, and empty waves

## 2. Reactive State Management

- [ ] 2.1 Add transient `Signal<Boolean> collapsed` and `toggleCollapsed()` method with lookahead depth guard to `WavePreviewState`
- [ ] 2.2 Ensure collapsed state resets on `clear()` and world load in `WavePreviewState` and `WavePreviewFeature`

## 3. UI Presentation & Internationalization

- [ ] 3.1 Add localized format bundle keys for inline wave HP in `assets/bundles/bundle.properties` with translator comments
- [ ] 3.2 Implement compact number formatting helper for HP values (`k`/`M` suffixes)
- [ ] 3.3 Update `WavePreviewPanelView` to render inline wave HP on section headers and omit HP on empty waves
- [ ] 3.4 Wire click listener on `WavePreviewPanelView` to trigger collapse toggle and conditionally render only the first wave when collapsed

## 4. Verification & Testing

- [ ] 4.1 Run Gradle test suite (`:mod:test`) to verify all calculator and wave preview tests pass
- [ ] 4.2 Verify Solim encapsulation guard (`SolimEncapsulationGuardTest`) and bundle completeness
