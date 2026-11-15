## 1. State & Tracking Enhancements

- [ ] 1.1 Update `TeamResourceState` to track ever-existed core items in `everSeenItems`, preserving items with count `0` when exhausted instead of removing them.
- [ ] 1.2 Implement compact 3-significant-digit number formatting (`formatCompactAmount`) supporting values up to millions with uniform width.
- [ ] 1.3 Implement fixed-size `float[60]` RingBuffer for resource amounts and power metrics, updated at 1Hz with zero steady-state heap allocations.
- [ ] 1.4 Update rate calculation logic to differentiate positive (+), negative (-), and stagnant (zero) states with appropriate color and suppression.

## 2. Power Bar Consolidation

- [ ] 2.1 Refactor `SplitBar` into a unified battery bar displaying stored energy fraction (`stored / capacity`) partitioned across active power graphs.
- [ ] 2.2 Format power segment text as `<Stored> ± <Net>` with dynamic discharge color feedback (`Pal.remove` when balance < 0).
- [ ] 2.3 Enforce adaptive text suppression when power sub-bar segments are narrower than text label width plus padding.

## 3. Sparkline Tooltip Component

- [ ] 3.1 Create `SparklineWidget` drawing min/max bounds and 60-second line graphs via `Lines.line` and zero per-frame allocation.
- [ ] 3.2 Implement hover tooltip for resource items displaying item name, exact amount, rate, and 60-second sparkline.
- [ ] 3.3 Implement hover tooltip for power bar segments displaying network details, stored energy, net balance, and 60-second sparkline.

## 4. Minimalist HUD Overhaul

- [ ] 4.1 Refactor `TeamResourceHudView` item list to remove card wrappers, borders, and margins, placing items closely in a flow-wrapped layout.
- [ ] 4.2 Render rate indicator as a reduced-size subscript at the bottom-right of the amount label, hidden when stagnant.
- [ ] 4.3 Refactor unit list to remove card borders and position unit count labels at the bottom-right of unit icons.
- [ ] 4.4 Replace separated power metrics with the single unified `SplitBar` in `TeamResourceHudView`.

## 5. Verification & Tests

- [ ] 5.1 Add unit tests covering compact number formatting, zero-rate suppression, and ever-seen item retention.
- [ ] 5.2 Add unit tests for RingBuffer recording, history bounds, and zero heap allocation during sampling.
- [ ] 5.3 Execute `./gradlew build` to verify clean compilation, Solim encapsulation, and test suite passage.
