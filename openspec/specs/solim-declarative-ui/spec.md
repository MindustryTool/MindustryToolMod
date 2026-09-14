# solim-declarative-ui Specification

## Purpose
TBD - created by archiving change create-solim-core. Update Purpose after archive.
## Requirements
### Requirement: Implicit parent stack with lambda scopes
The framework SHALL provide solim.ui.ParentStack (and Ui facade) with static helpers column(), 
ow(), stack(), grid(int columns), wrap(), scroll(), container(), card(), and dialog(String title) returning fluent builder instances supporting .children(Runnable). Calling .children(Runnable) pushes the layout Element onto ParentStack, executes the lambda, pops with 	ry/finally, attaches the layout to the outer active parent container, and returns the container instance. Every child created inside the .children(Runnable) lambda SHALL auto-attach to current parent. This includes BaseComponent subclass instances — constructing a BaseComponent inside a children() block SHALL auto-attach it to the current parent without requiring an explicit component() call. The stack() helper SHALL create a solim.layout.SolimStack overlay container attached to ParentStack.

#### Scenario: Push/pop with try/finally and configuration before children
- **WHEN** column().grow().children(() -> { text("Settings"); row().growX().children(() -> { button("Cancel"); button("Save"); }); }) executes
- **THEN** internally: configuration .grow() is applied to column first, then column table is pushed to ParentStack, text is added to column, row table is pushed with .growX(), buttons are added to row, row is popped and attached to column, column is popped; if lambda throws, inally still pops

#### Scenario: Auto-attach children including BaseComponent subclasses
- **WHEN** inside column(() -> { text("Chat"); new ChatMessageListView(store, service); row().children(() -> { textField(input); button("Send", this::send); }); })
- **THEN** 	extField and utton are children of inner 
ow; 	ext and ChatMessageListView element are children of outer column — all without explicit dd or component() calls

#### Scenario: Stack helper creates SolimStack overlay
- **WHEN** stack().grow().layer(() -> icon(Icon.chat)).layer(() -> icon(Icon.warning)) is executed inside an active parent
- **THEN** a SolimStack is instantiated, its Stack element is attached to the parent, and both layers are overlaid on top of each other

#### Scenario: No start/end API
- **WHEN** solim.ui.Ui is inspected
- **THEN** it does NOT expose startColumn()/endColumn() or startComponent()/endComponent() — only configuration-before-children fluent methods exist

### Requirement: Icon button declarative facades
`Ui` SHALL provide static facades `iconButton(Drawable icon, Runnable onClick)` and `iconButton(Drawable icon, ImageButtonStyle style, Runnable onClick)` that construct an `IconButton`, automatically attach it to the active parent in `ParentStack`, and return the component for chained modifier calls.

#### Scenario: Attaching icon button via Ui facade
- **WHEN** `iconButton(Icon.infoCircle, onClick)` is called inside a `row(...)`
- **THEN** an `IconButton` is created, its element attached to the row table, and the `IconButton` instance returned

### Requirement: Stack cleanup guarantee
`ParentStack` SHALL guarantee cleanup even if child construction throws, using `try { push; runnable.run(); } finally { pop; }`.

#### Scenario: Exception cleanup
- **WHEN** `column(() -> { text("A"); throw new RuntimeException("fail"); })`
- **THEN** `ParentStack` size returns to previous value after catch, and subsequent `column(...)` works correctly

### Requirement: Current parent tracking and child add
`ParentStack` SHALL expose `current()` (or `peek()`) returning current parent `Element`/`Table` and `add(Element|Component)` that resolves and attaches to current parent. If no parent is active, `add` SHALL either throw `IllegalStateException` or attach to a supplied root.

#### Scenario: Add outside parent throws or roots
- **WHEN** `text("Hello")` is called with no active parent
- **THEN** framework either throws `IllegalStateException("No parent")` or requires caller to supply container; behavior is documented

### Requirement: Child resolution Element vs Component
`ElementResolver` SHALL resolve children: `Component → component.element()` , `Element → element` . Layout helpers SHALL accept both without forcing `.element()` at call sites.

#### Scenario: Resolve Component
- **WHEN** `ElementResolver.resolve(new Header())` where `Header implements Component`
- **THEN** returns `header.element()`

#### Scenario: Resolve Element passthrough
- **WHEN** `ElementResolver.resolve(new Label("hi"))`
- **THEN** returns same `Label` instance

#### Scenario: Column add overloads
- **WHEN** `column(() -> { add(new Header()); add(someElement); })`
- **THEN** both are added via resolver without `// require new Header().element()` comment

### Requirement: Layout facades return layout Element
Each declarative helper SHALL return the created layout `Element` so callers can chain modifiers or store references.

#### Scenario: Chained modifiers
- **WHEN** `Element e = column(() -> { text("Hi"); }).padding(24).gap(16)`
- **THEN** `e` is the column `Table` with padding/gap applied

### Requirement: UI arc escape-hatch for raw Arc elements
`UI` SHALL expose a static `arc(Element el)` method that attaches a raw Arc `Element` to the current `ParentStack` parent and returns it. This replaces the `component(() -> rawElement)` workaround for Arc elements that do not extend `BaseComponent`. The method is intentionally named `arc` — not `element` — to signal that it is an escape hatch for direct Arc layer access, making casual misuse for things like `arc(new Label("hi"))` visually incongruent with the available Solim equivalent `text("hi")`.

#### Scenario: Raw Arc element attaches via arc()
- **WHEN** `arc(new SchematicImage(schematic).setScaling(Scaling.fit))` is called inside a `children()` block
- **THEN** the `SchematicImage` is attached to the current parent table

#### Scenario: arc() returns the element for chaining
- **WHEN** `Element img = arc(new SchematicImage(s))` is called
- **THEN** the return value is the same `SchematicImage` instance passed in

#### Scenario: arc() outside a children scope is a no-op attachment
- **WHEN** `arc(someEl)` is called with no active `ParentStack` parent
- **THEN** no exception is thrown and the element is not attached to any table (consistent with `attachToParent` no-op behavior)

### Requirement: Single-threaded stack without ThreadLocal
`ParentStack` SHALL be implemented as a simple `Deque<Element>` / `ArrayDeque` static stack for single-threaded game thread; `ThreadLocal` SHALL NOT be used unless proven necessary.

#### Scenario: Stack is plain static
- **WHEN** `ParentStack.java` is inspected
- **THEN** it contains `private static final Deque<...> stack = new ArrayDeque<>()` and no `ThreadLocal` import

### Requirement: Select input facade
`UI` SHALL provide a static facade `select(Signal<T> signal, List<T> options)` that constructs a `SolimSelect`, automatically attaches its element to the active parent in `ParentStack`, and returns the component for chained modifier calls. No `switch()` facade SHALL exist (`switch` is a Java keyword); `switchToggle(Signal<Boolean>)` remains the switch facade.

#### Scenario: Attaching select via UI facade
- **WHEN** `select(signal, options)` is called inside a `children()` block
- **THEN** a `SolimSelect` is created showing the current signal value, its element attached to the parent table, and the `SolimSelect` instance returned

