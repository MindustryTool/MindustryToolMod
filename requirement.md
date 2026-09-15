# Implement SimpleUI Core Architecture

Implement and/or refactor the SimpleUI library into a lightweight, reactive UI framework built on top of **Anuken Arc / Mindustry Scene2D**.

The framework should prioritize simplicity, performance, and direct mutation of Arc `Element`s.

---

# Core Philosophy

The framework must follow these principles:

* No Virtual DOM
* No diffing
* No reconciliation
* No component re-render loop
* No React-style hooks
* No JSX
* UI elements are built once
* Arc's `Element` tree is the only render tree
* Reactive state directly updates Arc element properties
* Keep wrappers thin and Arc-native
* Do not recreate a browser/CSS engine

The primary reactive flow should be:

```text
Signal / Computed
        ↓
Effect / Reactive Binding
        ↓
Arc Element Property
```

Example:

```java
Signal<Boolean> darkMode = Signal.of(false);

button("Toggle")
    .style(
        darkMode.map(enabled ->
            enabled
                ? Styles.PRIMARY
                : Styles.GHOST
        )
    );
```

When `darkMode` changes, the underlying Arc element should update directly.

**Do not rebuild the component.**

---

# 1. Reactive System

Implement a small, clean reactive system.

## Signal

Support:

```java
Signal<Integer> count = Signal.of(0);

count.get();
count.set(10);
```

Requirements:

* Equality check before notifying
* Support subscriptions/listeners
* Return a disposable subscription
* Automatically track reads while evaluating `Computed`
* Automatically track reads while evaluating `Effect`

Example:

```java
Subscription subscription = count.subscribe(value -> {
    Log.info("Count: @", value);
});

subscription.dispose();
```

Signals themselves should remain simple reactive values.

---

# 2. Computed

Support:

```java
Computed<String> title = Signal.computed(() ->
    "Count: " + count.get()
);
```

Requirements:

* Automatic dependency tracking
* Lazy recomputation
* Dependency cleanup when dependencies change
* Dynamic dependencies

Example:

```java
Computed<String> value = Signal.computed(() -> {
    if (darkMode.get()) {
        return username.get();
    }

    return email.get();
});
```

When `darkMode` changes, dependencies must update correctly.

Requirements:

* Detect cycles safely
* Do not leak old dependencies
* Propagate invalidation to dependent Computeds
* Support subscriptions/listeners
* Support `dispose()`

Also support:

```java
signal.map(...)
```

Example:

```java
Computed<String> text = enabled.map(value ->
    value ? "Enabled" : "Disabled"
);
```

---

# 3. Effect

Add first-class `Effect` support.

Effects are for executing side effects when reactive dependencies change.

Example:

```java
Effect effect = Effect.of(() -> {
    Log.info("Dark mode: @", darkMode.get());
});
```

Or:

```java
effect(() -> {
    Log.info("Dark mode: @", darkMode.get());
});
```

Requirements:

* Automatically track every `Signal` and `Computed` read during execution
* Automatically subscribe to those dependencies
* Re-run when any dependency changes
* Support dynamic dependencies
* Remove old dependencies before collecting new ones
* Be disposable
* Prevent dependency leaks
* Avoid recursive/infinite execution where possible
* Handle errors safely and log them

Example:

```java
Signal<Boolean> enabled = Signal.of(true);
Signal<String> username = Signal.of("Player");
Signal<String> email = Signal.of("player@example.com");

Effect effect = Effect.of(() -> {

    if (enabled.get()) {
        Log.info(username.get());
    } else {
        Log.info(email.get());
    }

});
```

If `enabled` changes, the effect must stop depending on the old branch and start depending on the new branch.

---

## Effect Cleanup

Support cleanup from effects.

Example desired API:

```java
Effect.of(() -> {

    Subscription subscription = api.events().subscribe(...);

    return subscription::dispose;

});
```

If the API design makes returning cleanup difficult because of Java functional interfaces, provide a clean alternative such as:

```java
Effect.of(cleanup -> {

    Subscription subscription = api.events().subscribe(...);

    cleanup.add(subscription::dispose);
});
```

Requirements:

* Run previous cleanup before re-running the effect
* Run cleanup when the effect is disposed
* Cleanup errors must not prevent other cleanup or disposal
* Log cleanup failures safely

Do not overcomplicate the API.

---

# 4. Reactive Context

The reactive system needs a shared dependency tracking mechanism.

It must support both:

```text
Computed
Effect
```

The currently executing reactive observer should be able to collect dependencies when:

```java
signal.get();
computed.get();
```

is called.

The implementation may use a stack-based context because rendering and UI logic run on a single thread.

Do not use `ThreadLocal` unless genuinely necessary.

Expected conceptual flow:

```text
Effect starts
    ↓
Push active observer
    ↓
Signal.get()
    ↓
Register dependency
    ↓
Pop active observer
```

The same mechanism should work for `Computed`.

Consider introducing a shared internal abstraction such as:

```java
ReactiveObserver
```

implemented by:

```text
Computed
Effect
```

Keep this internal API simple and clear.

---

# 5. Component System

Use a minimal component abstraction.

```java
public interface Component {

    Element element();

    default void dispose() {
    }
}
```

Optionally provide a convenient base class:

```java
public abstract class BaseComponent implements Component {

    private Element element;

    protected abstract Element build();

    @Override
    public final Element element() {
        if (element == null) {
            element = build();
        }

        return element;
    }

    @Override
    public void dispose() {
    }
}
```

Requirements:

* `build()` runs once
* Components do not automatically re-render
* Components can contain `Signal`, `Computed`, and `Effect` as normal class fields
* Components can be passed as children to layouts
* Child components must resolve automatically to their `Element`

Example:

```java
public final class Counter extends BaseComponent {

    private final Signal<Integer> count = Signal.of(0);

    private final Computed<String> text =
        count.map(value -> "Count: " + value);

    @Override
    protected Element build() {

        return button(
            text,
            () -> count.set(count.get() + 1)
        );
    }
}
```

---

# 6. Component Lifecycle and Disposal

Components must support proper cleanup.

Reactive bindings and effects create resources and subscriptions that need disposal.

Example:

```java
public final class ChatPanel extends BaseComponent {

    private Effect effect;

    @Override
    protected Element build() {

        effect = Effect.of(() -> {
            Log.info("Message count: @", messages.get().size);
        });

        return ...;
    }

    @Override
    public void dispose() {
        effect.dispose();
    }
}
```

Provide convenient lifecycle cleanup where practical.

However:

* Do not introduce a complicated ownership/scope system
* Do not introduce React hooks
* Keep lifecycle explicit and understandable

Lifecycle:

```text
constructor
    ↓
build()
    ↓
element mounted
    ↓
dispose()
```

---

# 7. Implicit Parent Stack UI Declaration

Implement an implicit parent stack for declarative UI construction.

Desired syntax:

```java
column(() -> {

    text("Settings");

    row(() -> {

        button("Cancel");

        button("Save");

    });

});
```

Internally:

```text
push Column

    add Text

    push Row

        add Button
        add Button

    pop Row

pop Column
```

Do NOT implement:

```java
startComponent();
endComponent();
```

Use lambda scopes so stack cleanup is guaranteed with `try/finally`.

Requirements:

```java
column(() -> {});
row(() -> {});
stack(() -> {});
grid(...);
wrap(() -> {});
scroll(() -> {});
```

Every created child should automatically attach to the current parent.

Example:

```java
column(() -> {

    text("Chat");

    row(() -> {

        textField(input);

        button("Send", this::send);

    });

});
```

The framework must guarantee stack cleanup even if an exception occurs.

---

# 8. Child Resolution

Layouts should support children that are:

```text
Element
Component
```

Automatically resolve:

```text
Component → component.element()
Element   → element
```

Example:

```java
column(() -> {

    add(new Header());

    add(new ChatPanel());

});
```

Avoid requiring:

```java
new Header().element();
```

everywhere.

---

# 9. Reactive Property Binding

Widgets should support both static and reactive values.

Example:

```java
text("Hello");
```

and:

```java
text(username);
```

where `username` is a `Signal<String>` or `Computed<String>`.

Reactive bindings should:

1. Apply the current value immediately
2. Subscribe to future changes
3. Update the Arc element directly
4. Be disposable

Examples:

```java
text(username);

button("Save")
    .visible(isLoggedIn)
    .enabled(canSave);

button("Toggle")
    .style(buttonStyle);
```

Do not rebuild the widget when a value changes.

---

# 10. Styling System

Create a lightweight styling system.

Architecture:

```text
Style
  ↓
Widget
```

## Static Style

```java
button("Save")
    .style(Styles.PRIMARY);
```

## Reactive Style

```java
button("Save")
    .style(
        dirty.map(value ->
            value
                ? Styles.PRIMARY
                : Styles.GHOST
        )
    );
```

When the signal changes:

```text
Signal changes
      ↓
New Style resolved
      ↓
Entire style applied to Element
```

Do not implement style diffing unless clearly required by Arc internals.

Do not implement CSS.

Do not implement a CSS parser.

Do not recreate CSS Grid or Flexbox.

Style objects should preferably be immutable.

---

# 11. Layout Components

Implement the following core layout primitives.

```text
Column
Row
Stack
Grid
Wrap
Scroll
Container
Spacer
Divider
```

---

## Row / Column

Support basic Flexbox-like concepts:

```java
row()
    .gap(8)
    .justify(Justify.BETWEEN)
    .align(Align.CENTER);
```

Supported justification:

```text
START
CENTER
END
BETWEEN
AROUND
EVENLY
```

Supported alignment:

```text
START
CENTER
END
STRETCH
```

Use Arc's existing layout mechanisms wherever possible.

Do not attempt to perfectly reproduce CSS Flexbox.

---

## Grow

Support:

```java
cell(textField(input))
    .growX();
```

and convenient APIs where possible:

```java
textField(input)
    .growX();
```

Map this to Arc layout behavior.

---

## Spacer

Support:

```java
row(() -> {

    button("Back");

    spacer();

    button("Save");

});
```

The spacer should consume remaining available space.

---

## Grid

Support a simple application-oriented grid.

Example:

```java
grid(3, () -> {

    button("One");
    button("Two");
    button("Three");

});
```

Also consider:

```java
grid()
    .columns(3)
    .gap(8);
```

Do not implement the complete CSS Grid specification.

Responsive grid support may be implemented later:

```java
grid()
    .minCellWidth(250);
```

---

## Wrap

Support basic wrapping:

```java
wrap(() -> {

    text("Java");
    text("Mindustry");
    text("Arc");

});
```

Children should wrap to the next line when necessary.

---

## Stack

Support overlayed children:

```java
stack(() -> {

    image(background);

    text("Loading");

});
```

---

# 12. Core Components

Implement only the following core components initially.

## Layout

```text
Column
Row
Stack
Grid
Wrap
Scroll
SplitPane
Spacer
Divider
Container
```

Implement `SplitPane` only if Arc provides sufficient primitives.

---

## Display

```text
Text
Image
Icon
Badge
Avatar
```

Start with:

```text
Text
Image
Icon
```

Badge and Avatar can remain lightweight convenience components.

---

## Input

```text
Button
IconButton
TextField
TextArea
Checkbox
Switch
Slider
Select
```

Start with:

```text
Button
TextField
Checkbox
Switch
Slider
Select
```

Do not implement advanced controls until the core architecture is stable.

---

## Overlay

```text
Dialog
Popup
```

Use Arc's native overlay/dialog mechanisms whenever possible.

---

## Feedback

```text
Spinner
ProgressBar
Alert
```

---

# 13. Component Composition

Custom components should be normal Java classes.

Example:

```java
public final class UserCard extends BaseComponent {

    private final Signal<User> user;

    public UserCard(Signal<User> user) {
        this.user = user;
    }

    @Override
    protected Element build() {

        return row(() -> {

            text(user.map(User::name));

        });
    }
}
```

Usage:

```java
column(() -> {

    add(new UserCard(user));

});
```

Constructor arguments are the equivalent of props.

Do not create a React-style props system.

---

# 14. Package Structure

Use a clean package structure similar to:

```text
simpleui/
│
├── core/
│   ├── Component
│   ├── BaseComponent
│   ├── Disposable
│   └── Subscription
│
├── signal/
│   ├── Signal
│   ├── Computed
│   ├── Effect
│   ├── ReactiveObserver
│   └── ReactiveContext
│
├── ui/
│   ├── Ui
│   ├── ParentStack
│   ├── ElementResolver
│   └── Binding
│
├── layout/
│   ├── Column
│   ├── Row
│   ├── Stack
│   ├── Grid
│   ├── Wrap
│   ├── Scroll
│   ├── Container
│   ├── Spacer
│   └── Divider
│
├── display/
│   ├── Text
│   ├── Image
│   └── Icon
│
├── input/
│   ├── Button
│   ├── TextField
│   ├── Checkbox
│   ├── Switch
│   ├── Slider
│   └── Select
│
├── overlay/
│   ├── Dialog
│   └── Popup
│
├── feedback/
│   ├── Spinner
│   ├── ProgressBar
│   └── Alert
│
└── style/
    ├── Style
    └── Styles
```

---

# 15. Implementation Rules

Follow these rules strictly:

1. Prefer Arc-native functionality over reimplementation.
2. Do not create a Virtual DOM.
3. Do not create a renderer/reconciliation system.
4. Do not rebuild entire components for state changes.
5. Signals and Computeds update Arc elements directly through bindings.
6. Effects handle reactive side effects.
7. Keep components normal Java classes.
8. Constructor parameters are props.
9. Use the implicit parent stack for UI declaration.
10. Ensure parent stack cleanup with `try/finally`.
11. Keep `Signal`, `Computed`, and `Effect` usable as normal class fields.
12. Ensure all subscriptions and effects are disposable.
13. Avoid unnecessary abstractions.
14. Do not attempt to implement CSS.
15. Do not copy React architecture.
16. Prefer composition over giant component APIs.
17. Keep the library small and modular.
18. Use immutable style definitions where practical.
19. Add tests for reactive dependency tracking.
20. Add tests for dynamic dependencies.
21. Add tests for Computed disposal.
22. Add tests for Effect disposal and cleanup.
23. Add tests ensuring disposed effects no longer react.

---

# 16. Example Final API

The target developer experience should look approximately like this:

```java
public final class SettingsPanel extends BaseComponent {

    private final Signal<Boolean> darkMode =
        Signal.of(false);

    private final Signal<Boolean> dirty =
        Signal.of(false);

    private final Computed<String> saveText =
        dirty.map(value ->
            value
                ? "● Save Changes"
                : "Save Changes"
        );

    private Effect logger;

    @Override
    protected Element build() {

        logger = Effect.of(() -> {
            Log.info("Dirty state: @", dirty.get());
        });

        return column(() -> {

            text("Settings");

            divider();

            row(() -> {

                text("Dark Mode");

                button(
                    darkMode.map(value ->
                        value ? "On" : "Off"
                    ),
                    () -> {
                        darkMode.set(!darkMode.get());
                        dirty.set(true);
                    }
                )
                    .style(
                        darkMode.map(value ->
                            value
                                ? Styles.PRIMARY
                                : Styles.GHOST
                        )
                    );

            });

            spacer();

            row(() -> {

                button("Discard", () -> {
                    dirty.set(false);
                });

                button(saveText, () -> {
                    dirty.set(false);
                })
                    .enabled(dirty)
                    .style(Styles.PRIMARY);

            })
                .justify(Justify.END)
                .gap(8);

        })
            .padding(24)
            .gap(16);
    }

    @Override
    public void dispose() {
        logger.dispose();
    }
}
```

---

# Final Architecture

The framework should fundamentally work like this:

```text
                 Signal
                   │
                   ▼
                Computed
                   │
          ┌────────┴────────┐
          ▼                 ▼
       Effect        UI Binding
          │                 │
          ▼                 ▼
      Side Effect      Arc Element
```

UI construction:

```text
Developer API
      ↓
Implicit Parent Stack
      ↓
Arc Element Tree
```

Before implementing advanced components, establish and test the foundation in this order:

1. Signal
2. Computed
3. Effect
4. Reactive dependency tracking
5. Subscription and disposal
6. Parent stack
7. Element/component resolution
8. Reactive bindings
9. Row/Column
10. Text
11. Button
12. Style binding

Build everything else on top of this foundation.

The final library should feel declarative for developers while remaining fundamentally imperative underneath.
