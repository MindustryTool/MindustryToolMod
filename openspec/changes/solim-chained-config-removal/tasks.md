## 1. Inventory and missing prop vocabulary

- [ ] 1.1 Produce the full inventory of chained configuration setters per the boundary table, tagging each `prop` / `behaviour` / `content` / `getter`
- [ ] 1.2 Add the missing layout/value factories (`fillParent`, `columns` for `Grid`, and any element/table/cell setter without a prop yet)
- [ ] 1.3 Add the state/config factories (`checked` two-way, `selected`, `value`, `activeTab`, `progress`, `hideOnZero`, `placeholder`, `maxLength`, `fallback`, `url`, `scrollingDisabled`, `scrollX`, `scrollY`, `pinnedEdge`, `headerGap`, `tabStyle`, `textColor`)
- [ ] 1.4 Add the collapse factories (`collapsed`/`expanded` two-way, `duration`, `animated`)
- [ ] 1.5 Add the media factories (`autoplay`, `loop`, `volume`, `muted` two-way, `controls`, `playIcon`, `pauseIcon`, `replayIcon`, `volumeIcon`, `muteIcon`, `placeholder`, `fallback`, `scaling`, `rounded`)
- [ ] 1.6 Confirm each new factory declares its Phase 2/3 capability

## 2. Two-way state binding

- [ ] 2.1 Add the two-way overloads (`Signal<T>`) for user-togglable state, installing a `TwoWayBinding` owned by the host
- [ ] 2.2 Keep the one-way `Readable<T>` overloads for the same property so a read-only consumer is possible
- [ ] 2.3 Route `SolimVideo.muted` through its existing internal `mutedState` signal so the caller's signal and the internal state stay one source of truth
- [ ] 2.4 Confirm applied-once config (`autoplay`, `duration`, `animated`, icons) takes plain values only

## 3. Deprecate the setters

- [ ] 3.1 `@Deprecated` every configuration setter with a javadoc naming the replacement prop
- [ ] 3.2 Keep content (`Button.text`/`Button.icon`), behaviour (`onClick`, `items`, `show`, `mount`, `scrollTo`, …), and getters unchanged
- [ ] 3.3 Keep reconciler config (`ReactiveGrid`/`ForEach`/`VirtualList` `columns`/`key`/`items`/`overscan`/`empty`) fluent

## 4. Migrate mod

- [ ] 4.1 Migrate settings and dialogs packages to `props(...)`
- [ ] 4.2 Migrate chat packages
- [ ] 4.3 Migrate browser / schematic / map / content-patch packages
- [ ] 4.4 Migrate feature views and overlays (HUD, popup, quick access)
- [ ] 4.5 Apply the image `padding(...)` → `margin(...)` mapping deliberately
- [ ] 4.6 Leave Arc (non-Solim) `.color`/`.style`/`.background` calls untouched
- [ ] 4.7 Verify affected screens render identically before/after each package

## 5. Delete the setters

- [ ] 5.1 Delete every deprecated configuration setter
- [ ] 5.2 Build `solim-core` and `mod`; fix the stragglers the compiler reports
- [ ] 5.3 Confirm no configuration setter remains (grep the deleted names)

## 6. Tests

- [ ] 6.1 Migrate `solim-core` tests that call a deleted setter to `props(...)`
- [ ] 6.2 Add a test proving a two-way `collapsed`/`muted` prop writes back to the caller's signal
- [ ] 6.3 Add a test proving `Grid.columns(...)` as a prop reflows identically to the removed setter
- [ ] 6.4 Add a test proving reconciler `columns`/`key`/`items` still behave as specified (fluent, order-independent, terminal `children`)
- [ ] 6.5 Add a test proving applied-once props (`autoplay`, `duration`) take effect at build/apply

## 7. Specs and documentation

- [ ] 7.1 Replace the `solim-config-dsl` precedence requirement with the configuration-only requirement
- [ ] 7.2 Amend `solim-ui-dx` (all-configuration-via-props; content vs setter boundary)
- [ ] 7.3 Amend `solim-widgets` (`Text`, `Button`, `NetworkImage`, popup chrome)
- [ ] 7.4 Remove the `solim-styling` chained-card-binding requirement
- [ ] 7.5 Amend `solim-video` (props instead of chained setters)
- [ ] 7.6 Update `AGENTS.md`'s props/modifier guidance and `docs/solim_internals.md`
