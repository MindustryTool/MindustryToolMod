# solim-lifecycle Specification

## Purpose

Mechanical merge of 12 specs per change `spec-domain-merge` (stage 3 core, concat-then-dedupe). Sources: solim-component, basecomponent-ui-separation, lifecycle-correctness, ownership-encapsulation, scoped-context-api, solim-automatic-ownership, solim-component-auto-attach, solim-core-split, solim-dynamic, solim-runtime-encapsulation, structural-reconciler, unified-ownership-api. Each source below appears under a `**Source:` marker with its purpose body and requirement blocks verbatim; per-source `## Purpose` / `## Requirements` header lines are removed so all requirements parse inside the single `## Requirements` section. TBD purposes carried forward; requirement dedupe is follow-up work.
## Requirements

**Source: solim-component**

### Requirement: Component interface
The framework SHALL provide `solim.core.Component` with `arc.scene.Element element()`, `default void dispose()`, and `default Component name(String name)` to rename the component's underlying Arc `Element`.

#### Scenario: Component returns Element
- **WHEN** `Component c = new MyComponent()` and `Element e = c.element()` is called
- **THEN** `e` is the Arc `Element` built by the component (non-null after build)

#### Scenario: Default dispose is no-op
- **WHEN** a `Component` does not override `dispose()`
- **THEN** calling `dispose()` does not throw and is safe

#### Scenario: Default name sets element name
- **WHEN** `Component c = new MyComponent()` and `c.name("custom-name")` is called
- **THEN** `c.element().name` equals `"custom-name"` and `c` is returned

### Requirement: BaseComponent single-build semantics
`BaseComponent` SHALL be an abstract class implementing `Component` with lazy `build()` semantics: `protected abstract Element build()` runs exactly once on first `element()` call, result is cached, and subsequent `element()` calls return cached instance. Components SHALL NOT automatically re-render.

#### Scenario: Build runs once
- **WHEN** `BaseComponent comp = new Counter()` then `comp.element()` is called twice
- **THEN** `build()` is invoked exactly once and both calls return same `Element` instance

#### Scenario: No automatic re-render on signal change
- **WHEN** a `Signal` inside a `BaseComponent` changes
- **THEN** `build()` is NOT called again; only reactive bindings/effects mutate existing `Element` properties

### Requirement: Constructor props and normal Java composition
Custom components SHALL be normal Java classes with constructor parameters as props; no React-style props map or hooks SHALL be required. Components may hold `Signal`, `Computed`, `Effect` as fields.

#### Scenario: Constructor props
- **WHEN** `UserCard card = new UserCard(Signal<User> user)` with `user` field stored
- **THEN** `card.build()` can read `user.get()` or `user.map(User::name)` without framework injecting props

#### Scenario: Signal fields inside component
- **WHEN** `Counter` holds `Signal<Integer> count = Signal.of(0)` and `Computed<String> text = count.map(v -> "Count: " + v)` and `build()` returns `button(text, () -> count.set(count.get()+1))`
- **THEN** clicking button updates `text` via reactive binding without rebuilding `Counter`

### Requirement: Component as child via ElementResolver
Layouts and parents SHALL accept children that are `Element` or `Component`; `Component` children SHALL be automatically resolved to `component.element()` without requiring callers to write `new Header().element()`. Components instantiated inside a `children()` block SHALL be automatically attached to the current parent via the pending attachment mechanism, and a component is never moved between ownership contexts to make it attach.

#### Scenario: Direct Component child without component() call
- **WHEN** `column(() -> { new Header(); new ChatPanel(); })` where `Header` and `ChatPanel` extend `Component`
- **THEN** parent column contains their resolved `Element`s in order, without any `component()` call

#### Scenario: Mixed Element and Component children
- **WHEN** `column(() -> { text("Hello"); new UserCard(user); image(tex); })`
- **THEN** all three children are attached correctly without requiring manual `.element()` calls

#### Scenario: Explicit component() still valid
- **WHEN** `column(() -> { component(new Header()); })`
- **THEN** the element is attached exactly once; no duplicate child is added

### Requirement: Component lifecycle and explicit disposal
Components SHALL follow lifecycle `constructor → build() → element mounted → dispose()` . Components owning `Effect` or `Subscription` SHALL dispose them in overridden `dispose()`. Framework SHALL NOT introduce complex ownership/scope hooks; lifecycle is explicit.

#### Scenario: Effect disposed with component
- **WHEN** `ChatPanel` creates `Effect effect = Effect.of(() -> Log.info(messages.get().size))` in `build()` and overrides `dispose() { effect.dispose(); }`
- **THEN** after `panel.dispose()` the effect no longer reacts to `messages` changes

#### Scenario: BaseComponent dispose default
- **WHEN** `BaseComponent` without overrides is disposed
- **THEN** no exception and no resource leak beyond its cached `Element` reference

### Requirement: No React hooks or re-render loop
The framework SHALL NOT expose React-style hooks (`useState`, `useEffect`) nor a component re-render/reconciliation loop. Keep components as plain classes.

#### Scenario: No hooks API
- **WHEN** `solim.core` package is inspected
- **THEN** it contains no `useState`, `useEffect`, `useMemo`, or JSX-like APIs

### Requirement: Default element naming for Solim components
Every concrete Solim component SHALL automatically assign a default name to its underlying Arc `Element` upon construction, adhering to the format `solim-<component>-<internal>`. If a developer explicitly calls `.name(String name)`, the custom name SHALL completely overwrite the default name.

#### Scenario: Default name assigned on construction
- **WHEN** a Solim component such as `Button`, `Column`, `Row`, `Card`, or `Text` is instantiated without calling `.name()`
- **THEN** its `element().name` is populated with the default name conforming to `solim-<component>-<internal>` (e.g. `solim-button-sizedButton`, `solim-column-table`, `solim-card-cardButton`)

#### Scenario: Explicit name completely overwrites default name
- **WHEN** a Solim component is instantiated and `.name("custom-name")` is invoked
- **THEN** its `element().name` equals `"custom-name"` without preserving the default prefix

### Requirement: BaseComponent default naming
`BaseComponent` SHALL assign a default name formatted as `solim-<component>-<internal>` (derived from the lowercase class simple name and the built element's simple name/role) to its built element if no custom `name(...)` has been assigned before or after `build()`.

#### Scenario: BaseComponent uses default name when unset
- **WHEN** a subclass of `BaseComponent` is built and no custom name is set
- **THEN** its `element().name` is formatted as `solim-<subclass>-<internal>`

#### Scenario: BaseComponent preserves custom name override
- **WHEN** a subclass of `BaseComponent` has `.name("my-card")` called
- **THEN** its `element().name` equals `"my-card"`

**Source: basecomponent-ui-separation**

### Requirement: BaseComponent has no UI attachment logic
`BaseComponent` SHALL NOT import or reference `AttachmentStack`, `Table`, or any Arc layout type for the purpose of UI placement. It SHALL NOT call `AttachmentStack.registerPendingComponent(...)` or any equivalent in its constructor.

#### Scenario: BaseComponent constructor has no UI side effects
- **WHEN** `new MyComponent()` is called outside any `children()` block
- **THEN** no UI element is attached to any parent and no `AttachmentStack` state is mutated

#### Scenario: element() builds without mounting
- **WHEN** `component.element()` is called directly
- **THEN** the Arc Element is built and returned but NOT attached to any scene parent

### Requirement: UI attachment is driven by the composition layer
All UI attachment (adding an Element to a Table/parent) SHALL happen through `Ui.*` factory methods or the explicit `AttachmentStack` API in the composition layer. Application components SHALL obtain their Element via `element()` or `element.add(component.element())`.

#### Scenario: Ui.* factory attaches element to parent
- **WHEN** a Solim factory like `column()` is called inside a `children()` block
- **THEN** the resulting Element is attached to the current parent via `AttachmentStack`

#### Scenario: Component created manually requires explicit attachment
- **WHEN** `new MyComponent()` is called and then `table.add(component.element())`
- **THEN** the element is attached at that point, not at construction time

**Source: lifecycle-correctness**

### Requirement: Disposal order is reversed
Resources owned by a `BaseComponent` SHALL be disposed in the order: the `onDispose()` hook first, then child components, then the component's own non-component resources. Child components SHALL be disposed before the component's own resources so that a reactive resource cannot recompute against an element its child has already disposed. Within each of those groups, resources SHALL be disposed in reverse registration order. Each disposal SHALL be attempted independently so that a failure in one does not prevent others from being disposed.

#### Scenario: onDispose runs while the tree is intact
- **WHEN** a component with children is disposed
- **THEN** the `onDispose()` hook executes before any child is disposed

#### Scenario: LIFO disposal order within a group
- **WHEN** a component owns resources A, B, C in that order and no children
- **THEN** on `dispose()`, C is disposed first, then B, then A

#### Scenario: Children dispose before own resources
- **WHEN** a component owns a child component and a `Computed` that reads from that child
- **THEN** the child is disposed first and the `Computed` is disposed afterwards

#### Scenario: Error isolation during disposal
- **WHEN** one disposable throws during `dispose()`
- **THEN** the error is logged via `Log.err` and remaining disposables continue to be disposed

#### Scenario: Disposable list is cleared after disposal
- **WHEN** `dispose()` completes
- **THEN** the internal disposable list is empty

### Requirement: Disposed component cannot be rebuilt
A `BaseComponent` that has been disposed SHALL throw `IllegalStateException` if `element()` is called.

#### Scenario: element() after dispose() throws
- **WHEN** `element()` is called on a disposed component
- **THEN** `IllegalStateException` is thrown with a message identifying the component class

### Requirement: Double disposal is safe
Calling `dispose()` more than once on any `Disposable` in `solim` and `solim-*` packages SHALL be a no-op on subsequent calls, and `isDisposed()` SHALL return `true` once disposed. This extends the previous `BaseComponent`-only guarantee to every covered type, including leaf components that previously inherited the default `false`.

#### Scenario: Second dispose() call
- **WHEN** `dispose()` is called a second time
- **THEN** no exception is thrown, no resources are disposed again, and `isDisposed()` returns `true`

### Requirement: Partial builds are cleaned up on failure
If `build()` throws, all resources registered with the component up to that point SHALL be disposed.

#### Scenario: Exception during build() cleans up
- **WHEN** `build()` throws a `RuntimeException` after registering some resources
- **THEN** `dispose()` is called, those resources are released, and the exception propagates to the caller

#### Scenario: OwnershipContext is always popped on build failure
- **WHEN** `build()` throws
- **THEN** `OwnershipContext.pop()` is still called (the context stack is not corrupted)

### Requirement: build() returning null is detected
If `build()` returns `null`, the component SHALL throw `IllegalStateException` with a descriptive message.

#### Scenario: Null return from build()
- **WHEN** `build()` returns `null`
- **THEN** `IllegalStateException` is thrown and the component's resources are disposed

**Source: ownership-encapsulation**

Confine raw ownership primitives to framework internals so application modules use only lifecycle-safe declarative paths.

### Requirement: No manual ownership API on the mod path
Application components SHALL have no callable manual ownership method. `OwnershipContext.register()/registerChild()` is the canonical internal path; `listen()`/`createSignal()`/`effect()` are the canonical mod-facing lifecycle-safe paths.

#### Scenario: Guard test rejects manual ownership references
- **WHEN** the ownership guard test scans `:mod` sources
- **THEN** zero references to `own(` or `ownChild(` exist outside comments, and the test fails otherwise

**Source: scoped-context-api**

### Requirement: Ambient Lifecycle Ownership in Component Build
The Solim framework SHALL automatically track child components, disposables, and Solim controls instantiated or attached while a component is the active ambient owner — inside its `build()` or inside a `children()` block — without requiring explicit ownership invocations. Ambient ownership SHALL be the only ownership path for application code and framework code alike. A component's constructor SHALL allocate plain values only, because the component is not the active ambient owner while its own constructor runs.

#### Scenario: Child component created in build context
- **WHEN** a component instantiates child components or Solim controls within its `build()` method
- **THEN** the child components and controls are automatically registered with the parent component's disposable registry and disposed when the parent is disposed

#### Scenario: Child component created in a children block
- **WHEN** a component instantiates child components or Solim controls within its own `children()` block
- **THEN** the children are registered against that component, not against the enclosing scope

#### Scenario: Constructor allocates plain values only
- **WHEN** a component's constructor body runs
- **THEN** the component is not the active ambient owner, so any owned resource it would create belongs to that resource's own construction site rather than the component

#### Scenario: Clean stack unwinding on build failure
- **WHEN** a component's `build()` method throws an exception during construction
- **THEN** the ambient build context stack is cleanly unwound and does not leak context to subsequent component builds

#### Scenario: No manual ownership call exists to misuse
- **WHEN** framework or application code needs to bind a resource to the current component
- **THEN** no ownership method can be called on the component, because ownership is expressed solely by the ambient context the resource is created in

### Requirement: OwnershipContext is the canonical internal registration path
Framework code outside `solim.core` that must own a resource SHALL use `OwnershipContext.register(disposable)` or `OwnershipContext.registerChild(child)` instead of calling `own()` directly. These methods SHALL be null-safe, pause-aware, and delegate to the ambient owning component.

#### Scenario: Layout registers an effect internally
- **WHEN** a layout creates an `Effect` outside `solim.core` that must live with the owning component
- **THEN** it passes the effect to `OwnershipContext.register(...)` and the effect is disposed with the component

### Requirement: Automatic Control and Binding Registration
Solim controls and reactive property bindings declared inside a component build SHALL automatically register their subscriptions and event listeners with the enclosing component.

#### Scenario: SolimTextField declared in build
- **WHEN** a `SolimTextField` is created during a component's `build()` without wrapping in `own(...)`
- **THEN** its internal signal and event bindings are automatically bound to the enclosing component's lifecycle and disposed upon component disposal

#### Scenario: Reactive property binding declared in build
- **WHEN** a property binding is created during a component's `build()`
- **THEN** the subscription is registered with the current component and cancelled upon component disposal

**Source: solim-component-auto-attach**

`BaseComponent` instances created inside an active `AttachmentStack` scope auto-attach their element to the current parent, making `component()` unnecessary for the common in-`children()` usage.

### Requirement: BaseComponent auto-attaches to active parent
When a BaseComponent subclass is instantiated inside an active AttachmentStack scope (i.e., inside a children() block), it SHALL automatically schedule its element for attachment to the current parent container without requiring an explicit component() call. The deprecated component() wrapper function SHALL be removed from the public API.

#### Scenario: BaseComponent auto-attaches in children block
- **WHEN** column(() -> { new ChatMessageListView(store, service); }) is called
- **THEN** the ChatMessageListView element is attached to the column table without any component() call

#### Scenario: BaseComponent in nested children blocks
- **WHEN** 
ow(() -> { text("Label"); new MyCard(data); }) is called inside a column(() -> { ... })
- **THEN** MyCard's element is attached to the inner 
ow, not the outer column

#### Scenario: No auto-attach outside children scope
- **WHEN** a BaseComponent is instantiated outside any AttachmentStack scope (e.g., stored as a field before uild() runs)
- **THEN** no element attachment occurs; the element is only attached when instantiated inside a parent's children() block or attached via parent resolver

**Source: solim-core-split**

Specifies the architectural separation of the Solim UI framework into `:solim-core` (internal framework engine) and `:solim` (public API facade).

### Requirement: Solim Core Module Separation
The Solim UI framework SHALL be split into two Gradle subprojects: `:solim-core` and `:solim`.
`:solim-core` SHALL contain all internal reactive engines, component base classes, layout implementations, widget primitives, style definitions, overlay managers, unit conversions, and structural reconcilers.
`:solim` SHALL contain solely the user-facing public API facade class `solim.UI` (`UI.java`).

#### Scenario: Build solim-core independently
- **WHEN** `:solim-core:build` is executed
- **THEN** all core Solim implementation classes and tests compile and pass without relying on `:solim`

#### Scenario: Solim facade module contains only UI.java
- **WHEN** the source directory of `:solim` is inspected
- **THEN** only `solim.UI` (`UI.java`) is present under its source directory

### Requirement: Mod Dependency Constraint
The `:mod` Gradle subproject SHALL depend exclusively on `:solim` for Solim UI functionality and SHALL NOT declare a direct dependency on `:solim-core`. All UI component instantiation and declarative operations in `:mod` SHALL use `solim.UI`.

#### Scenario: Mod build dependencies
- **WHEN** `mod/build.gradle` dependencies are inspected
- **THEN** `project(':solim')` is declared as an implementation dependency and `project(':solim-core')` is not present

#### Scenario: Mod compiles without direct solim-core dependency
- **WHEN** `:mod:compileJava` is executed
- **THEN** compilation succeeds using only `project(':solim')` without compile-time errors

### Requirement: Complete Public UI Facade
`solim.UI` in `:solim` SHALL provide all user-permitted static factory methods for Solim UI operations. This SHALL include:
- Layouts: `column()`, `row()`, `grid()`, `reactiveGrid()`, `card()`, `scroll()`, `stack()`, `wrap()`, `collapser()`, `divider()`, `spacer()`
- Widgets: `button()`, `text()`, `textField()`, `slider()`, `checkbox()`, `switchToggle()`, `select()`, `image()`, `icon()`, `networkImage()`, `badge()`
- Overlays: `dialog()`, `hud()`, `popup()`
- Reactivity: `signal()`, `computed()`, `effect()`, `createSignal()`
- Structural & Components: `dynamic()`, `when()`, `forEach()`, `virtualList()`, `arc()`
- Units & Events: `unit()`, `dvw()`, `dvh()`, `listen()`
The keyed-collection factories `reactiveGrid(items)`, `forEach(items)`, and `virtualList(items, heightProvider)` SHALL accept only fundamental data, with configuration applied through fluent methods and the required item factory supplied through terminal `void` `children(...)`. The facade SHALL NOT expose `badgeCount()`, `container()`, `divider(String)`, `divider(char)`, or no-argument `icon()`. The legacy duplicate facade `solim.core.Ui` SHALL NOT exist; `solim.UI` is the sole public facade. Internal helpers such as `isExpanding` (now `SolimToken.isExpandingChild`), `element`, and `add` SHALL NOT be exposed on `solim.UI`.

#### Scenario: UI methods instantiation
- **WHEN** UI elements, signals, layouts, or widgets are created in user code
- **THEN** all methods are accessible statically via `solim.UI`

#### Scenario: Reactivity factory methods on UI
- **WHEN** a user creates a signal or computed value via `UI.signal(value)` or `UI.computed(supplier)`
- **THEN** the corresponding `Signal<T>` or `Computed<T>` instance from `solim-core` is instantiated and returned

#### Scenario: Single facade available
- **WHEN** a consumer inspects the Solim modules for a public UI facade
- **THEN** only `solim.UI` provides factory methods and no `solim.core.Ui` type exists

#### Scenario: Structural facade includes when
- **WHEN** a consumer needs conditional rendering
- **THEN** `UI.when(condition, supplier)` is available alongside `UI.dynamic(...)`

#### Scenario: Removed facade methods unavailable
- **WHEN** a consumer attempts to use `badgeCount`, `container`, string/char `divider`, or no-argument `icon`
- **THEN** the methods do not exist and the consumer uses `badge(Readable<String>)`, `column()`, `divider(Direction)`, and `image()` instead

### Requirement: API Dependency Propagation
The `:solim` subproject SHALL declare an `api` dependency on `:solim-core` using the `java-library` plugin (or equivalent `api` configuration) so that public return types and chained modifier methods (e.g. `Column`, `Button`, `Signal<T>`, `Readable<T>`, `Component`) are accessible to consumer modules without consumers having to declare `:solim-core` directly.

#### Scenario: Chained modifiers in consumer
- **WHEN** a consumer in `:mod` calls `UI.column().grow().gap(UI.unit(2))`
- **THEN** the chained methods compile cleanly on the consumer classpath

**Source: solim-dynamic**

### Requirement: Dynamic value equality guard
The system SHALL skip rebuilding its child component when the source signal emits a value that is equal to the previous value, and SHALL cleanly manage parent cell sizing constraints across collapse and expand transitions. The factory function SHALL be invoked for all distinct source values including null.

#### Scenario: Same value does not trigger rebuild
- **WHEN** the source signal emits a value that is `Objects.equals()` to the current value
- **THEN** the existing child component is preserved without disposal or recreation

#### Scenario: Different value triggers rebuild
- **WHEN** the source signal emits a value that is not equal to the current value
- **THEN** the existing child component is disposed and a new one is created from the factory

#### Scenario: First emission triggers build
- **WHEN** the source signal emits for the first time (no previous value)
- **THEN** the child component is created from the factory

#### Scenario: Null source value invokes factory
- **WHEN** the source signal emits null and the previous value was not null
- **THEN** the factory function is invoked with null as its argument

#### Scenario: Factory returning null collapses parent cell
- **WHEN** the factory returns null (for a null or non-null source value)
- **THEN** the container is hidden (`visible = false`), the parent cell size is collapsed to `0f`, and padding is set to `0f`

#### Scenario: Factory returning component expands parent cell with unconstrained bounds
- **WHEN** the factory transitions from returning null to returning a non-null Component (for a null or non-null source value)
- **THEN** the container becomes visible (`visible = true`), parent cell min and max size constraints are restored to unconstrained sentinel (`Float.NEGATIVE_INFINITY`), size constraints configured on the Dynamic component are re-applied, and the layout hierarchy is invalidated

### Requirement: Nested dynamic subtree lifecycle
The system SHALL support nesting `Dynamic` components within other `Dynamic` components, ensuring that inner subtrees update independently while the outer condition is stable, and cascade full disposal when the outer condition switches or collapses.

#### Scenario: Inner dynamic switches while outer condition remains stable
- **WHEN** an inner `Dynamic` component's source signal updates while the enclosing outer `Dynamic` condition has not changed
- **THEN** the inner dynamic disposes its previous child component and builds the new child component without re-evaluating or recreating the outer component

#### Scenario: Outer condition switch or collapse cascades disposal to nested dynamic subtree
- **WHEN** an outer `Dynamic` source signal changes to a different value or collapses to null
- **THEN** the active inner `Dynamic` component, its active child, and all registered effects in the inner subtree are disposed recursively

#### Scenario: Mutating inner signal after outer branch collapse does not trigger factory
- **WHEN** the inner source signal updates after the enclosing outer branch has been collapsed or switched away
- **THEN** the inner dynamic factory SHALL NOT be invoked and no new elements or listeners are created

#### Scenario: Re-expanding outer condition recreates active nested dynamic subtree
- **WHEN** the outer `Dynamic` source signal transitions from collapsed/null back to an active state
- **THEN** a fresh inner `Dynamic` component is instantiated, bound to the current inner source signal, and renders the current state

#### Scenario: Simultaneous update to outer and inner signals resolves deterministically
- **WHEN** both outer and inner signals are mutated prior to a signal dispatcher flush
- **THEN** if the outer condition evaluates to active, the newly mounted inner dynamic reflects the updated inner signal; if the outer condition evaluates to collapsed, the outer container collapses and the inner factory is not invoked for the collapsed state

**Source: solim-runtime-encapsulation**

Physically isolate the Solim engine runtime from application modules at compile time while preserving automatic UI mounting.

### Requirement: Physical compile-time isolation of Solim runtime
The framework SHALL place all internal engine classes (`AttachmentStack`, `OwnershipContext`, `ReactiveContext`, `SignalDispatcher`, `StructuralReconciler`, `ElementResolver`, `Binding`, `Ui`) inside the `:solim-runtime` subproject under package `solim.runtime.*`. The `:solim-runtime` subproject SHALL NOT be on `:mod`'s compile classpath.

#### Scenario: Mod referencing AttachmentStack fails compilation
- **WHEN** application code in `:mod` imports or references `solim.runtime.AttachmentStack` or any class from `solim.runtime.*`
- **THEN** compilation fails with a missing symbol or package error (`package solim.runtime does not exist`)

#### Scenario: Mod referencing OwnershipContext fails compilation
- **WHEN** application code in `:mod` imports or references `solim.runtime.OwnershipContext`
- **THEN** compilation fails with a missing symbol or package error

### Requirement: Strict Directed Acyclic Graph across Solim subprojects
The framework subprojects SHALL follow a strict DAG dependency hierarchy:
- `:solim-api` depends on no other Solim module.
- `:solim-runtime` depends only on `:solim-api`.
- `:solim-core` depends on `:solim-api` (via `api`) and `:solim-runtime` (via `implementation`).
- `:solim` depends on `:solim-core` (via `api`) and `:solim-runtime` (via `implementation`).
- `:mod` depends on `:solim` and `:solim-mcp`.

#### Scenario: Zero circular dependencies in Gradle
- **WHEN** `./gradlew projects` or `./gradlew build` runs
- **THEN** Gradle resolves all task graphs without circular dependency warnings or errors

### Requirement: Preservation of automatic UI mounting
Application components extending `BaseComponent` SHALL retain automatic UI mounting when instantiated within container `children(...)` blocks without requiring explicit wrapper calls.

#### Scenario: Component constructed in children block auto-attaches
- **WHEN** `new MyCustomView()` is called inside a `UI.column(() -> { ... })` block
- **THEN** the component's element is automatically attached to the enclosing container Table

**Source: structural-reconciler**

### Requirement: Shared structural reconciler
A shared reconciler utility SHALL exist (e.g., `StructuralReconciler`) that implements the keyed child lifecycle: create new keys, preserve existing keys (reuse component), remove missing keys (dispose component), and mount/unmount elements in the container. The reconciler SHALL execute each factory invocation inside an `OwnershipScope` and adopt every resource the factory registers into the component the factory returns.

#### Scenario: New key creates a new component
- **WHEN** reconciliation runs and a new key is present that was not in the previous set
- **THEN** a new component is created via the factory and its element is added to the container

#### Scenario: Existing key reuses component
- **WHEN** reconciliation runs and a key is present in both the previous and current set
- **THEN** the existing component is reused (not rebuilt) and remains in the container

#### Scenario: Removed key disposes component
- **WHEN** reconciliation runs and a key from the previous set is absent from the current set
- **THEN** the corresponding component is disposed and its element is removed from the container

#### Scenario: Order changes move elements without rebuilding
- **WHEN** reconciliation runs and the order of existing keys changes
- **THEN** existing components are not rebuilt; only their elements are reordered in the container

#### Scenario: Resources created by a factory are disposed with its component
- **WHEN** an item factory constructs a `Computed` and returns a component, and the key is later removed
- **THEN** both the component and the `Computed` are disposed, and the `Computed` is detached from its sources

#### Scenario: Factory failure disposes partially created resources
- **WHEN** a factory registers resources and then throws during reconciliation
- **THEN** the resources registered by that invocation are disposed and the previously active components are preserved

### Requirement: Dynamic, ForEach, and ReactiveGrid delegate to StructuralReconciler
`Dynamic`, `ForEach`, and `ReactiveGrid` SHALL share the same child-lifecycle implementation provided by `StructuralReconciler`. They SHALL NOT independently duplicate create/preserve/remove/dispose logic.

#### Scenario: ForEach uses StructuralReconciler
- **WHEN** `ForEach` reconciles a new list
- **THEN** it delegates to `StructuralReconciler` for lifecycle management

#### Scenario: ReactiveGrid uses StructuralReconciler
- **WHEN** `ReactiveGrid` reconciles a new data set
- **THEN** it delegates to `StructuralReconciler` for lifecycle management

### Requirement: Reconciler untracked reactive isolation
The `StructuralReconciler` SHALL instantiate new components and build their initial elements inside an untracked reactive context (`ReactiveContext.untracked`) so that signal accesses during child component initialization do not leak into outer collection observers.

#### Scenario: Child component creation does not register dependencies with ambient effect
- **WHEN** a collection observer effect triggers reconciliation and new items are constructed that read reactive signals during build
- **THEN** those signal reads are not recorded as dependencies of the collection observer effect

**Source: unified-ownership-api**

### Requirement: Component extends Disposable
The `Component` interface SHALL extend `Disposable` so that components can be passed directly to `own()` and similar ownership APIs.

#### Scenario: Component is Disposable
- **WHEN** a class implements `Component`
- **THEN** it compiles as a `Disposable` and can be passed to any method accepting `Disposable`

### Requirement: Legacy ownership APIs are removed or deprecated
`registerDisposable(...)`, `ownChild(...)`, `own(...)`, and any other ownership alias method SHALL be removed outright. There SHALL be no deprecation shim and no replacement method.

#### Scenario: No ownership method resolves
- **WHEN** any module attempts to call `own(...)`, `ownChild(...)`, or `registerDisposable(...)`
- **THEN** compilation fails with a missing symbol, and the author binds the resource by creating it where the component is the active ambient owner

#### Scenario: No deprecation warning stands in for removal
- **WHEN** code calls a legacy ownership alias
- **THEN** no deprecation shim remains that lets the call compile and behave like `own()`

### Requirement: Dynamic and When parent stack isolation during root element build
`Dynamic` and `When` SHALL isolate the `AttachmentStack` during the evaluation of captured root component elements (`root.element()`). Any component instantiated and built inside a void factory passed to `Dynamic` or `When` SHALL NOT attach its element to an ambient parent container on the stack, and SHALL attach exclusively to the `Dynamic` or `When` container.

#### Scenario: Child component inside Dynamic enclosed by Column attaches to Dynamic container
- **WHEN** a component with children (such as `Card` or `Row`) is created within `dynamic(...)` enclosed inside an outer `column(...)`
- **THEN** the child component's element parent is the `Dynamic` container table, NOT the outer column table

#### Scenario: Child component inside When enclosed by Column attaches to When container
- **WHEN** a component with children (such as `Card` or `Row`) is created within `when(...).thenDo(...)` or `elseDo(...)` enclosed inside an outer `column(...)`
- **THEN** the child component's element parent is the `When` container table, NOT the outer column table

### Requirement: BaseComponent is an ownership node
`BaseComponent` SHALL participate in the ownership tree: it SHALL record the node that adopted it, expose its child nodes, and register children adopted during its own build against itself rather than against whichever component happened to construct it.

#### Scenario: A component's children are its own
- **WHEN** a component constructs and builds children inside its own `build()`
- **THEN** those children are recorded as its children, not as children of the enclosing component

#### Scenario: Owner is queryable
- **WHEN** a component or resource is asked for its owning node
- **THEN** the owning node is returned, and a component can report whether it is an ancestor of another component

### Requirement: Single lazy realization upon parent attachment
Component `element()` SHALL be invoked lazily when the component is attached to its parent container by `AttachmentStack.pop()`. Factory methods in `solim.UI` SHALL NOT eagerly realize `element()`; `AttachmentStack.attachPendingComponents()` SHALL be the sole mechanism that resolves a pending component's element.

#### Scenario: Factory chaining before realization
- **WHEN** `button("Click").minWidth(100f).size(40f).padding(unit(2))` is called inside a `children()` block
- **THEN** `element()` is not invoked by the factory while the parent `children()` scope is still open; it is resolved when that scope closes and attaches its pending children

#### Scenario: Element realized exactly once
- **WHEN** a component is realized during attachment and `element()` is requested again later
- **THEN** the cached element is returned and `build()` is not invoked a second time

### Requirement: Immutable component ownership and no reparenting
`Component` SHALL NOT provide a `reparentTo` method. A component's lifecycle and ownership SHALL remain immutable once instantiated. When a new parent requires a component structure, a new component instance SHALL be instantiated.

#### Scenario: Components do not mutate parent ownership
- **WHEN** a component is created in an ownership scope
- **THEN** it cannot be reassigned or adopted into another owner via `reparentTo`.

#### Scenario: Element resolution does not reparent
- **WHEN** `ElementResolver` resolves a `Component` child to an `Element`
- **THEN** it resolves the element without moving the component to a different owner.

#### Scenario: Reconciled items do not reparent
- **WHEN** a reconciler mounts an item component
- **THEN** the item is attached through the attachment pipeline and is never moved to a different owner to make it attach.

### Requirement: The ambient ownership frame is unwound deterministically
A component SHALL install itself as the active ambient owner for exactly the duration of its `build()` and of each `children()` block, and SHALL unwind that frame deterministically when the block returns or throws, so the enclosing scope is restored and the `OwnershipContext` LIFO invariant holds.

#### Scenario: Scope teardown stays balanced
- **WHEN** a `build()` or `children()` block returns
- **THEN** the enclosing owner is the active ambient owner again and the stack is not left holding an orphan frame

#### Scenario: A throwing block still unwinds
- **WHEN** a `build()` or `children()` block throws
- **THEN** the ambient frame is cleared and the exception propagates to the caller

#### Scenario: The component is the owner only inside a window
- **WHEN** a resource is created before or after every window for that component
- **THEN** the component does not claim ownership of it, so the resource belongs to the scope that was actually ambient at creation time

