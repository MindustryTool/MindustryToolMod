# AGENTS.md

## Project Rules

This project is a **Mindustry game mod**, not a backend application.

Prioritize:

1. Correctness
2. Simple game-mod architecture
3. Clear ownership and lifecycle
4. Declarative Solim UI
5. Automatic reactive bindings
6. Localized structural updates
7. Performance optimization

Do not introduce complexity unless it provides clear value.

---

# Internationalization (i18n) — Mandatory

## Core Rule

**All user-visible text must be translatable.**

Never hardcode display text in Java code unless it is explicitly non-user-facing.

Primary translation bundle:

```text
assets/bundles/bundle.properties
```

This applies to all user-visible text, including:

* Buttons
* Labels
* Menus
* Dialogs
* Tooltips
* Notifications
* Chat and player messages
* Errors and warnings
* Status messages
* Command responses
* Validation messages
* Empty and loading states
* Help text
* Settings descriptions

### Static text

```java
Core.bundle.get("translation.key");
```

### Dynamic text

Use bundle formatting instead of string concatenation:

```java
Core.bundle.format("translation.key", value1, value2);
```

❌ Bad:

```java
player.sendMessage("Welcome, " + player.name + "!");
```

✅ Good:

```properties
# Welcome message displayed to a player.
# {0} is the player's display name.
message.welcome=Welcome, {0}!
```

```java
player.sendMessage(Core.bundle.format("message.welcome", player.name));
```

## Adding Translation Keys

When introducing new user-visible text:

1. Check whether an existing key already represents the same concept.
2. Reuse it when appropriate.
3. Otherwise create a meaningful new key.
4. Add it to `assets/bundles/bundle.properties`.
5. Add a descriptive comment directly above the key.
6. Use the key in code instead of hardcoding text.

### Translation Comments

**Every translation key must have a descriptive comment directly above it.**

Comments should explain:

* What the text means
* Where it is displayed
* When it should be used
* Important context for translators
* Placeholder meanings such as `{0}` and `{1}`

❌ Bad:

```properties
# Save
button.save=Save
```

✅ Good:

```properties
# Displayed on a button that saves the current configuration or changes.
button.save=Save
```

Group comments do **not** replace comments for individual keys.

### Key Naming

Use lowercase, dot-separated keys.

Preferred categories:

```text
ui.*
button.*
menu.*
dialog.*
message.*
error.*
warning.*
status.*
command.*
setting.*
```

Rules:

* Use meaningful names.
* Group keys by feature or purpose.
* Follow existing project conventions.
* Do not create duplicate keys for the same concept.
* Avoid vague names such as `text1`, `label`, or `message2`.

## Exceptions

These generally do not require translation:

* Internal logs
* Debug messages
* Developer-only errors
* Class, method, and variable names
* Configuration keys
* API fields
* Database values
* Protocol and machine-readable strings

If text can be shown to a player or end user, it must be translated.

---

# HTTP Client — Mandatory

All HTTP requests must use `mindustrytool.services.Request`.

Do not construct HTTP clients directly outside `Request.java`, including:

* `HttpURLConnection`
* `URL.openConnection()`
* Other direct HTTP connection APIs

Use either existing facades:

```java
MindustryTool.getSession();
Github.getReleases();
```

Or own a configured `Request` instance:

```java
private final Request api = Request.builder()
        .baseUrl(Config.API_URL)
        .timeout(Duration.ofSeconds(10))
        .authProvider(authProvider)
        .build();
```

Direct connection construction is forbidden outside `Request.java`.

---

# Java Compatibility — Mandatory

## Language vs Runtime

The project supports **Java 17 language syntax** through its compiler/desugaring toolchain, but runs against a **Java 8 runtime environment**.

You may use supported modern language syntax such as:

* `var`
* Switch expressions
* Text blocks

However, **do not use Java standard library APIs introduced after Java 8** unless they are explicitly provided by an included library or backport.

### Common Replacements

| Do not use                             | Use instead                                            |
| -------------------------------------- | ------------------------------------------------------ |
| `java.util.function.*` (`Function`, `Supplier`, `Consumer`, `Predicate`, `BiFunction`, …) | `arc.func.*` (`Func`, `Func2`, `Prov`, `Cons`, `Cons2`, `Boolf`, `Boolf2`) — `java.util.function` default/static methods need D8 desugaring companions (`Function$-CC`) that crash on Android under Mindustry's `ModClassLoader` |
| `List.of()`, `Set.of()`, `Map.of()`    | Java 8 collections, `Arrays.asList()`, Arc collections |
| `stream.toList()`                      | `collect(Collectors.toList())`                         |
| `String.isBlank()`                     | `trim().isEmpty()`                                     |
| `String.strip()`                       | `trim()`                                               |
| `Optional.isEmpty()`                   | `!isPresent()`                                         |
| `Predicate.not()`                      | Lambda                                                 |
| `takeWhile()` / `dropWhile()`          | Filters or loops                                       |
| `readAllBytes()` / `transferTo()`      | Java 8-compatible streams                              |
| `Files.readString()` / `writeString()` | `Fi` or Java 8 I/O                                     |

Always verify Java 8 runtime compatibility before finishing.

---

# Nullability — Mandatory

By default, values are non-nullable.

If a field, parameter, local value, or return value can legitimately be `null`, annotate it with:

```java
import arc.util.Nullable;
```

Example:

```java
public @Nullable Dialog getSettingDialog() {
    return null;
}

public void process(@Nullable String value) {
    if (value != null) {
        // ...
    }
}

private @Nullable String cachedToken;
```

Do not use other `@Nullable` annotations.

Only add null checks when null is genuinely possible.

---

# Java Imports — Mandatory

Fully qualified class names are forbidden. Always import the type and use the simple class name instead.

Use the following regex to search for fully qualified class names in the codebase and replace them with imports:

```regex
^(?!\s*import\b).*?\b[a-z_][\w]*(?:\.[a-zA-Z_][\w]*)*\.[A-Z][\w]*\b
```

❌ Avoid:

```java
arc.scene.ui.ImageButton.ImageButtonStyle
```

✅ Good:

```java
import arc.scene.ui.ImageButton.ImageButtonStyle;

ImageButtonStyle
```

Fully qualified names are allowed only when strictly necessary to resolve unavoidable naming conflicts.

---

# Simple Conditions: Prefer Ternary Operator — Mandatory

Prefer the ternary operator (`? :`) over `if`/`if-else` blocks or mutable variables for simple condition checks, variable initialization, and method arguments.

❌ Bad:

```java
String title;
if (name != null) {
    title = name;
} else {
    title = "Unknown";
}

if (previewHeight != null) {
    card.height(previewHeight);
} else {
    card.height(unit(58));
}
```

✅ Good:

```java
String title = name != null ? name : "Unknown";

card.height(previewHeight != null ? previewHeight : Readable.of(unit(58f)));
```

Use `if` statements when branches involve multiple statements, side effects, early returns, or complex nested logic.

---

# Legacy Code

**Ignore the `old/` folder entirely.**

Do not modify or refactor it unless explicitly requested.

Treat it as legacy code outside the scope of normal tasks.

---

# Architecture & Coding Style

## Feature-Oriented Architecture

Organize code primarily around **features and game functionality**, not artificial technical layers.

Prefer:

```text
features/
├── settings/
│   ├── FeatureSettingDialog
│   ├── FeatureSettingsView
│   └── FeatureCard
├── chat/
│   ├── ChatFeature
│   └── ChatView
└── server/
    ├── ServerFeature
    └── ServerDialog

core/
signal/
ui/
utils/
```

Avoid backend-style architecture unless the project genuinely needs it.

Do not introduce unnecessary:

* Repository layers
* DAO layers
* Service layers for trivial logic
* Dependency injection frameworks
* Controllers
* DTOs
* Mappers
* Request/response architecture
* Enterprise abstractions

Prefer direct, understandable code.

## SOLID — Pragmatic Use

Follow SOLID principles when they improve the code.

Do not apply them mechanically.

Classes should have a clear responsibility, but do not split simple functionality into unnecessary layers.

## Interfaces

Do not create interfaces for every class.

Create an interface only when there is a meaningful behavioral contract or multiple implementations.

Good:

```java
Component
Disposable
Feature
```

Avoid meaningless patterns such as:

```text
FeatureManagerInterface
FeatureManagerImpl
```

unless multiple implementations are genuinely required.

## One Source of Truth

Do not duplicate state.

❌ Avoid:

```text
boolean enabled
Signal<Boolean> enabledSignal
```

Prefer a single reactive source:

```java
private final Signal<Boolean> enabled = Signal.of(false);
```

Expose normal and reactive access when useful:

```java
public boolean isEnabled() {
    return enabled.get();
}

public void setEnabled(boolean value) {
    enabled.set(value);
}

public Readable<Boolean> enabled() {
    return enabled;
}
```

---

# Solim UI — Mandatory

## Module Separation: Internal Engine Isolation (`:solim-runtime`) — Mandatory

**The `mod` module must ONLY depend on `:solim` and `:solim-mcp`. It must NEVER depend on or reference `:solim-runtime`.**

- `:solim-api` contains core public contracts (`Component`, `Disposable`, `SpacingAware`, `SchedulableEffect`, `ReactiveObserver`).
- `:solim` is the public API library and facade for mod development (`solim.UI`).
- `:solim-core` contains public UI components, layouts, and reactive types (`BaseComponent`, `Signal`, `Computed`, `Effect`, `Ui`, etc.).
- `:solim-runtime` contains the private engine runtime (`AttachmentStack`, `OwnershipContext`, `ReactiveContext`, `SignalDispatcher`, `StructuralReconciler`). It is an internal `implementation` dependency physically excluded from `:mod`'s compile classpath.

Mod code must use the public `solim.UI.*` declarative facades. Attempting to reference `:solim-runtime` classes or calling `own()` will result in compile-time errors.

## Dialogs, Buttons & Layout Conventions — Mandatory

### Dialog Content: Centered & Width-Constrained

Every settings/HUD dialog must center its content and constrain its width:

1. Call `maxWidth(500f)` on the dialog so content columns never stretch full-width.
2. The view's root must be `column().grow().center()` so content is centered both ways.

```java
// Dialog
name("musicSettingDialog");
addCloseButton();
closeOnBack();
maxWidth(500f);
children(() -> new MusicSettingsView(feature));
```

```java
// View root
@Override
protected Element build() {
    return column().grow().center().children(() -> {
        scroll().growY().children(() -> { /* sections */ });
    }).element();
}
```

### Button Sizes — Mandatory

Standard buttons use a uniform size of `unit(11)` with an icon size of `unit(6)`:

```java
button()
        .style(WebStyles.ghost())
        .size(unit(11))
        .tooltip(...)
        .children(() -> icon(Icon.add).size(unit(6)));
```

Small inline exceptions (e.g. dense list rows) may use smaller sizes, but default to `unit(11)` / `unit(6)`.

### Collections: Prefer `reactiveGrid` — Mandatory

For item collections (lists, grids, cards):

* **Prefer `reactiveGrid(...)`** for reactive, keyed collections.
* **Never use the static `grid(...)` layout** for dynamic content; it severs reactivity.
* **Use `wrap()` for flexible flowing layouts** (e.g. chips, tags, buttons that flow across multiple lines).

### WebStyles — Mandatory

Use `WebStyles` (`WebStyles.ghost()`, `WebStyles.secondary()`, `WebStyles.outline()`, `WebStyles.filterChip()`, `WebStyles.Colors.*`) for button styles and colors whenever possible instead of ad-hoc colors and raw Arc button styles.

## Solim-First

**All application UI must use Solim and follow its declarative style.**

Prefer `solim.*` APIs over direct `arc.scene.*` APIs.

Do not manually construct Arc widgets when Solim provides an equivalent abstraction.

Avoid imperative UI construction such as:

```java
Table table = new Table();
Label label = new Label("Title");

table.add(label);
table.row();
```

Prefer declarative Solim code:

```java
column()
        .grow()
        .gap(unit(2))
        .children(() -> {
            text("Title");

            row()
                    .gap(unit(1))
                    .children(() -> {
                        text("Label");
                        button("Action", this::onAction);
                    });
        });
```

Direct Arc usage is allowed only when:

1. Solim has no reasonable equivalent.
2. Arc interoperability with Mindustry is required.
3. Solim itself needs access to Arc internals.

When direct Arc usage is necessary, isolate it behind Solim abstractions when practical.

**Do not bypass Solim for convenience.**

---

# Declarative UI Style

UI should describe **what the UI is**, not manually describe how to construct and mutate it step by step.

Prefer clear component hierarchies:

```java
@Override
protected Element build() {
    return column(() -> {
        header();
        content();
        footer();
    });
}
```

## Modifier Order

Keep modifiers visually associated with their component.

Use this general order:

1. Create component
2. Size and growth
3. Spacing
4. Alignment
5. Visual styling
6. Behavior and events
7. `children()` last

Example:

```java
row()
        .growX()
        .padding(unit(2))
        .gap(unit(1))
        .left()
        .children(() -> {
            // children
        });
```

For complex UI, preserve readability through nested declarative `children()` blocks rather than introducing unnecessary temporary variables.

---

# Reactive UI

Use Solim's reactive APIs:

* `Signal<T>`
* `Computed<T>`
* `Readable<T>`
* Reactive component bindings

Application code should declare relationships; Solim should manage subscriptions and updates.

Prefer:

```java
Computed<String> label = value.map(v -> v + "%");
text(label);
```

Do not manually update Arc widgets when a Solim binding can express the relationship.

❌ Avoid:

```java
value.subscribe(v -> {
    label.setText(v + "%");
});
```

## ❌ Never Call `signal.get()` in `build()` — Mandatory

**Never unwrap signals using `.get()` during component construction.**

Calling `signal.get()` in `build()` extracts a static snapshot once and severs all reactivity. The UI will never update when the signal changes.

Pass the `Readable<T>` / `Signal<T>` directly to the component, or use `.map()` to transform it. If an untracked one-time read is truly intentional (such as inside an `onClick` callback), use `signal.peek()`.

❌ Bad (Static snapshot, breaks reactivity):
```java
@Override
protected Element build() {
    float scale = scaleSignal.get(); // ❌ Severed! Will not update on scale change
    int cols = colsSignal.get();       // ❌ Severed!
    String name = nameSignal.get();    // ❌ Severed!

    return column(() -> {
        text(name);
        text(countSignal.get() + " items"); // ❌ Not reactive!
        grid(cols).children(() -> ...);
        button().size(48f * scale);         // ❌ Static size!
    });
}
```

✅ Good (Declarative reactive flow):
```java
@Override
protected Element build() {
    Readable<Float> buttonSize = scaleSignal.map(s -> 48f * s);
    Readable<String> countText = countSignal.map(c -> c + " items");

    return column(() -> {
        text(nameSignal);
        text(countText);
        grid(colsSignal).children(() -> ...);
        button().size(buttonSize);
    });
}
```

---

# Automatic Component Binding

Solim components should accept both static and reactive values.

For example:

```java
text("Hello");
text(username);
```

Properties should support reactive values where appropriate:

```java
text(status);

button("Save")
        .visible(canSave)
        .enabled(canSave);

card()
        .width(cardWidth);

button()
        .color(enabled.map(value ->
                value ? Color.green : Color.scarlet
        ));
```

The component must:

1. Detect reactive values.
2. Install bindings automatically.
3. Update the underlying Arc element.
4. Own and dispose those bindings automatically.

Application developers should not manually subscribe for normal UI properties.

---

# Binding and Resource Ownership

Resources created by Solim belong automatically to their creating component.

This includes:

* Child components
* Reactive bindings
* Effects
* Solim event listeners
* Input controls
* Structural reactive children

Application code must not manually manage normal component ownership. `BaseComponent.own()` is package-private and inaccessible outside `solim.core`.

Instead, use declarative ambient ownership:
* Use `effect(() -> ...)` for reactive side effects.
* Use `listen(EventType.class, listener)` for event listeners.
* Use `createSignal(...)` for event-backed signals.
* Child components declared in `children()` blocks or instantiated in `build()` are automatically attached and owned.

Components created during `build()` should automatically become children of the current component.

When a parent is disposed, its owned children and resources must also be disposed.

---

# Effects

Use `Effect` only for genuine side effects.

Good:

```java
effect(() -> {
    Log.info("Feature enabled: " + enabled.get());
});
```

```java
effect(this::saveConfig);
```

Do not use effects for ordinary UI property updates.

❌ Avoid:

```java
Effect.of(() -> {
    label.setText(name.get());
});
```

That behavior belongs inside the relevant Solim component binding.

---

# Structural Reactivity

Property changes should update existing elements.

Structural changes require structural reactive components.

Use structural reactivity for:

* Dynamic lists
* Dynamic children
* Conditional UI
* Keyed collections

Example:

```java
reactiveGrid(
        features,
        Feature::id,
        FeatureCard::new
);
```

For keyed collections, preserve existing components whenever possible:

* Added item → create component
* Removed item → remove and dispose component
* Existing key → reuse component
* Order changed → reorder existing elements

Do not clear and rebuild an entire UI tree when targeted structural updates are possible.

---

# Build Once, Bind Automatically

Normal lifecycle:

```text
build()
    ↓
Create Arc elements once
    ↓
Install bindings automatically
    ↓
Signals update existing elements
```

Do not rebuild an entire component when only a property changes.

Rebuild or reconcile structure only when the actual structure changes.

Solim uses Arc's retained UI tree.

Do not introduce:

* Virtual DOM
* Global diffing
* Full render reconciliation

Prefer:

```text
Property change
    → Update existing element

Structural change
    → Targeted keyed reconciliation
```

---

# Mindustry and Arc APIs

## Do Not Add Unnecessary Null Checks

During normal mod runtime, guaranteed `Core.*` and `Vars.*` values should be used directly.

Do not write defensive checks such as:

```java
if (Core.app != null) {
    Core.app.post(...);
}
```

Use:

```java
Core.app.post(...);
```

This applies to normal runtime APIs such as:

```text
Core.app
Core.graphics
Core.scene
Core.bundle
Core.atlas
Core.settings
Core.camera

Vars.ui
Vars.player
Vars.state
Vars.world
Vars.control
```

Only check for null when null is genuinely possible, such as:

* Explicitly nullable APIs
* Optional implementations
* User-provided values
* Uncertain lifecycle states

## Do Not Wrap APIs Without Value

Do not create wrappers that merely rename Mindustry or Arc APIs.

❌ Avoid:

```java
public static void addToScene(Element element) {
    Core.scene.add(element);
}
```

Create abstractions only when they provide meaningful functionality such as:

* Declarative integration
* Automatic ownership
* Reactive binding
* Reusable behavior

---

# Testing Rules

## Mod UI

UI tests are **not required** for the `mod/` module.

The mod runs inside Mindustry's initialized runtime, where scene, graphics, skins, fonts, atlas, and game state are available. Headless tests for mod UI are prone to runtime and mocking failures.

## Solim

UI primitives and framework behavior in the `solim/` module should be tested.

This includes:

* Components
* Reactive bindings
* Layout behavior
* Signals
* Lifecycle ownership
* Structural reactivity
* Clean up/Dispose test

Test framework behavior where it can be tested independently of the Mindustry runtime.

## Value-Driven Testing Philosophy

A small number of high-value tests is better than a large number of meaningless tests.

Before writing a test, ask:

> **"What real behavior would break if this test failed?"**

If the answer is unclear, do not write the test.

Tests should survive internal refactors: if the implementation is completely rewritten while preserving public behavior, the tests must still pass. Do not test private fields or internal implementation structure.

### ❌ Do NOT Write Tests Simply To:

* Verify a constant contains a specific value
* Verify a collection has the expected hardcoded entries
* Verify getters return non-null values
* Verify strings are non-empty
* Verify implementation details
* Increase code coverage
* Restate the production code in test form
* Test that something does not contain an arbitrary historical bug unless that behavior is an explicit contract

### ✅ Prefer Tests That Verify:

* User-visible behavior
* Public API contracts
* State transitions
* Component interaction
* Event/signal propagation
* Lifecycle/disposal behavior
* Edge cases that have meaningful semantics
* Regressions for bugs that actually occurred
* Invariants that must remain true during refactoring

### UI Component Testing

For UI components, prioritize behavioral tests such as:

* Correct children are created
* Configuration actually affects layout/behavior
* Signals update the UI
* UI changes update the underlying state
* Disposal removes listeners/effects
* Repeated mounting/unmounting does not leak
* User interaction produces the expected state change

---

# Before Completing Any Task

Verify:

### Internationalization

* [ ] No new user-visible text is unnecessarily hardcoded.
* [ ] Existing translation keys were reused where appropriate.
* [ ] Every new display string has a translation key.
* [ ] New keys were added to `assets/bundles/bundle.properties`.
* [ ] Every new key has a descriptive comment directly above it.
* [ ] Placeholder meanings are documented.
* [ ] Dynamic text uses `Core.bundle.format()` where appropriate.
* [ ] No duplicate translation keys were introduced.

### Java

* [ ] All HTTP calls go through `Request`.
* [ ] No unsupported Java 9+ runtime APIs are used.
* [ ] Nullable values use `arc.util.Nullable`.
* [ ] Fully qualified class names are forbidden and replaced with imports (use regex to check).
* [ ] Ternary operator is preferred over `if` for simple conditions and assignments.

### Architecture

* [ ] The implementation follows feature-oriented game-mod architecture.
* [ ] No unnecessary enterprise layers or abstractions were introduced.
* [ ] State has a clear source of truth.
* [ ] `old/` was not modified unless explicitly requested.

### UI & Cleanup

* [ ] Solim was used instead of direct Arc UI where possible.
* [ ] UI is declarative and readable.
* [ ] Reactive properties use automatic component bindings.
* [ ] Effects are only used for genuine side effects.
* [ ] Bindings and resources are automatically owned and disposed.
* [ ] Structural changes use targeted reconciliation instead of unnecessary full rebuilds.
* [ ] If Mindustry was started to test UI, it has been stopped using the MCP `stop` tool.

**A UI or player-facing feature is not complete until its user-visible text is properly translated and its UI follows the Solim architecture.**
