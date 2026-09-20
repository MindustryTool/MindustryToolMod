# stack-direct-layers Specification

## Purpose
TBD - created by archiving change browser-card-parity-fix. Update Purpose after archive.
## Requirements
### Requirement: Direct-attach stack layers without Row wrapper
`SolimStack` SHALL provide Component-returning layer methods named `layer`: `layer(Supplier<Component>)` for single-root layers and `layer(Function<Element, Component>)` as the returning parent-capture form. Each method SHALL build the returned component in an isolated `ParentStack` context, attach its element via `Stack.add`, track it as an owned layer, and return the `SolimStack` for chaining. `SolimStack` SHALL NOT expose `layer(Runnable)`, `children(Runnable)`, `children(Cons<Element>)`, or `childrenComponent(...)`; `children` SHALL remain the ambient multi-child builder only on non-stack containers, so the framework-wide `children(Runnable)` convention is preserved everywhere except `SolimStack`.

#### Scenario: Single-root layer attaches directly
- **WHEN** `stack().grow().layer(() -> networkImage(url).rounded(8))` executes inside an active parent
- **THEN** the `Stack` contains the image element directly with no intermediate `Row` table element between the `Stack` and the image

#### Scenario: Parent-capture layer reads parent before returning
- **WHEN** `stack().layer(parent -> divider().update(div -> div.setWidth(parent.getWidth() / 2)))` executes
- **THEN** the layer function receives the `Stack` element, the returned divider is attached directly, and width updates track the provided parent

#### Scenario: Null layer is a no-op
- **WHEN** `layer((Supplier<Component>) null)` or a supplier returning `null` executes
- **THEN** no child is added and no exception is thrown

#### Scenario: Legacy layer and children methods are removed
- **WHEN** the public API of `SolimStack` is inspected
- **THEN** it SHALL NOT declare `layer(Runnable)`, `children(Runnable)`, `children(Cons<Element>)`, or `childrenComponent(...)`, and only `layer(Supplier<Component>)` / `layer(Function<Element, Component>)` remain

### Requirement: Stack-owned layer disposal
`SolimStack` SHALL own every component attached through `layer(Supplier<Component>)` and `layer(Function<Element, Component>)` and dispose each owned layer exactly once when the stack is disposed; second dispose SHALL be safe and SHALL NOT throw or double-dispose.

#### Scenario: Dispose releases layers once
- **WHEN** a stack with two returning layers is disposed twice
- **THEN** each layer component receives exactly one `dispose()` call and the second `dispose()` is a no-op

#### Scenario: Exception-safe layer build
- **WHEN** a layer supplier throws during isolated build
- **THEN** no partial child remains attached to the `Stack` and the exception propagates with `ParentStack` restored to its prior size

