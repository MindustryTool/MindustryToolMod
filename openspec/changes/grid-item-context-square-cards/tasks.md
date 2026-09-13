## 1. Grid Item Context Primitives in Solim

- [ ] 1.1 Create `GridItemContext` interface in `solim-core/src/solim/layout/GridItemContext.java` exposing `Readable<Float> itemWidth()` and `Readable<Integer> columnCount()`
- [ ] 1.2 Update `ReactiveGrid` in `solim-core` to track backing table width, compute usable cell `itemWidth`, and support `BiFunction<T, GridItemContext, Component>` item factory
- [ ] 1.3 Add overloaded builder methods to `solim/src/solim/UI.java` for `grid` and `reactiveGrid` taking `BiFunction<T, GridItemContext, Component>`

## 2. Card Components & Browser Integration

- [ ] 2.1 Update `SchematicCard` constructor to accept optional `Readable<Float> previewHeight` and bind preview container height to it
- [ ] 2.2 Update `MapCard` constructor to accept optional `Readable<Float> previewHeight` and bind preview container height to it
- [ ] 2.3 Update `SchematicBrowserDialog` to pass `ctx.itemWidth()` to `SchematicCard`
- [ ] 2.4 Update `MapBrowserDialog` to pass `ctx.itemWidth()` to `MapCard`

## 3. Verification & Testing

- [ ] 3.1 Run `./gradlew test` to verify unit and regression tests pass
- [ ] 3.2 Build mod desktop jar via `./gradlew jar`
