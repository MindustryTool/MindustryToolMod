## Why

`solim.modifier.Prop` is fully type-erased: it carries a `Kind`, an `@Nullable Object value`, an `@Nullable Readable<?> source`, and a `Applier(@Nullable Object target, @Nullable Object value)`. Every factory in `Values` therefore recovers types by hand:

- numeric prop values are read back through a private `num(...)` helper plus `((Number) value).floatValue()`;
- reactive values are narrowed with `(Readable<Float>)`, `(Readable<Boolean>)`, `(Readable<Color>)`, `(Readable<?>)` casts;
- `Props.applyAlignCellRecord` dispatches `align`/`top`/`bottom`/`left`/`right`/`center` by comparing `prop.label()` strings and then casts the resolved value to `Integer`.

The result is roughly ninety unchecked casts across `solim.modifier`. A wrong value type is a `ClassCastException` at apply time or a silent no-op, never a compile error, and `Prop` carries no information a host could use to reject a property it cannot apply.

## What Changes

- **`Prop` becomes generic over its value**: `Prop<Requires, V>` where `V` is the value type and `Requires` is the host capability the property needs. The `Applier(Object, Object)` is replaced by a value-typed setter, and the `num(...)`/`(Readable<...>)`/`(Color)`/`(Integer)` casts are deleted.
- **A root capability marker is introduced now**: `solim.modifier.HostCapability`. Every Solim host implements it. In this phase every factory requires `HostCapability`, so nothing is rejected yet; the type parameter exists so later phases only swap the marker instead of changing every public signature.
- **Target dispatch becomes cast-free**: a property resolves its target through a typed host view (`withElement`, `withTable`, `withCell`) selected by its required capability, so neither the value nor the Arc target is cast.
- **Value-less properties get an explicit form** (`growX()`, `top()`, `center()`, …) instead of a `valueLess` boolean riding alongside a `null` value.
- **The label-string align dispatch is deleted** from `Props`; align properties carry their own value-typed applier.
- `PropsDslTest`, which currently pins the permissive type-erased behaviour, is rewritten to assert the typed contract.

This phase deliberately does **not** remove chained configuration methods, does not restrict which properties a host accepts at compile time, and does not add semantic (per-widget) markers. Those are the later phases.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `solim-config-dsl`: `Prop` becomes generic over its required capability and value type, and applying a property performs no unchecked cast of the value or the Arc target. The existing "Props targets any Arc type", capability-rejection, `children()`-relocation, precedence, and chained-method-removal requirements are untouched by this phase.

## Impact

- `solim-core`: `solim.modifier.Prop` (two type parameters, value-typed applier, valueless form, cast-free target dispatch), `solim.modifier.Props` (typed list, removed label dispatch), `solim.modifier.Configurable` (`props(Prop<? super SELF, ?>...)`), `solim.modifier.Values` (all ~60 factories typed; `num(...)` and the narrowing casts removed).
- `solim-core` tests: `solim/core/PropsDslTest` and any test asserting the erased `Prop` shape.
- Compatibility: call sites that declare a bare `Prop` variable become a compile error only where they use the erased type; `props(gap(2))`-style call sites are unaffected.
- No behaviour change: the phase is a typing change, so `width`/`height`/`size` remain `CELL` props, cell records still happen before attachment, and reactive props still install an `Effect` inside the host's ownership window.
