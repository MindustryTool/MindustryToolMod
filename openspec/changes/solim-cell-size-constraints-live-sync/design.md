## Context

In Solim UI, components such as `Button`, `Text`, `NetworkImage`, and `Card` implement `CellConfig` to configure parent layout cell constraints. Some components (notably `Button` and `Text` via `solim.UI`) attach immediately to their parent Table via `ParentStack.attachToParent()` during instantiation. Modifiers chained after instantiation (such as `.height(...)`, `.minWidth(...)`, `.margin(...)`) execute on an element that is already attached to its parent `Table`.

`ElementConfig.width()` / `height()`, `CellConfig.growX()` / `growY()`, and `CellConfig.margin()` already detect if the element is attached and actively update the live `Cell`. However, `CellConfig.minWidth()`, `minHeight()`, `maxWidth()`, and `maxHeight()` only recorded their values in `cellConfig()` without calling `applySizeToParentCell()`. Consequently, any min/max size constraints applied after attachment had zero effect on the live Arc `Cell`.

## Goals / Non-Goals

**Goals:**
- Guarantee that calling `minWidth()`, `minHeight()`, `maxWidth()`, and `maxHeight()` on an already-attached component immediately updates the parent Table's `Cell`.
- Support both static (`float`) and reactive (`Readable<Float>`) sizing overloads.
- Invalidate the layout hierarchy so parent containers (like `WrapTable` or `Table`) recalculate sizes immediately.
- Cover with unit tests in `solim-core`.

**Non-Goals:**
- Changing `PendingCellConfig` storage schema or moving classes.
- Altering Arc's underlying `Table` or `WrapTable` layout math.

## Decisions

### Decision 1: Immediate Application via `applySizeToParentCell`
In `CellConfig.java`, update:
- `minWidth(float)`
- `minWidth(Readable<Float>)`
- `minHeight(float)`
- `minHeight(Readable<Float>)`
- `maxWidth(float)`
- `maxWidth(Readable<Float>)`
- `maxHeight(float)`
- `maxHeight(Readable<Float>)`

Each static method will perform:
```java
if (this instanceof Component) {
    cellConfig().applySizeToParentCell(((Component) this).element());
}
```
And each reactive overload will register an `Effect` that calls `cellConfig().applySizeToParentCell(el)` whenever the readable changes:
```java
if (this instanceof Component) {
    Element el = ((Component) this).element();
    cellConfig().applySizeToParentCell(el);
    if (el != null) {
        Effect e = Effect.of(() -> cellConfig().applySizeToParentCell(el));
        ComponentContext.register(e);
    }
}
```
*Rationale*: This matches the existing pattern in `CellConfig.margin()` and `ElementConfig.width()`, providing uniform and reliable behavior across all modifiers.

## Risks / Trade-offs

- **[Performance / Re-layout Cost]** → Calling `applySizeToParentCell` triggers `parentTable.invalidateHierarchy()`. This is only called when size modifiers are chained or reactive signals change, which is necessary for correct layout display.
