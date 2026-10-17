## Why

After Phase 2 a property is checked against the *target* it writes, not against the host's *semantics*. `checked(Readable<Boolean>)` requires `CellCapability`/`ElementCapability`, so it compiles on any element; on a host with no checked state it is a silent no-op. `icon(...)` compiles on a `Divider`. `.style(...)` already exists as an untyped prop — `style(@Nullable Object)` and `style(@Nullable Readable<?>)` — so a `Label.LabelStyle` passed to a button, or a `TextField.TextFieldStyle` passed to an image, is accepted and then either no-ops or is applied to the wrong widget.

The style case is the concrete driver: `SolimButtonStyle` is `public final` and does **not** extend Arc's `ButtonStyle`, and `Label.LabelStyle`, `TextField.TextFieldStyle`, `List.ListStyle`, and `ScrollPane.ScrollPaneStyle` share no common base. A single `style(Object)` prop therefore cannot be typed by value alone.

Java cannot express "valid on `Button` *or* `Checkbox`", so the semantic axis needs a trait vocabulary that hosts implement, plus a runtime fail-fast for the residue that depends on a value rather than a host type.

## What Changes

- **Semantic trait capabilities**: marker interfaces that hosts implement when they honour a family of properties — for example `TextCapability`, `LabelCapability`, `ButtonCapability`, `CheckableCapability`, `WrappableCapability`, `FieldCapability`, `SliderCapability`, `SelectCapability`, `ScrollCapability`, `VideoCapability`, `IconCapability`. A property valid on several widget families requires the trait they share (e.g. `checked(...)` requires `CheckableCapability`, implemented by `Button`, `Checkbox`, and `Switch`), so the "A or B" case becomes "the trait both implement" instead of a union.
- **`.style()` becomes a typed prop per host family**: `style(Label.LabelStyle)` requires `LabelCapability`, `style(SolimButtonStyle)` requires `ButtonCapability`, `style(TextField.TextFieldStyle)` requires `FieldCapability`, and so on, replacing the untyped `style(Object)`/`style(Readable<?>)`.
- **Semantic props are typed**: `checked`, `icon`, `wrap`, `ellipsis`, `fontScale`, `text`, `selected`, and the media props gain their trait capability instead of a generic element/cell capability.
- **The residue fails fast**: a property whose validity depends on the *value* rather than the host type throws a descriptive `IllegalArgumentException` at application time instead of silently no-op'ing.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `solim-config-dsl`: semantic trait capabilities restrict a property to the hosts that honour it, and a value-dependent mismatch fails fast instead of no-op'ing.
- `solim-styling`: style application is declared as a capability-typed prop (`style(LabelStyle)`, `style(SolimButtonStyle)`, …) rather than an untyped `style(Object)`; the static and reactive style requirements are restated in prop terms.

## Impact

- `solim-core`: new trait markers in `solim.modifier`; host `implements` clauses gain their traits; `Values` gains typed `style(...)` overloads and retypes `checked`/`icon`/`wrap`/`ellipsis`/`fontScale`/`text`; the `style(Object)`/`style(Readable<?>)` factories are removed.
- `mod`: call sites passing a style through the untyped prop must resolve to a typed overload; call sites using `style(...)` on a host that does not declare the trait become compile errors.
- Tests: a rejection test per trait (e.g. `style(LabelStyle)` on a button), a multi-host trait test (`checked` on `Button`/`Checkbox`/`Switch`), and a fail-fast test for the value-dependent residue.

## Non-Goals

- Removing chained configuration methods — the last phase. During this phase the chained `.style(...)`, `.checked(...)`, `.wrap(...)` methods still exist.
- Replacing `SolimButtonStyle` or making Arc style classes share a base.
