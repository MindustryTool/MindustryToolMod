## Context

Phase 2's capabilities are about *where* a property writes (element, table, cell, container). They say nothing about *whether the host honours it*:

| Property | Phase 2 requirement | Hosts that actually honour it |
| --- | --- | --- |
| `checked(Readable<Boolean>)` | `ElementCapability` | `Button`, `Checkbox`, `Switch` |
| `icon(Drawable)` | `ElementCapability` | `Button`, `Image`/`Icon` |
| `wrap(boolean)` / `ellipsis(boolean)` / `fontScale(float)` | `ElementCapability` | `Text`, a `Button` label, a `Checkbox` label, `TextField` |
| `text(String)` | `ElementCapability` | `Text` (sets label), `Button` (appends a child), `TextField` (placeholder) — **three meanings** |
| `style(...)` | `ElementCapability` | value-type dependent |

`style` is the hard case. `Values.style(@Nullable Object)` and `style(@Nullable Readable<?>)` accept anything, and the applier picks the widget by runtime type. `SolimButtonStyle` is `public final` and does not extend Arc's `com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle`; `Label.LabelStyle`, `TextField.TextFieldStyle`, `ScrollPane.ScrollPaneStyle`, and `List.ListStyle` have no common base either. So the style prop's capability is not expressible by value type alone.

Java has no union types, so "valid on `Button` or `Checkbox`" cannot be written directly. The resolution is a trait vocabulary: a marker per honoured-by set, implemented by exactly the hosts in that set.

There is a residue that is value-dependent rather than type-dependent — for example a style whose class must match the receiving widget. That cannot be checked at compile time and must fail loudly rather than reuse the current silent no-op.

## Goals / Non-Goals

**Goals:**

- Make "this host cannot honour this property" a compile error for every property whose validity is a host property.
- Type `.style(...)` per host family so a `LabelStyle` on a button and a `SolimButtonStyle` on a label are both compile errors, without changing `SolimButtonStyle` or Arc's style classes.
- Express the "A or B" case as a shared trait rather than a union.
- Make the value-dependent residue throw a descriptive error instead of silently doing nothing.

**Non-Goals:**

- Deleting chained configuration methods (last phase).
- A generic `Class<?>`-based capability (`requires(Button.class)`), which loses the compile-time guarantee and reintroduces raw types.
- Changing the style value objects or their builders.

## Decisions

### 1. Semantic capabilities are trait markers implemented by honoured hosts

```java
public interface TextCapability extends ElementCapability {}        // hosts whose text is content
public interface LabelCapability extends ElementCapability {}       // hosts that accept a Label.LabelStyle
public interface ButtonCapability extends ElementCapability {}      // hosts that accept a SolimButtonStyle
public interface CheckableCapability extends ElementCapability {}   // checked state
public interface WrappableCapability extends ElementCapability {}   // wrap/ellipsis/fontScale
public interface FieldCapability extends ElementCapability {}       // text input
public interface SliderCapability  extends ElementCapability {}
public interface SelectCapability  extends ElementCapability {}
public interface ScrollCapability  extends ElementCapability {}
public interface VideoCapability   extends ElementCapability {}
public interface IconCapability    extends ElementCapability {}
```

Each host implements the traits it honours. `Button`, `Checkbox`, and `Switch` implement `CheckableCapability`; `Text`, `Button`, `Checkbox`, and `TextField` implement `WrappableCapability`; `Text` implements `LabelCapability`; `Button` implements `ButtonCapability`; `TextField` implements `FieldCapability`.

- *Alternative — one marker per widget (`ButtonCapability`, `CheckboxCapability`, …) plus overloads everywhere*: `checked(...)` would need one overload per widget and would still not express "any checkable host". Rejected.
- *Alternative — `requires(Class<?> host)` runtime check*: loses the compile-time guarantee that this whole sequence exists to provide. Rejected.

### 2. `style` becomes a set of typed overloads, one per host family

`style(Label.LabelStyle)` / `style(Readable<Label.LabelStyle>)` → `Prop<LabelCapability, …>`; `style(SolimButtonStyle)` / reactive → `Prop<ButtonCapability, …>`; `style(TextField.TextFieldStyle)` → `Prop<FieldCapability, …>`; `style(ScrollPane.ScrollPaneStyle)` → `Prop<ScrollCapability, …>`; `style(List.ListStyle)` → `Prop<SelectCapability, …>`; `style(Slider.SliderStyle)` → `Prop<SliderCapability, …>`. The untyped `style(Object)`/`style(Readable<?>)` factories are removed.

Java overload resolution picks the overload from the static type of the argument, so `style(Styles.PRIMARY)` where `Styles.PRIMARY` is a `SolimButtonStyle` selects the button overload and its `ButtonCapability` requirement.

- *Alternative — keep one `style(Object)` and check the class at apply time*: cannot reject at compile time and keeps a cast by definition. Rejected.
- *Alternative — a single `style(Style)` with a Solim `Style` base*: `SolimButtonStyle` is `final` and Arc's style classes cannot be re-parented; introducing a Solim base would require wrapping every Arc style. Rejected.

### 3. `text(...)` is a trait property with host-dependent meaning

`text(...)` requires `TextCapability` (hosts where text is content — `Text`, `Button`, `TextField` placeholder). The three meanings are preserved; a host that has no text concept does not implement `TextCapability`.

- *Alternative — split into `content(...)` / `placeholder(...)`*: renames the existing prop and forces call-site churn for a naming preference; the trait already prevents misuse. Rejected.

### 4. The value-dependent residue fails fast

Where validity depends on the runtime class of the value rather than the host type (for example a style applied through a dynamically chosen overload), the property validates at application and throws `IllegalArgumentException` naming the host and the value class. Silent no-ops remain only for a genuinely absent value (`null`), never for a mismatch.

- *Alternative — keep the no-op*: the defect this sequence exists to remove. Rejected.

### 5. Trait markers extend the target markers

Every trait extends `ElementCapability`, so a trait host keeps satisfying the target-axis props and `props(...)` remains one call. Traits compose with the Phase 2 markers rather than replacing them: `GuideButton implements ElementCapability, ButtonCapability, CheckableCapability, ContainerCapability`.

## Risks / Trade-offs

- **Trait proliferation.** Every new family adds a marker. Mitigated by defining a trait only when a property set needs it, and by naming traits after the honoured property family, not the widget.
- **Overlapping styles.** A `Button` accepts a `SolimButtonStyle`, but its text label could accept a `LabelStyle` too. The button's own style prop requires `ButtonCapability`; a label-style prop for the inner label is a separate, later concern and is out of scope.
- **Overload ambiguity.** If two style overloads have the same erasure or a value is `null`, resolution can be ambiguous; a `null` style should be expressed with an explicit cast or a dedicated `cleared` factory. Documented, and tested.
- **`TextCapability` on `Button`** re-introduces the "`text` appends a child" behaviour into a prop; that is existing behaviour and is preserved, and Phase 4 keeps it fluent as content.

## Migration Plan

1. Add the trait markers; declare them on the hosts.
2. Add the typed `style(...)` overloads; delete `style(Object)`/`style(Readable<?>)`; retype `checked`/`icon`/`wrap`/`ellipsis`/`fontScale`/`text`/media props.
3. Build `solim-core` and `mod`; resolve each style call site to a typed overload (the compiler lists them).
4. Replace the remaining silent no-ops with fail-fast errors.
5. Add the rejection, multi-host-trait, and fail-fast tests.

Rollback: revert the change directory and its diff. The target-axis typing from Phase 2 remains; only the semantic markers and typed style overloads disappear.

## Open Questions

None.
