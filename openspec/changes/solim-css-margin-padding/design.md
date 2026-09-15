## Context

Solim is a declarative, reactive UI framework running on top of Arc (Mindustry's UI engine, derived from LibGDX Scene2D).
In Arc:
- `Table.margin(...)` / `Table.pad(...)` sets the inner insets of the container table (space inside the border).
- `Cell.pad(...)` sets the spacing of the cell containing an element (space outside the element's border, pushing neighboring cells away).

In Solim's current design:
- `TableConfig` declares `padding(...)` as an alias for `margin(...)`, both manipulating inner table margins.
- `CellConfig` declares `cellPadding(...)` to manipulate `Cell.pad(...)` (outer margins).

Because developers expect the standard CSS box model (`margin` = outside, `padding` = inside), having `.cellPadding()` for outer spacing and `.margin()` for inner spacing creates constant confusion and bugs.

## Goals / Non-Goals

**Goals:**
- Provide standard CSS box-model semantics across Solim components:
  - `.margin(...)`: sets outer spacing around the element (delegating to parent `Cell.pad(...)`).
  - `.padding(...)`: sets inner spacing inside the element/container (delegating to Arc `Table.margin(...)`).
- Maintain 100% backward compatibility by keeping `.cellPadding(...)` on `CellConfig` as a deprecated forwarder to `.margin(...)`.
- Resolve method resolution on layout containers (`Row`, `Column`, `Card`, `Grid`) so `.margin(...)` operates on outer parent cell and `.padding(...)` operates on inner table padding.
- Support `PendingCellConfig` in `Popup.render()` so root popup content can configure margin/padding.

**Non-Goals:**
- Re-engineering Arc's layout engine to support vertical margin collapsing or `auto` margins.
- Breaking existing code that relies on `padding(...)` on `Row`, `Column`, `Card`, etc.

## Decisions

### Decision 1: Define `margin(...)` on `CellConfig`
`CellConfig` is the mixin for elements configuring their parent cell.
We add:
- `default SELF margin(float m) { return margin(m, m, m, m); }`
- `default SELF margin(float top, float left, float bottom, float right)`
- `default SELF margin(Readable<Float> m)`
- `default SELF margin(Readable<Float> top, Readable<Float> left, Readable<Float> bottom, Readable<Float> right)`
- `default SELF marginTop(...)`, `marginBottom(...)`, `marginLeft(...)`, `marginRight(...)`, `marginX(...)`, `marginY(...)`

These write directly to `cellConfig().padTop`, `padLeft`, `padBottom`, `padRight` and call `applyMarginToParentCell()`.

### Decision 2: Disambiguate containers implementing both `CellConfig` and `TableConfig`
Classes like `Row`, `Column`, `Card`, `Grid`, and `Button` implement both `CellConfig` and `TableConfig`.
If both interfaces provide default methods for `margin(...)`, the compiler requires an explicit override.
We explicitly implement `margin(...)` in these classes to delegate to `CellConfig` (outer margin).
In `TableConfig`, `margin(...)` is marked `@Deprecated` in favor of `padding(...)`.
Result:
```java
row()
    .margin(unit(2))   // CSS margin: outer space outside the row
    .padding(unit(3))  // CSS padding: inner space inside the row
```

### Decision 3: Deprecate `cellPadding(...)`
`cellPadding(...)` in `CellConfig` is retained with `@Deprecated` annotation and delegates directly to `margin(...)`:
```java
@Deprecated
default SELF cellPadding(float p) {
    return margin(p);
}
```

### Decision 4: Enable `PendingCellConfig` application in `Popup`
In `solim.overlay.Popup.render()`, instead of just `table.add(content.element())`, check `PendingCellConfig.find(content)` and apply it to the created cell:
```java
Cell<?> cell = table.add(content.element());
PendingCellConfig config = PendingCellConfig.find(content);
if (config != null) {
    List<Disposable> effects = config.applyToCell(cell);
    for (Disposable effect : effects) {
        ComponentContext.register(effect);
    }
}
```

## Risks / Trade-offs

- **[Risk] Existing code calling `.margin()` on a Table expected inner padding**
  $\rightarrow$ *Mitigation*: In the current codebase, `.padding()` is already the idiomatic Solim way used for containers (`.padding(unit(1))`). Code using `.cellPadding()` will continue to work seamlessly via deprecated forwarders.
- **[Risk] Compiler ambiguity on dual-interface implementors**
  $\rightarrow$ *Mitigation*: Explicit implementations on `Row`, `Column`, `Card`, `Grid`, and `Button` resolve any interface default method conflicts unambiguously.
