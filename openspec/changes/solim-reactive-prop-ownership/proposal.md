## Why

Reactive configuration inside `solim-core` flows through two different paths. A capability-typed prop is applied by the engine inside the host's ownership window (`Prop.elementSource` / `Prop.tableSource`, wrapped by `Props.applyAll`). A non-reactive `Prop.raw` prop instead hands a `Readable` to a chained `setX(Readable)` method, and that method opens its own ownership window with `withinOwnership(...)` and installs the `Effect` by hand.

The second path undermines three accepted requirements at once:

- `solim-config-dsl` requires configuration to be declared only through props, yet `setX(Readable)` survives as a second, competing configuration API.
- `solim-component-ownership` forbids a manual ownership mutation API on `Component`, yet the chained setters and component constructors reach for `withinOwnership` / `openSelfWindow`.
- `solim-lifecycle` requires a component's constructor to allocate plain values only, yet `Badge`, `Hud` and `Text(Readable)` create owned resources while their constructors run.

The same shape also lets a value be declared twice, producing two live bindings that silently fight over one target — a second source of truth, which the project forbids.

## What Changes

- Convert every reactive prop in `Values` to the engine-owned form (`Prop.elementSource` / `Prop.tableSource`), so the binding is created by the props window and owned by the host: `text`, `drawable`/`src`, `url`, `controls`, `columns`, `enabled`, `checked`, `disabled`, `collapsed`, `expanded`, and the reactive `style` variants.
- **BREAKING**: delete the chained `setX(Readable)` methods the props replace — `Text.setText`, `SolimImage.drawable`, `SolimVideo.setUrl` / `setControls`, `Grid.setColumns`, `Button.setEnabled` / `setChecked`, `SolimTextField.setDisabled`, `SolimCollapser.setCollapsed` / `setExpanded`.
- Remove the dead `Button.setStyle(Readable)`; `Button.setStyle(Cons)` stays as a fluent content action but no longer opens a window.
- Replace the ~16 hand-rolled `OwnershipContext.push(...) + Effect.of(...)` blocks inside `Values` with reactive prop definitions.
- **BREAKING**: a reactive value is declared exactly once. There is no replacement or cancellation of a live binding; a value that changes over time is a `Signal`. Duplicate non-reactive props keep last-wins.
- **BREAKING**: delete the manual ownership window API `Component.withinOwnership`, `Component.openSelfWindow` and `Component.closeSelfWindow`. A constructor declares props (the host opens its own window) or uses a self-adopting primitive, never a window.
- `Component.listen(...)` adopts its registration to its own component; a component's constructor-time children are owned by the component's realization window.
- `Badge` implements `TextCapability` and becomes lazy so `build()` owns its internal label.
- Delete the per-component binding fields that existed only to serve implicit replacement (`SolimVideo.urlBinding` / `controlsBinding`).

## Capabilities

### New Capabilities

None. The ownership rules extend existing capabilities.

### Modified Capabilities

- `solim-config-dsl`: reactive props become the only path that installs a binding; a prop-created binding is owned by the host; a reactive value is declared once and is not replaced by a later declaration.
- `solim-component-ownership`: `Component` exposes no ownership window API at all, and constructor-time resources are owned by the component's own realization window or a self-adopting primitive.
- `solim-lifecycle`: the constructor rule is restated so constructors declare props (host window) and use self-adopting primitives instead of opening a window.
- `solim-widgets`: reactive text and reactive image content bind through the prop path rather than a reactive setter.
- `solim-video`: reactive `url` and `controls` are props-owned bindings; the component holds no binding field.

## Impact

- **solim-core**: `solim.modifier.Values` and `Props`, `solim.core.Component`, `solim.display.Text`, `solim.input.Button` / `SolimTextField`, `solim.layout.Grid` / `SolimCollapser`, `solim.display.SolimImage`, `solim.display.video.SolimVideo`, `solim.display.Badge`, `solim.display.Hud`.
- **solim**: `solim.UI` factories that currently reach a reactive constructor or content path (`text`, `image`, `icon`, `collapser`) are re-expressed through props.
- **mod**: no call sites use the deleted Solim setters; mod-owned `Feature.setEnabled` is unaffected.
- **Tests**: three solim-core tests use `withinOwnership` and are migrated.
- **Sequencing**: this change is written now and implemented **after** `unify-media-sources`, so the media sites target the post-merge `SolimImage` / `src(...)` / `ImageCapability` / `VideoSource` surface rather than `NetworkImage`.
