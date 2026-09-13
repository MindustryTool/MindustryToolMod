## Context

The Solim modifier system currently has two layers:
- `ElementConfig` (858-line static utility) — handles Element mutations, Table operations, and Element→Cell bridging
- `CellConfig` (mixin interface) — handles parent-cell configuration only

The static utility pattern means callers must pass `element` as the first argument, creating verbose call sites. The single class conflates three distinct concerns (Element ops, Table ops, Cell bridging). The Element overloads of `margin`/`padding` dispatch via `instanceof Table`, mixing two concepts in one method.

`SizeConstraints` holds pending configuration for parent cells but is named as if it only constrains size.

## Goals / Non-Goals

**Goals:**
- Split ElementConfig into three mixin interfaces by target type: Element, Table, Cell
- Rename SizeConstraints → PendingCellConfig
- Remove Element-overload dispatch (instanceof Table checks)
- CellConfig removes methods now provided by ElementConfig/TableConfig (no duplication)
- Maintain backward compatibility for mod-facing CellConfig fluent API

**Non-Goals:**
- Changing the mod-facing API (CellConfig methods on Row, Column, Card stay the same)
- Changing ParentStack behavior
- Refactoring the reactive system
- Changing GapContainer or gap spacing logic
- Keeping deprecated static wrappers — all static ElementConfig methods are deleted entirely

## Decisions

### D1: Three mixin interfaces instead of static utilities

**Decision**: ElementConfig, TableConfig, and CellConfig are all mixin interfaces with default methods.

**Why**: Mixins provide fluent chaining without requiring callers to pass `element` as the first argument. Each component implements `element()` or `table()` and gets all methods automatically. This matches the existing CellConfig pattern.

**Alternative**: Keep static utilities. Rejected because the existing 858-line static utility is exactly the problem we're solving.

### D2: CellConfig keeps only parent-cell unique methods

**Decision**: CellConfig removes width/height/size, opacity, rounded/border/background, and alignment. Components get these from ElementConfig or TableConfig directly.

**Why**: No duplication. Each mixin provides its own domain. CellConfig only has what's unique to parent-cell configuration: grow, min/max, cellPadding.

**Alternative**: CellConfig keeps overlapping methods and delegates to ElementConfig. Rejected — unnecessary indirection when the component already implements ElementConfig.

### D3: Remove Element-overload dispatch

**Decision**: Delete `margin(Element, float)`, `padding(Element, float)`, etc. that check `instanceof Table`.

**Why**: These methods conflate two concepts: Table-level margin and parent Cell padding. Callers should use TableConfig for Tables or CellConfig.cellPadding for parent-cell padding. The bridge is unnecessary indirection.

**Impact**: ~18 method deletions. Callers in solim-core re-pointed to TableConfig or CellConfig.

### D4: Rename SizeConstraints → PendingCellConfig

**Decision**: Rename to PendingCellConfig.

**Why**: Accurately describes what it is: pending configuration for the parent Cell. "Constraints" implies restrictions; this is a buffer of values to apply later.

### D5: PendingCellConfig keeps applyToCell + applyGrow/Margin/AlignToParentCell

**Decision**: Remove `applySizeToParentCell` (ElementConfig handles this). Keep `applyToCell` (ParentStack entry point) and `applyGrow/Margin/AlignToParentCell` (called from CellConfig methods for immediate application when element is attached).

**Why**: ElementConfig.width/height already handle parent Cell size when the element is attached. PendingCellConfig doesn't need to duplicate this. But grow, cellPadding, and alignment have no ElementConfig equivalent — PendingCellConfig handles them.

## Risks / Trade-offs

- **[Risk]** Breaking internal solim-core call sites → **Mitigation**: All ~180+ ElementConfig static calls are re-pointed. Build verification catches misses.
- **[Risk]** Confusion during migration (old names vs new names) → **Mitigation**: Delete old files, rename all references in one pass.
- **[Trade-off]** More interfaces (3 instead of 1 class) → **Benefit**: Clearer responsibility boundaries, smaller files, easier navigation.
- **[Trade-off]** CellConfig no longer has width/height/opacity/rounded/border/background → **Mitigation**: Components implement ElementConfig to get these methods. No behavior change, just different source interface.
