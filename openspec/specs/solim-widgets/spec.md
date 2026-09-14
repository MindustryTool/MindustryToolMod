# solim-widgets Specification

## Purpose

Mechanical merge of 12 specs per change `spec-domain-merge` (stage 4 framework-ui, concat-then-dedupe). Sources: solim-widgets, animated-loader, solim-arc-interop-facade, solim-declarative-ui, solim-dialog, solim-hud, solim-input-extensions, solim-network-image, solim-popup-menu, solim-scroll-pagination, solim-tabs, solim-virtual-list. Each source below appears under a `**Source:` marker with its purpose body and requirement blocks verbatim; per-source `## Purpose` / `## Requirements` header lines are removed so all requirements parse inside the single `## Requirements` section. TBD purposes carried forward; requirement dedupe is follow-up work.

## Requirements

**Source: solim-widgets**

TBD - created by archiving change create-solim-core. Update Purpose after archive.
### Requirement: Text widget with static and reactive content
`Text` SHALL display string content via `text(String)` and `text(Readable<String>)` (reactive). It SHALL wrap `arc.scene.ui.Label` and provide fluent chained property modifiers including `.color(Color)`, `.color(Readable<Color>)`, `.style(LabelStyle)`, `.wrap(boolean)`, `.ellipsis(boolean)`, `.fontScale(float)`, text alignment (`.left()`, `.center()`, `.right()`), and padding/margin modifiers (`.padding(float)`, `.padding(float, float, float, float)`, `.paddingTop(float)`, `.paddingBottom(float)`, `.paddingLeft(float)`, `.paddingRight(float)`, `.margin(float)`, `.margin(float, float, float, float)`, `.marginTop(float)`, `.marginBottom(float)`, `.marginLeft(float)`, `.marginRight(float)`). Reactive bindings SHALL be managed internally by the component lifecycle.

#### Scenario: Static Text
- **WHEN** `text("Settings")` is called
- **THEN** a `Label` with "Settings" is created and added to current parent

#### Scenario: Reactive Text
- **WHEN** `Computed<String> t = count.map(v -> "Count: " + v)` and `text(t)` then `count.set(5)`
- **THEN** label text updates to "Count: 5" via binding without recreation

#### Scenario: Chained text styling and layout modifiers
- **WHEN** `text("Description").wrap(true).ellipsis(true).color(Color.lightGray).fontScale(0.9f)` is declared
- **THEN** the underlying `Label` has word-wrapping, ellipsis truncation, light gray color, and 0.9 font scale configured directly

#### Scenario: Reactive text color binding
- **WHEN** `text("Status").color(statusColorReadable)` is declared and the status color changes
- **THEN** the label's color updates immediately via internal component binding without external `Binding` calls

#### Scenario: Text padding and margin modifiers
- **WHEN** `text("Title").padding(12f).margin(4f)` is declared inside a parent table
- **THEN** the parent cell padding around the label reflects the combined padding and margin

### Requirement: Image and Icon widgets
`Image` SHALL display `Drawable`/`TextureRegion` with `image(Drawable)` and `image(Signal<Drawable>)`. `Icon` SHALL display icon drawable with `icon(IconType)` and reactive overload. Both SHALL support size/style bindings, as well as fluent padding and margin modifiers on `SolimImage` and `SolimImage.SizedImage` (`.padding(float)`, `.padding(float, float, float, float)`, `.paddingTop(float)`, `.paddingBottom(float)`, `.paddingLeft(float)`, `.paddingRight(float)`, `.margin(float)`, `.margin(float, float, float, float)`, `.marginTop(float)`, `.marginBottom(float)`, `.marginLeft(float)`, `.marginRight(float)`).

#### Scenario: Image display
- **WHEN** `image(backgroundDrawable)` is called
- **THEN** an `Image` with that drawable is added

#### Scenario: Icon reactive
- **WHEN** `icon(darkMode.map(v -> v ? Icon.moon : Icon.sun))` and `darkMode` toggles
- **THEN** icon drawable updates via binding

#### Scenario: Image padding and margin modifiers
- **WHEN** `image(icon).padding(8f).margin(2f)` is declared inside a parent table
- **THEN** the parent cell padding around the image reflects the combined padding and margin

### Requirement: Badge and Avatar lightweight components
`Badge` SHALL be a lightweight label/container for counts/status, `Avatar` SHALL display user image with fallback. Both may be convenience composites over `Text`/`Image` + `Container`.

#### Scenario: Badge count
- **WHEN** `badge(count.map(v -> v > 99 ? "99+" : String.valueOf(v)))` is used
- **THEN** badge text updates reactively

### Requirement: Button and IconButton with click handler and reactive props
`Button` and `IconButton` SHALL support `button(String|Signal|Computed|Readable, Runnable onClick)` and `iconButton(Drawable, Runnable onClick)` / `iconButton(Drawable, ImageButtonStyle, Runnable onClick)` plus chained modifiers `.enabled(Readable<Boolean>)`, `.visible(Readable<Boolean>)`, `.size(float)`, `.tooltip(String)`, `.tooltip(Readable<String>)`, `.onLongClick(Runnable)`, `.onLongClick(long, Runnable)`, `.style(...)`, and `.stopClickPropagation()`. When a long click triggers, regular `onClick` SHALL be suppressed. Reactive bindings SHALL be managed internally by the component.

#### Scenario: Button click handler
- **WHEN** `button("Save", () -> save())` is clicked
- **THEN** `save()` is invoked

#### Scenario: Button reactive text and style
- **WHEN** `Computed<String> saveText = dirty.map(v -> v ? "● Save" : "Save")` and `button(saveText, onSave).enabled(dirty).style(Styles.PRIMARY)` then `dirty.set(true)`
- **THEN** button text, enabled, and style update via bindings; click only enabled when dirty true

#### Scenario: Button long click handler
- **WHEN** a user holds a button configured with `.onLongClick(onLongPressAction)` for >= 300ms
- **THEN** `onLongPressAction` is executed and regular `onClick` is suppressed upon release

#### Scenario: Button short click does not trigger long press
- **WHEN** a user taps and releases a button configured with both `.onClick(clickAction)` and `.onLongClick(longAction)` in < 300ms
- **THEN** `clickAction` is executed and `longAction` is not triggered

#### Scenario: Reactive tooltip on Button
- **WHEN** `button("Action", () -> {}).tooltip(tooltipSignal)` is declared and `tooltipSignal` emits a new string
- **THEN** the button's tooltip text updates to reflect the new string value

### Requirement: TextField and TextArea with Signal binding
`TextField` SHALL bind to `Signal<String>` via `textField(signal)` with two-way sync: typing updates signal, signal changes update field text (without cursor jump when possible). `TextArea` SHALL be multiline variant.

#### Scenario: Two-way TextField
- **WHEN** `Signal<String> input = Signal.of("")` and `textField(input)` is displayed and user types "hi"
- **THEN** `input.get()` becomes "hi"; when `input.set("hello")` programmatically, field text updates to "hello"

#### Scenario: TextArea multiline
- **WHEN** `textArea(description)` is used
- **THEN** a multiline `TextArea` is created bound to `description` signal

### Requirement: Checkbox, Switch, Slider, Select input widgets
`Checkbox` (`checkbox(String, Signal<Boolean>)`, `checkbox(String, boolean, Consumer<Boolean>)`), `Switch` (`switch(Signal<Boolean>)`), `Slider` (`slider(Signal<Float>, min, max, step)`, `slider(Signal<Integer>, min, max, step)`), `Select<T>` (`select(Signal<T>, List<T>)` or `select(Signal<T>, T... options)`) SHALL all support reactive value binding and change callbacks, delegating to Arc widgets.

#### Scenario: Checkbox binding
- **WHEN** `checkbox("Enable", enabled)` where `enabled = Signal.of(false)` and user toggles checkbox
- **THEN** `enabled.get()` toggles to true; when `enabled.set(false)` programmatically, checkbox shows unchecked

#### Scenario: Checkbox callback
- **WHEN** `checkbox("Enable", true, val -> callback.accept(val))` is rendered and user unchecks the box
- **THEN** the callback is invoked with `false`

#### Scenario: Slider binding
- **WHEN** `slider(volume, 0f, 1f, 0.1f)` where `volume = Signal.of(0.5f)` and slider drags to 0.8
- **THEN** `volume.get()` updates to 0.8; programmatic `volume.set(0.2f)` moves slider thumb

#### Scenario: Integer Slider binding
- **WHEN** `slider(cols, 1, 9, 1)` where `cols = Signal.of(3)` and slider drags to 6
- **THEN** `cols.get()` updates to 6; programmatic `cols.set(4)` moves slider thumb to 4

#### Scenario: Select binding
- **WHEN** `select(selected, List.of("A","B","C"))` and user picks "B"
- **THEN** `selected.get()` becomes "B"; programmatic `selected.set("C")` updates displayed selection

### Requirement: Dialog and Popup overlay widgets
`Dialog` SHALL wrap Arc `BaseDialog`/`Dialog` with declarative content via `dialog(title, Runnable content)` and `show()`/`hide()` methods, using Arc native overlay. `Popup` SHALL be lightweight tooltip/context menu.

#### Scenario: Dialog show/hide
- **WHEN** `Dialog d = dialog("Confirm", () -> { text("Are you sure?"); row(() -> { button("Yes", d::hide); button("No", d::hide); }); }); d.show();`
- **THEN** dialog appears via Arc `show()` and content is built via parent stack inside dialog container

### Requirement: Spinner, ProgressBar, Alert feedback widgets
`Spinner` SHALL show loading indicator, `ProgressBar` SHALL bind to `Signal<Float>` progress (0..1), `Alert` SHALL show dismissible message with type (info/warning/error).

#### Scenario: ProgressBar reactive progress
- **WHEN** `progressBar(progress)` where `progress = Signal.of(0.3f)` then `progress.set(0.7f)`
- **THEN** progress bar visual updates to 70%

#### Scenario: Alert dismiss
- **WHEN** `alert("Saved!", AlertType.SUCCESS).show()` then close button clicked
- **THEN** alert is removed from parent

### Requirement: Widgets built on binding, layout, style foundations
All widgets SHALL reuse `Binding`/`Effect` for reactivity, `ParentStack` for declarative construction, and `Style` for styling; SHALL NOT reimplement reactive or layout logic per-widget.

#### Scenario: Widget thin wrapper
- **WHEN** `Button.java`/`Text.java` are inspected
- **THEN** they delegate to Arc `TextButton`/`Label` creation and use `Binding.of(...)` or `Effect.of(...)` for reactive props, not custom state loops

### Requirement: Prioritized implementation order
Widgets SHALL be implemented in order: `Text`, `Button`, then `TextField`, `Checkbox`, `Switch`, `Slider`, `Select` before `Image`/`Icon`, then Badge/Avatar, then Dialog/Popup, then Spinner/ProgressBar/Alert. Tests SHALL cover first batch before advanced controls.

#### Scenario: Text and Button tests first
- **WHEN** `gradle :solim:test` runs after initial implementation
- **THEN** `Text`/`Button` reactive binding tests pass before `Dialog` tests are required

### Requirement: Card is a single-Table layout element
The `Card` component SHALL be a single-Table layout element consistent with `Column` and `Row`, where child composition, sizing, and background all target the same Arc `Table` instead of a Button-wrapping-Table pair.

#### Scenario: Card children attach to the element table
- **WHEN** `card(() -> { text("Hi"); })` is rendered
- **THEN** the text label is a direct child cell of the table returned by both `element()` and `table()`.

#### Scenario: Card click isolation from inner buttons
- **WHEN** an inner button stops event propagation inside a clickable card
- **THEN** the inner button handler runs and the card `onClick` handler does not run.

**Source: animated-loader**

Reusable animated circular loader component for mod dialogs.

### Requirement: Animated Circle Loader Component
The system SHALL provide a reusable `Loader` Solim component that renders `loader-circle.png` and continuously animates its rotation around its center point.

#### Scenario: Continuous center-origin rotation
- **WHEN** the `Loader` component is mounted and rendered on screen
- **THEN** it SHALL update its rotation each frame using `Time.delta` and keep its origin centered to prevent visual wobble

#### Scenario: Screen and dialog centering
- **WHEN** `Loader.centered()` is invoked in a container
- **THEN** it SHALL return a layout container that expands (`grow()`) and centers the spinner vertically and horizontally

### Requirement: Dialog Loading State Integration
The system SHALL display the animated circular loader instead of static loading text across all primary mod dialogs.

#### Scenario: Schematic browser loading
- **WHEN** the schematic browser is fetching data from the API
- **THEN** the dialog SHALL display `Loader.centered()` in place of static loading text

#### Scenario: Map browser loading
- **WHEN** the map browser is fetching data from the API
- **THEN** the dialog SHALL display `Loader.centered()` in place of static loading text

#### Scenario: Auth login loading
- **WHEN** the login dialog is generating the authentication URL
- **THEN** the dialog SHALL display the animated circular loader centered in the dialog content area

**Source: solim-arc-interop-facade**

`UI.arc(Element el)` provides a named escape hatch for attaching raw Arc `Element` instances with no Solim equivalent, replacing the `component(() -> someElement)` workaround. The deliberate name discourages casual misuse.


### Requirement: UI.arc escape-hatch for raw Arc elements
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

**Source: solim-declarative-ui**

TBD - created by archiving change create-solim-core. Update Purpose after archive.
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

**Source: solim-dialog**

Declarative dialog component providing reactive signals, layout helpers, and automatic lifecycle management and disposal of attached content components and resources.
### Requirement: Automatic Content Component Lifecycle Management
The `SolimDialog` component SHALL manage content attachment declaratively via `children(Runnable)` using `ParentStack` and SHALL defer executing the content builder until the dialog is shown or unwrapped via `dialog()`. It SHALL automatically register any resources and components created within the content builder with its internal disposable registry. The legacy `content(Component)` method SHALL NOT be supported.

#### Scenario: Content builder is not executed on instantiation
- **WHEN** a `SolimDialog` is instantiated and configured with `children(Runnable contentBuilder)`
- **THEN** `contentBuilder` is not executed during instantiation
- **AND** no child elements or reactive bindings are created before the dialog is shown

#### Scenario: Content builder executed lazily when shown
- **WHEN** `show()` is invoked on a `SolimDialog` configured with `children(Runnable contentBuilder)`
- **THEN** `contentBuilder` is executed
- **AND** its child elements are attached to the dialog's content container via `ParentStack`

#### Scenario: Content builder executed when unwrapped
- **WHEN** `dialog()` is invoked on a `SolimDialog` configured with `children(Runnable contentBuilder)`
- **THEN** `contentBuilder` is executed before the wrapped dialog is returned
- **AND** showing the returned dialog displays the built content

#### Scenario: Content only built once across repeated shows
- **WHEN** `show()` is invoked multiple times on the same `SolimDialog`
- **THEN** `contentBuilder` is only executed once and existing elements are preserved

#### Scenario: Dialog disposal cleans up content components
- **WHEN** `dispose()` is invoked on a `SolimDialog` whose content was built
- **THEN** all attached components and resources are automatically disposed

### Requirement: Full Screen by Default
The `SolimDialog` component SHALL enable `fillParent(true)` by default to fill the entire viewport width and height, matching standard Mindustry dialog behavior, while allowing callers to override it via `fillParent(boolean)`.

#### Scenario: Default full screen sizing
- **WHEN** a `SolimDialog` is instantiated
- **THEN** `isFillParent()` returns `true` and the dialog expands to fill the entire scene

#### Scenario: Explicit non-fullscreen sizing
- **WHEN** a `SolimDialog` is configured with `fillParent(false)`
- **THEN** `isFillParent()` returns `false` and the dialog sizes according to its packed bounds

**Source: solim-hud**

TBD - created by archiving change solim-hud-components. Update Purpose after archive.
### Requirement: Floating Hud container
The Solim framework SHALL provide a `Hud` container (`hud()`, `hud(Runnable children)`) in `solim.overlay` representing a floating, non-modal screen overlay. The `Hud` root element SHALL default to `touchable = childrenOnly` so unconsumed touches pass through to underlying game elements.

#### Scenario: Creating a basic Hud container
- **WHEN** `hud(() -> { button("Click", () -> {}); })` is constructed
- **THEN** a `Hud` component is created with its content wrapped and added to the overlay root

#### Scenario: Touch pass-through on Hud container
- **WHEN** touches occur outside the HUD children but within the HUD container bounds
- **THEN** touches are not consumed by the HUD container and pass through to the scene below

### Requirement: Hud viewport boundary clamping and resize adaptation
The `Hud` container SHALL support a `keepInScreen()` method and automatically register an Arc `ResizeEvent` listener on construction to clamp its $(x, y)$ coordinates within $[0, \text{screenWidth} - \text{hudWidth}]$ and $[0, \text{screenHeight} - \text{hudHeight}]$. When scale is applied to the Hud container, `root.pack()` SHALL be called after the scale change so the root cell tracks the container's scaled visual bounds and hit-testing remains accurate.

#### Scenario: Clamping Hud coordinates when positioned outside screen
- **WHEN** a `Hud` has width 200, height 100, and $(x, y)$ is set to $(1200, 900)$ on a $1024 \times 768$ screen
- **THEN** `keepInScreen()` clamps $(x, y)$ to $(824, 668)$

#### Scenario: Screen resize event keeps Hud within viewport
- **WHEN** a `ResizeEvent` occurs in the game runtime
- **THEN** `Hud` updates its layout and clamps coordinates within the new viewport dimensions

#### Scenario: Hit region matches visual bounds after scale change
- **WHEN** `hud.scale(0.8f)` is applied
- **THEN** hovering and clicking within the visually rendered area of the Hud triggers correct element hover and click states, with no offset between visual position and hit region

### Requirement: Draggable modifier for Hud repositioning
The Solim framework SHALL provide a `.draggable()` modifier that attaches touch drag handling to an element. The handle element SHALL have `touchable = enabled` so its entire surface area can capture drag gestures. When a touch occurs on an interactive descendant button or clickable control within the handle, the drag listener SHALL NOT consume the event and SHALL return `false` on `touchDown` so the child control receives the click. When a touch occurs on the container background or non-button elements (such as text, images, or spacers), the drag listener SHALL return `true`, translate the parent `Hud` container by the drag delta, clamp coordinates to the screen, and optionally update reactive `Signal<Float>` x and y positions.

#### Scenario: Dragging a handle moves the Hud
- **WHEN** a user touches down on a non-button area of a handle configured with `.draggable()` and drags
- **THEN** the parent `Hud` element moves by the touch displacement vector and updates the associated coordinate signals

#### Scenario: Clamping during drag movement
- **WHEN** a drag movement attempts to push the `Hud` outside screen bounds
- **THEN** the movement is clamped at the screen border

#### Scenario: Interactive descendant button receives click without drag
- **WHEN** a user touches down and clicks a `Button` inside a handle container configured with `.draggable()`
- **THEN** the button action fires and no `Hud` dragging is initiated

#### Scenario: Dragging from label or spacer in handle moves Hud
- **WHEN** a user touches down and drags on a `Label`, `Image`, or empty background area within a handle container
- **THEN** the parent `Hud` moves with the drag gesture

**Source: solim-input-extensions**

Provides enter-key submission and validation hooks on SolimTextField.

### Requirement: Enter Key Submission
The system SHALL provide onEnter(Consumer<String> onSubmit) and onEnter(Runnable onSubmit) on solim.input.SolimTextField.

#### Scenario: Pressing enter key with text
- **WHEN** user types text into the field and presses the Enter key
- **THEN** the onEnter callback is invoked with the current text content of the field.

#### Scenario: Enter key on disabled field
- **WHEN** the text field is disabled or disabled signal evaluates to true
- **THEN** pressing Enter does not trigger the onEnter callback.

### Requirement: Custom Input Validation Feedback
The system SHALL allow attaching custom input validator predicates to SolimTextField with reactive validity signals.

#### Scenario: Content validation check
- **WHEN** a validator predicate is registered on the text field
- **THEN** validity updates reactively based on the current text length and format.

### Requirement: InputStyle Value Object
The system SHALL provide an immutable `InputStyle` value in `solim-core` describing textfield chrome. Any background slot left unset SHALL fall back to the field's base style at application time; cursor, selection, fonts, and colors behave the same way.

#### Scenario: Partial preset inherits the rest
- **WHEN** an `InputStyle` declares only background drawables and is applied to a field
- **THEN** the field keeps its base font, font colors, cursor, and selection while using the preset backgrounds

### Requirement: Per-Instance Input Styling Without Leaks
`SolimTextField` SHALL accept an `InputStyle` and install it by copying the field's current `TextFieldStyle` and overriding only the declared slots on the copy. The shared base style object SHALL remain unmodified, and sibling fields SHALL be visually unaffected.

#### Scenario: Styled field does not leak to siblings
- **WHEN** an `InputStyle` with blank backgrounds is applied to one field while another field shares the same base style
- **THEN** the styled field renders without chrome and the sibling field still renders the base chrome, and the base style object is reference-equal to its pre-application state

#### Scenario: Repeated application is allocation-free on the preset side
- **WHEN** the same `InputStyle` instance is applied to multiple fields
- **THEN** the preset itself is shared (one copy per field instance only, as Arc requires per-widget style objects)

**Source: solim-network-image**

Provides asynchronous texture fetching and rendering with in-memory caching and reactive URL binding.
### Requirement: Declarative Network Image Component
The system SHALL provide a NetworkImage component (solim.display.NetworkImage, Ui.networkImage) that asynchronously fetches an image from an HTTP/HTTPS URL and displays it in the Solim UI tree.

#### Scenario: Async image loaded successfully
- **WHEN** a valid image URL is supplied to 
etworkImage(url)
- **THEN** the component initially renders the placeholder drawable and transitions to the downloaded texture once the network response is received.

#### Scenario: Image download failure fallback
- **WHEN** an image URL fails to load or returns an HTTP error
- **THEN** the component falls back to the configured fallback drawable without throwing unhandled exceptions.

### Requirement: In-Memory Texture Caching
The system SHALL maintain an in-memory texture cache for downloaded images to avoid redundant network requests and visual flickering.

#### Scenario: Cached image display
- **WHEN** multiple components or repeat requests load an image URL that is already cached
- **THEN** the component retrieves the texture from cache immediately on the main thread without re-triggering an HTTP fetch.

### Requirement: Reactive URL Binding
The system SHALL support dynamic image updates when provided a Readable<String> URL source.

#### Scenario: URL signal change
- **WHEN** the underlying URL signal changes to a new image address
- **THEN** the component updates its display to fetch and render the new image.

### Requirement: Enforced Size Constraints Overriding Intrinsic Texture Dimensions
The `NetworkImage` component and underlying `SizedImage` SHALL respect explicit size constraints and preferred dimensions, preventing downloaded image dimensions from overriding configured sizes.

#### Scenario: Network image loaded with explicit size
- **WHEN** `networkImage(...).size(w, h)` is specified and a network image texture is loaded
- **THEN** the component's preferred width and height remain fixed at `w` and `h` rather than expanding to the texture's native dimensions.

### Requirement: Continuous-Curvature Corner Rounding on Network Image
The `NetworkImage` component SHALL support configuring continuous-curvature (L4 superellipse) rounded corners via `.rounded(int radius)`. When a positive corner radius is specified, downloaded and cached textures SHALL have anti-aliased transparent corners corresponding to the specified radius.

#### Scenario: Network image configured with rounded radius
- **WHEN** `networkImage(url).rounded(radius)` is loaded with `radius > 0`
- **THEN** the rendered texture displays with anti-aliased transparent corners according to the L4 continuous curvature superellipse formula.

#### Scenario: Memory cache separation by radius
- **WHEN** the same image URL is loaded with different corner radii (or one unrounded and one rounded)
- **THEN** each radius produces and retrieves its own distinct cached `TextureRegionDrawable` without colliding.

### Requirement: Network Request Timeout and Request Headers
The `NetworkImage` component SHALL configure HTTP image requests with an extended timeout of at least 30,000ms (30 seconds) and a standard User-Agent header (`MindustryTool/<version>` or browser-compatible identifier) to prevent CDN throttling and premature download aborts on large images or slower connections.

#### Scenario: Image fetch with extended timeout and user agent
- **WHEN** an image URL is requested from an external CDN host
- **THEN** the HTTP request is executed with a timeout of at least 30,000ms and sends a valid User-Agent request header

**Source: solim-popup-menu**

Native Solim floating context menu providing reactive provider content, explicit show/hide with stage-coordinate placement, prefer-above/flip/clamp positioning, tap-outside, Back/Escape, and resize dismissal, with headless-safe no-ops.
### Requirement: Reactive provider content
The `Popup` component SHALL render its menu content from a caller-supplied provider function applied to the data passed at show time, rebuilding content on every `show()` call.

#### Scenario: Menu content reflects shown data
- **WHEN** `show(data, x, y)` is called with a data value
- **THEN** the visible menu content is the provider function applied to that data value

#### Scenario: Re-showing hot-swaps content and anchor
- **WHEN** `show()` is called again while the menu is already visible with different data or coordinates
- **THEN** the menu content is rebuilt from the new data and the menu is repositioned at the new anchor without requiring hide first

### Requirement: Explicit show and hide with stage placement
The `Popup` component SHALL expose explicit `show(data, x, y)` and `hide()` controls placing the menu at stage coordinates, independent of the data flow. Setting data never shows the menu by itself.

#### Scenario: Show places menu at anchor
- **WHEN** `show(data, x, y)` is called with a non-null scene
- **THEN** the menu element is added to the scene root positioned at the clamped anchor coordinates

#### Scenario: Hide removes menu
- **WHEN** `hide()` is called while the menu is visible
- **THEN** the menu element is removed from the scene and all menu listeners are detached

### Requirement: Prefer-above anchor positioning with clamping
The `Popup` component SHALL position the menu with its bottom edge at the anchor vertical coordinate (floating above the anchor), flipping below the anchor when there is insufficient space above, and clamping both axes so the menu stays fully within the stage bounds.

#### Scenario: Menu opens above anchor with room
- **WHEN** `show(data, x, y)` is called and the menu fits between the anchor and the top stage edge
- **THEN** the menu bottom edge aligns with the anchor coordinate

#### Scenario: Menu flips below anchor without room
- **WHEN** `show(data, x, y)` is called and the menu does not fit above the anchor
- **THEN** the menu top edge aligns with the anchor coordinate, clamped inside the stage

#### Scenario: Menu stays within stage bounds
- **WHEN** `show(data, x, y)` is called with an anchor near any stage edge
- **THEN** the menu is fully visible inside the stage on both axes

### Requirement: Tap-outside dismissal
The `Popup` component SHALL dismiss the menu when the user touches anywhere outside the menu bounds, swallowing that touch so no underlying element receives it. Touches inside the menu SHALL reach menu children normally.

#### Scenario: Outside tap dismisses and swallows
- **WHEN** the user touches down outside the visible menu bounds
- **THEN** the menu is dismissed and the touch is consumed (underlying elements do not activate)

#### Scenario: Inside tap reaches menu
- **WHEN** the user touches down inside the visible menu bounds
- **THEN** the menu stays open and the touch is delivered to the touched menu child

### Requirement: Back and Escape dismissal
The `Popup` component SHALL dismiss the menu when the Back key (mobile) or Escape key (desktop) is pressed while the menu is visible, consuming the key event so underlying handlers do not also react.

#### Scenario: Escape closes open menu
- **WHEN** Escape is pressed while the menu is visible
- **THEN** the menu is dismissed and the key event is consumed

#### Scenario: Back closes open menu
- **WHEN** the Back key is pressed while the menu is visible
- **THEN** the menu is dismissed and the key event is consumed

### Requirement: Resize dismissal
The `Popup` component SHALL dismiss the menu when a stage resize event fires while the menu is visible, since absolute anchor coordinates do not survive re-layout.

#### Scenario: Resize hides menu
- **WHEN** the stage resizes while the menu is visible
- **THEN** the menu is dismissed

### Requirement: Headless-safe no-ops
The `Popup` component SHALL perform no scene operations when no scene exists, making `show()` and `hide()` safe to call in headless environments.

#### Scenario: Show without scene does nothing
- **WHEN** `show(data, x, y)` is called with no scene available
- **THEN** no exception is thrown and no listeners are attached

#### Scenario: Hide without scene does nothing
- **WHEN** `hide()` is called with no scene available and nothing shown
- **THEN** no exception is thrown

### Requirement: Fluent chainable configuration
The `Popup` component SHALL return its own instance from every configuration and control method (`children`, `rounded`, `border`, `show`, `hide`) so a menu is declared as a single chain.

#### Scenario: Chained configuration yields one instance
- **WHEN** a menu is configured through chained calls (`children(provider)`, `rounded()`, `border()`, `show()`, `hide()`) on a typed instance
- **THEN** each call returns the same single menu instance in order (configuration starts from direct assignment, since generic inference does not propagate through chains rooted at the bare facade call)

### Requirement: Default visible menu chrome
The `Popup` component SHALL render with a default dark rounded menu background so an unstyled menu is visible, while existing `rounded()` and `border()` calls override the chrome.

#### Scenario: Unstyled menu is visible
- **WHEN** a menu is shown without explicit chrome configuration
- **THEN** the menu renders with the default dark rounded background

#### Scenario: Explicit chrome overrides default
- **WHEN** `rounded()` or `border()` is configured before showing
- **THEN** the menu renders with the caller-specified chrome instead of the default

**Source: solim-scroll-pagination**

Provides reach-top and reach-bottom boundary triggers with debouncing on Solim Scroll containers for infinite pagination.

### Requirement: Scroll Boundary Triggers
The system SHALL provide onReachTop(float thresholdPx, Runnable callback) and onReachBottom(float thresholdPx, Runnable callback) methods on solim.layout.Scroll.

#### Scenario: Reaching top threshold triggers pagination
- **WHEN** user scrolls up within 	hresholdPx of the top of the scroll container
- **THEN** the registered onReachTop callback is executed to trigger history fetching.

#### Scenario: Edge crossing debouncing
- **WHEN** user remains within the top threshold area during continuous scrolling
- **THEN** the callback is not invoked repeatedly until the user scrolls away and crosses the threshold again.

### Requirement: Scroll Position Binding
The system SHALL support programmatically setting and reading scroll positions on solim.layout.Scroll.

#### Scenario: Programmatic scroll to bottom
- **WHEN** scrollToBottom() is called on a Scroll instance
- **THEN** the underlying scroll pane adjusts its scroll position to the bottom of the content.

**Source: solim-tabs**

Provides a declarative tabs component for organizing Solim views into selectable tab panels with reactive active tab binding.
### Requirement: Declarative Tabs Component
The system SHALL provide a Tabs component (solim.layout.Tabs, Ui.tabs) for organizing views into selectable tab panels.

#### Scenario: Switching active tab
- **WHEN** user clicks on a tab header or the active tab signal changes value
- **THEN** the active tab button updates its visual state and the corresponding tab content is displayed.

#### Scenario: Reactive active tab binding
- **WHEN** 	abs is constructed with a Signal<Integer> activeTab index
- **THEN** mutations to the signal change the displayed tab, and selecting a tab button writes the new index into the signal.

### Requirement: Lazy Tab Mounting and Inactive Tab Layout Isolation
The Tabs component SHALL support lazy tab mounting where tab content is instantiated only when first selected, and inactive tab containers SHALL have their layout updates disabled (`setLayoutEnabled(false)`) so inactive subtrees do not participate in scene layout validation or consume CPU cycles.

#### Scenario: Inactive tabs do not build until selected
- **WHEN** a tab container is constructed with multiple tabs
- **THEN** only the currently active tab's content builder is initially executed and mounted.

#### Scenario: Inactive tab layout disabled
- **WHEN** a tab is not active
- **THEN** its container element has `setLayoutEnabled(false)` and `setVisible(false)` applied, preventing layout propagation from affecting the parent scene.

**Source: solim-virtual-list**

Reusable Solim virtual list layout primitive that measures item heights and mounts only elements intersecting the visible viewport plus an overscan buffer inside a scroll container.


### Requirement: Reusable virtual list layout component
The Solim layout system SHALL provide a declarative `VirtualList` component (accessible via `solim.UI.virtualList(...)`) that accepts an item collection, key selector, item height provider, and item component builder, and mounts only elements that intersect the visible viewport plus an overscan buffer.

#### Scenario: Mounting only visible items in viewport
- **WHEN** a list of 100 items is supplied to `VirtualList` inside a scroll viewport that can physically fit 10 items
- **THEN** only the visible items plus configured overscan buffer items are instantiated and added to the Scene2D hierarchy, while off-screen items are not mounted

#### Scenario: Overscan buffer preservation
- **WHEN** items are rendered in `VirtualList`
- **THEN** an overscan buffer (default 3 items above and below the visible viewport) is mounted to prevent visual blanking during scrolling

### Requirement: Prefix-sum height indexing and scroll bounds
The `VirtualList` SHALL maintain a prefix-sum array of cumulative item heights, report the total height as its preferred height to the parent `ScrollPane`, and locate visible items in $O(\log N)$ time using binary search.

#### Scenario: Total scrollable height reflects all items
- **WHEN** items with variable or fixed heights are loaded into `VirtualList`
- **THEN** `getPrefHeight()` returns the exact sum of all item heights plus spacing, allowing the `ScrollPane` scrollbar to reflect the full content extent

#### Scenario: Binary search visible window lookup
- **WHEN** the scroll position changes to an arbitrary offset $Y$
- **THEN** the component computes the first and last visible item indices in $O(\log N)$ time using binary search on the cumulative height array

### Requirement: Viewport scroll synchronization
The `VirtualList` SHALL monitor `ScrollPane` visual scroll updates and reconcile active mounted child components without rebuilding or recreating unchanged items.

#### Scenario: Scrolling updates mounted elements smoothly
- **WHEN** the user scrolls the viewport down by several item heights
- **THEN** items scrolling out of view past the overscan buffer are unmounted, newly visible items are mounted, and previously mounted items that remain in the visible window are retained

### Requirement: Container width invalidation
The `VirtualList` SHALL detect changes to container width and trigger re-measurement of variable-height items and rebuild of the prefix-sum array.

#### Scenario: Width resize recalculates layout
- **WHEN** the container width changes due to window or panel resizing
- **THEN** variable-height items are re-measured against the new width, the prefix-sum array is updated, and the visible window is re-computed

