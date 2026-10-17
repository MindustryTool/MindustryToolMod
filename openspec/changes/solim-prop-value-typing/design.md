## Context

`Prop` is the single currency of the `props(...)` DSL. Today it is a `final class` with no type parameters:

```java
public final class Prop {
    public enum Kind { ELEMENT, TABLE, CELL }
    public interface Applier { void apply(@Nullable Object target, @Nullable Object value); }

    private final Kind kind;
    private final String label;
    private final Applier applier;
    private final @Nullable Object value;
    private final @Nullable Readable<?> source;
    private final boolean valueLess;
}
```

`Prop.element/table/cell` take `Cons2<Element, Object>`, `Cons2<Table, Object>` and `Cons2<PendingCellConfig, Object>` setters and each does its own target cast (`(Element) t`, `(Table) t`, `(PendingCellConfig) t`). `Props.targetFor` maps `Kind` to `host.element()/table()/cellConfig()` and returns `@Nullable Object`. `Props.applyAlignCellRecord` switches on `prop.label()` strings and casts `prop.resolve()` to `Integer`.

The consequences:

- **Value locality is lost.** `Values` must narrow every reactive source and every numeric value by hand. Wrong usage (a `String` where a `Float` is expected) is not a compile error.
- **The label is load-bearing.** `align`/`top`/`bottom`/`left`/`right`/`center` are recognised by string equality in `Props`, so the `label` argument is behaviour, not diagnostics.
- **`valueLess` is a parallel flag.** Value-less properties (`growX`, `top`, …) carry `null` plus a boolean.

Capability restriction is explicitly *not* part of this change; that is the next phase. This phase introduces the type parameter so the later phase does not have to change every public factory twice.

Java 8 constraints apply: no `java.util.function`, `arc.func` only, `arc.util.Nullable` only, no `@SafeVarargs` on interface default methods.

## Goals / Non-Goals

**Goals:**

- Give `Prop` a value type so a factory cannot produce a property whose value does not match its setter.
- Give `Prop` a required-capability type so the following phase can constrain hosts without another public signature change.
- Remove the casts in `Values`/`Props`/`Prop` and the label-string align dispatch.
- Preserve the observable behaviour of `props(...)`: cell records before attachment, reactive `Effect` inside the host window, last-write-wins ordering.

**Non-Goals:**

- Compile-time rejection of a property a host cannot honour (next phase).
- Removing chained configuration methods (last phase).
- Semantic per-widget markers such as "`icon` is only valid on a `Button`/`Image`" (third phase).
- A public "rendered size" or unit-conversion API.

## Decisions

### 1. `Prop<Requires, V>` with a root marker introduced now

`Prop` gains two type parameters: `Requires` (a host capability) and `V` (the value type). This phase declines to restrict anything, so every factory returns `Prop<HostCapability, V>` where `HostCapability` is a new empty marker interface in `solim.modifier` implemented by every Solim host.

- *Alternative — ship `Prop<V>` and widen later*: every one of the ~60 factory return types changes twice, and every mod field declared as `Prop` changes twice. Rejected.
- *Alternative — skip the capability parameter and rely on runtime checks*: does not satisfy the goal of rejecting unsupported properties at compile time in the next phase. Rejected.

Because `props(...)` is bounded by `Prop<? super SELF, ?>`, a `Prop<HostCapability, V>` is accepted by every host (`SELF <: HostCapability`), which is exactly "nothing is restricted yet".

### 2. Value-typed applier, cast-free target dispatch

`Applier` becomes value-typed and the target is resolved through a typed host view rather than `Object`:

```java
public final class Prop<Requires extends HostCapability, V> {
    interface Setter<V> { void set(Element e, @Nullable V v); }   // one per capability slot
    // construction picks the slot from Requires; application calls the slot's typed hook
}
```

Concretely, the prop stores a capability-derived slot (`ELEMENT`/`TABLE`/`CELL`/`CELL_SOURCE`) and the *element/table/cell* is handed to the slot by the host view (`withElement(Cons<Element>)`, `withTable(Cons<Table>)`, `withCell(Cons<PendingCellConfig>)`). The value is never an `Object`, and the Arc target is never cast: the host view's own typed getters do the switch.

- *Alternative — keep `Kind` and cast the target inside `Prop`*: one cast per prop, still ~40 casts, and no compile-time proof the cast is valid. Rejected.
- *Alternative — sealed class hierarchy (`ElementProp`, `TableCellProp`, …)*: duplicates the ownership/window plumbing four times for no behavioural gain. Rejected in favour of one class plus a slot strategy.

### 3. Value-less properties get their own form

`growX()`, `growY()`, `grow()`, `top()`, `bottom()`, `left()`, `right()`, `center()`, `draggable()` and friends are `Prop<Requires, Void>` produced by a dedicated valueless factory; the `boolean valueLess` field and the "`null` value that is not a null value" convention are deleted.

- *Alternative — `Prop<Requires, Boolean>` with `Boolean.TRUE`*: every valueless property allocates a boxed `Boolean` and the value type lies about the payload. Rejected.
- *Alternative — a separate `Flag` type*: a second currency in `props(...)`, so `props(growX(), gap(2))` needs a common supertype. Rejected; `Void` keeps one currency.

### 4. `Props` carries a typed list; the label dispatch is deleted

`Props<T>` holds `List<Prop<? super T, ?>>`. Applying a property calls `prop.apply(hostView)`; the capture is resolved inside `Prop`, so no unchecked cast is needed outside. `Props.applyAlignCellRecord` and its `"align".equals(label)` ladder are removed because each align property now records its own typed cell value.

`Prop.label` stays as a diagnostic string (used by the profiler and error messages) but is no longer behaviour.

- *Alternative — keep the label switch for the cell pre-record path*: the align props must record into `PendingCellConfig` before attachment, but a value-typed align applier can do that directly; the switch exists only because the erased prop could not. Rejected.

### 5. `Values` factories are retyped mechanically

Each factory's setter is retyped and the narrowing casts disappear. `num(...)`, `(Readable<Float>)`, `(Readable<Boolean>)`, `(Readable<Color>)`, `(Readable<?>)` and the `(Integer)` align cast are deleted. Factories keep their current names and argument shapes so call sites do not change.

## Risks / Trade-offs

- **Public generic arity change.** Any code that declares a raw `Prop` or a `Prop[]` variable breaks. Only `solim-core` internals and `solim-core` tests currently do; the mod's call sites use inline `props(...)` arguments. Migration is compile-enforced; a raw type is already forbidden by `AGENTS.md`.
- **`props(...)` needs one suppressed unchecked warning.** `Prop<? super SELF, ?>...` cannot use `@SafeVarargs` on a Java 8 interface default method, so `Configurable.props` carries a single `@SuppressWarnings("unchecked")` with a comment naming the Java 8 varargs rule as the reason. This is the documented exception `AGENTS.md` permits.
- **Wildcard capture in `Props`.** Storing `Prop<? super T, ?>` means application happens inside `Prop`, not in a loop that reads the value. The design keeps the loop in `Props` and the value inside `Prop` so no capture leaks into `solim-core` call sites.
- **`Void` boxing.** `Prop<Requires, Void>` has a `null` payload; the slot must not skip it (that is what `valueLess` did). The slot contract explicitly distinguishes "no payload" from "null payload" by never consulting the payload for a valueless property.
- **Test churn.** `PropsDslTest` asserts the permissive erased behaviour; it is rewritten to assert that a property applies the value the factory typed, and that a valueless property still applies.

## Migration Plan

1. Add `solim.modifier.HostCapability` and make every host implement it (no behaviour change).
2. Add the type parameters and value-typed setter slot to `Prop`; introduce the typed host view; keep `Prop.element/table/cell/cellSource/raw` only as `solim-core`-internal bridges during the rewrite.
3. Retype every `Values` factory; delete `num(...)` and the narrowing casts; repoint the align props at the typed cell record.
4. Update `Configurable.props` to `Prop<? super SELF, ?>...` with the single documented suppression; make `Props` hold the typed list.
5. Rewrite `PropsDslTest`; run the `solim-core` suite.

Rollback: revert the change directory and its diff; the DSL surface (`props(gap(2))`) is unchanged, so no call site migration is entangled.

## Open Questions

None.
