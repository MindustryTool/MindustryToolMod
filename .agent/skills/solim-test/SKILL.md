---

name: solim-testing
description: Write, review, and improve tests for Solim UI components, reactive primitives, bindings, lifecycle ownership, disposal, layout behavior, and structural UI updates. Use when creating tests for any Solim code, fixing failing Solim tests, reviewing test coverage, or verifying reactive and lifecycle behavior. Tests must verify observable behavior rather than merely checking that objects are non-null or that code does not throw. For reactive components, verify initial state, actual updates, disposal, and that updates stop after disposal.
license: MIT
compatibility: Requires Java, JUnit 5, the project's existing test framework, and the Solim/Arc UI test environment.
metadata:
author: mindustry-tool
version: "1.1"
--------------

# Solim Testing Skill

## Purpose

This skill defines how to write, review, and improve tests for the Solim UI library.

Solim is a fine-grained reactive UI framework built on Arc Scene UI.

Tests must primarily verify:

* observable UI behavior
* actual layout behavior
* reactive updates
* lifecycle ownership
* cleanup
* disposal
* structural changes
* validation and edge cases

Do not write tests that merely prove an object exists unless object creation itself is the behavior being tested.

# Assertion-to-Claim Rule

Every test name makes a behavioral claim.

The assertions must directly prove that claim.

Before accepting a test, compare:

1. Test name
2. Action performed
3. Assertion

They must all describe the same behavior.

Bad:

@Test
void reactiveColumnsUpdatesLayout() {
    Signal<Integer> columns = Signal.of(2);
    grid.columns(columns);

    grid.add(...);

    assertEquals(3, grid.table().getChildren().size);
}

Problem:
- Name claims reactive updates.
- Action never changes the Signal.
- Assertion only checks child count.

This test must be rejected.

---

## Feature Removal Test

For every feature test, mentally remove the feature implementation.

Ask:

> If `columns()` did absolutely nothing, would this test fail?

If no, the test does not test `columns()`.

Examples:

If this implementation is removed:

grid.columns(4);

then this test:

assertEquals(3, table.getChildren().size);

still passes.

Therefore the test is invalid for testing columns.

The same rule applies to:

- modifiers
- reactive bindings
- layout configuration
- validation
- lifecycle
- disposal
- reconciliation

Every test must fail when the specific feature it claims to test is removed or broken.

---

# Critical Rule: Do Not Write "Does Not Crash" Tests

The following pattern is usually a **low-value test**:

```java
@Test
void gridGapModifier() {
    Grid grid = new Grid(2).gap(16f);

    assertNotNull(grid.table());
}
```

This does not verify that `gap(16f)` works.

The test would still pass if:

* `gap()` does nothing
* the wrong value is applied
* the value is ignored
* the implementation is broken

Instead, verify the observable result:

```text
Call API
↓
Inspect actual Arc/UI state
↓
Verify expected behavior
```

Before accepting a test, ask:

> Would this test fail if the feature being tested were completely removed?

If the answer is **no**, improve or replace the test.

---

# When to Use This Skill

Use this skill when working on:

* Solim UI components
* `BaseComponent`
* `Component`
* `Signal`
* `Computed`
* `Effect`
* reactive bindings
* UI inputs
* lifecycle ownership
* `Disposable`
* dynamic UI
* `Dynamic`
* `ForEach`
* reactive collections
* keyed reconciliation
* UI attachment
* layout components
* Solim modifiers

Also use this skill when:

* adding a new Solim component
* fixing a Solim bug
* refactoring lifecycle code
* fixing reactive behavior
* investigating memory leaks
* reviewing missing test coverage
* replacing weak tests

---

# Testing Philosophy

## Test observable behavior

The primary question is:

> What should a Solim user observe?

Prefer:

```java
Signal<String> value = Signal.of("Hello");

Label label = (Label) component.element();

assertEquals("Hello", label.getText().toString());

value.set("World");

assertEquals("World", label.getText().toString());
```

Avoid:

```java
assertEquals(1, component.disposables.size());
assertEquals(2, effect.dependencies.size());
```

unless explicitly testing framework internals.

---

# Required Test Quality Check

For every test, verify that the assertion actually tests the feature named by the test.

Bad:

```java
@Test
void gridWithThreeColumns() {
    Grid grid = new Grid(3);

    assertNotNull(grid.table());
}
```

Better:

```java
@Test
void wrapsChildrenAfterConfiguredColumnCount() {
    Grid grid = new Grid(2);

    // Add enough children to exercise row wrapping.

    // Assert actual row/cell/layout behavior.
}
```

The exact assertion must depend on the real implementation.

Do not invent layout assertions without first inspecting how the component uses Arc `Table`, `Cell`, rows, or rebuilding.

---

# The Standard Solim Lifecycle Test

For lifecycle-sensitive components, test:

```text
Create
  ↓
Build
  ↓
Verify initial state
  ↓
React to updates
  ↓
Verify actual updated state
  ↓
Dispose
  ↓
Update source again
  ↓
Verify no further reaction
```

This is the primary lifecycle contract for Solim.

---

# Required Workflow

Before writing tests:

1. Inspect the implementation being tested.
2. Identify its public API contract.
3. Identify observable behavior.
4. Identify reactive dependencies.
5. Identify owned resources.
6. Identify validation rules.
7. Identify cleanup/disposal behavior.
8. Search for similar existing tests.
9. Reuse project test utilities.
10. Replace weak tests where necessary.
11. Run affected tests.

Do not invent behavior that the implementation does not support.

---

# 1. Component Construction and Caching

Test component behavior rather than only non-null values.

## Lazy Build

```java
@Test
void buildsOnlyOnce() {
    AtomicInteger builds = new AtomicInteger();

    BaseComponent component = new BaseComponent() {
        @Override
        protected Element build() {
            builds.incrementAndGet();
            return new Label("Test");
        }
    };

    Element first = component.element();
    Element second = component.element();

    assertSame(first, second);
    assertEquals(1, builds.get());
}
```

Required checks when applicable:

* [ ] Build is lazy.
* [ ] Build executes once.
* [ ] `element()` returns the cached instance.
* [ ] Repeated calls do not rebuild.

Prefer:

```java
assertSame(first, second);
```

over:

```java
assertNotNull(first);
assertNotNull(second);
```

when identity caching is the actual behavior.

---

# 2. Layout Component Tests

For components such as:

* `Grid`
* `Row`
* `Column`
* layout containers

test the actual effect of layout APIs.

## Children

Test:

* child count
* child order
* nesting
* attachment behavior

Example:

```java
@Test
void preservesChildOrder() {
    Grid grid = new Grid();

    Element first = new Element();
    Element second = new Element();
    Element third = new Element();

    grid.add(first);
    grid.add(second);
    grid.add(third);

    assertSame(first, grid.table().getChildren().get(0));
    assertSame(second, grid.table().getChildren().get(1));
    assertSame(third, grid.table().getChildren().get(2));
}
```

---

## Column Count

Do not test this:

```java
Grid grid = new Grid(3);
assertNotNull(grid.table());
```

Instead add enough children to exercise the behavior.

Test:

* configured column count
* wrapping behavior
* changing column count
* rebuilding/rearranging if supported

Example structure:

```java
@Test
void wrapsChildrenAfterConfiguredColumnCount() {
    Grid grid = new Grid(2);

    grid.add(new Element());
    grid.add(new Element());
    grid.add(new Element());

    // Assert the actual row/wrapping behavior
    // based on Grid's implementation.
}
```

---

## Gap

Do not only verify that `gap()` does not crash.

Test:

```text
Set gap
↓
Inspect relevant Arc Cell/Table state
↓
Verify gap value is applied
```

For reactive gap:

```text
Initial signal value
↓
Verify initial gap

Signal changes
↓
Verify actual gap changes
```

---

# 3. Reactive Component Tests

Every reactive component should test the full lifecycle.

## Initial State

```text
Signal has value
↓
Component builds
↓
UI reflects value
```

## Reactive Update

```text
Signal changes
↓
UI/layout actually changes
```

Do not write:

```java
signal.set(newValue);
assertNotNull(component.element());
```

This does not test reactivity.

Instead assert the changed value, structure, identity, layout state, or other observable result.

---

## Required Reactive Lifecycle

For every reactive feature:

```text
Create signal
↓
Bind component
↓
Verify initial value
↓
Change signal
↓
Verify actual update
↓
Dispose component
↓
Change signal again
↓
Verify no further update
```

### Required checks

* [ ] Initial signal value is applied.
* [ ] Signal updates cause the expected change.
* [ ] Multiple updates work.
* [ ] Disposal stops updates.

---

# 4. Reactive Layout Tests

Layout components with reactive APIs require tests for actual layout changes.

Example API:

```java
grid.columns(signal);
```

Required behavior:

```text
columns = 2
↓
Grid layout uses 2 columns

columns = 4
↓
Grid layout updates to 4 columns
```

The test must inspect the real behavior.

Do not only assert that the `Grid` still exists.

---

## After Disposal

Every reactive layout binding should also test:

```java
@Test
void stopsReactiveUpdatesAfterDisposal() {
    Signal<Integer> columns = Signal.of(2);

    Grid grid = new Grid();
    grid.columns(columns);

    // Build and capture observable state.

    grid.dispose();

    columns.set(4);

    // Verify the captured layout/state did not update.
}
```

Adapt the assertion to the actual implementation.

---

# 5. Validation and Invalid Input

Every public API that accepts constrained values should have a defined behavior.

Examples:

```java
new Grid(0);
new Grid(-1);

grid.columns(0);
grid.columns(-1);
```

Determine the intended contract.

Possible contracts:

### Reject

```java
assertThrows(
    IllegalArgumentException.class,
    () -> new Grid(0)
);
```

### Normalize

For example:

```text
0 → 1
-1 → 1
```

If normalization is intended, assert that actual behavior.

---

## Reactive Validation

Reactive overloads must follow the same rules.

Example:

```text
Signal starts with valid value
↓
Component works

Signal changes to invalid value
↓
Same validation behavior applies
```

Do not test only static APIs.

---

# 6. Fluent API Tests

For fluent Solim APIs, verify chaining behavior where it is part of the contract.

Example:

```java
@Test
void modifiersReturnSameComponent() {
    Grid grid = new Grid();

    assertSame(grid, grid.columns(3));
    assertSame(grid, grid.gap(16f));
}
```

Also test reactive overloads if they are intended to be fluent.

---

# 7. Repeated Configuration Behavior

When an API can be called multiple times, define and test the behavior.

Example:

```java
grid.children(() -> {
    ParentStack.add(first);
});

grid.children(() -> {
    ParentStack.add(second);
});
```

Determine whether the intended behavior is:

* append
* replace
* reject
* rebuild

Then test it explicitly.

The same principle applies to:

* repeated `gap()`
* repeated `columns()`
* repeated bindings
* repeated children blocks

---

# 8. Edge Cases

Consider edge cases for every component.

For layout components:

* [ ] zero children
* [ ] one child
* [ ] exactly one full row
* [ ] one item beyond a full row
* [ ] multiple rows
* [ ] empty children block
* [ ] invalid configuration
* [ ] repeated configuration
* [ ] reactive configuration changes

For reactive components:

* [ ] initial value
* [ ] multiple updates
* [ ] same value behavior
* [ ] dynamic dependencies
* [ ] disposal
* [ ] update after disposal

---

# 9. Disposal Tests

Every component that owns resources should test disposal.

## Basic Disposal

* owned resources are disposed
* disposal is safe
* reactive resources unsubscribe

## Idempotency

```java
component.dispose();
component.dispose();
```

Must follow the intended idempotency contract.

## Failure Isolation

If one cleanup fails, remaining resources should still be cleaned up when that is the framework contract.

---

# 10. Effect Tests

Effects require strong lifecycle coverage.

Test:

* [ ] runs initially
* [ ] tracks dependencies
* [ ] reruns when dependencies change
* [ ] handles dynamic dependencies
* [ ] removes old dependencies
* [ ] cleanup runs before rerun
* [ ] cleanup runs on disposal
* [ ] disposed effects stop reacting
* [ ] errors do not corrupt reactive context

---

# 11. Automatic Effect Ownership

When an Effect is created inside a Solim component lifecycle scope:

```java
Effect.of(() -> {
    signal.get();
});
```

test automatic ownership if supported by the framework.

Required lifecycle:

```text
Component builds
↓
Effect reacts

Component disposes
↓
Effect unsubscribes

Signal changes
↓
Effect does not run
```

Also verify Effects created outside component ownership are not accidentally disposed.

---

# 12. UI Binding Tests

For one-way bindings:

```text
Signal
↓
Initial UI state

Signal changes
↓
UI changes

Dispose component
↓
Signal changes

UI does not change
```

For two-way bindings:

```text
Signal → Widget
Widget → Signal
No feedback loop
Dispose → Binding stops
```

---

# 13. Dynamic UI and Structural Tests

For dynamic UI, test actual structure.

Test:

* initial children
* addition
* removal
* replacement
* child order
* no duplicates
* removed child disposal

Do not only test:

```java
assertNotNull(table);
```

after changing the source.

---

# 14. Keyed Reconciliation

For keyed collections, test identity preservation.

Example:

```text
[A, B, C]

→

[C, B, A]
```

Expected:

```text
Same component/element for A
Same component/element for B
Same component/element for C

Only order changes
```

Test:

* [ ] add
* [ ] remove
* [ ] reorder
* [ ] identity preservation
* [ ] removed child disposal
* [ ] empty collection
* [ ] duplicate key behavior

Use:

```java
assertSame(previousElement, currentElement);
```

when identity preservation is part of the contract.

---

# 15. Prefer Real Arc Objects

Use real Arc UI objects when possible:

```java
new Table();
new Label();
new Element();
new Image();
```

Do not over-mock Arc.

Solim is an integration layer over Arc UI, so real objects usually produce more valuable tests.

---

# 16. Test Isolation

Every test must clean up resources it creates.

Especially:

* Effects
* Components
* listeners
* subscriptions
* global state

Tests must not depend on execution order.

Avoid timing-based tests and sleeps unless absolutely unavoidable.

---

# Existing Test Review Rules

When reviewing an existing test suite, identify and improve tests that only check:

```java
assertNotNull(...)
```

after performing an action.

Examples of suspicious tests:

```java
modifier();
assertNotNull(component);
```

```java
signal.set(value);
assertNotNull(element);
```

```java
method();
assertDoesNotThrow(...);
```

These may be valid for construction/error-boundary tests, but they are **not sufficient feature tests**.

For every suspicious test, ask:

> What observable behavior should this API actually change?

Then test that behavior.

---

# Test Naming

Use behavior-focused names:

```text
buildsOnlyOnce
preservesChildOrder
wrapsChildrenAfterColumnLimit
updatesGapWhenSignalChanges
updatesLayoutWhenColumnSignalChanges
stopsReactiveUpdatesAfterDisposal
rejectsInvalidColumnCount
preservesChildrenByKeyWhenReordered
```

Avoid:

```text
testGrid
gridWorks
gridModifier
test1
```

---

# Minimum Requirements by Component Type

## Static Component

* [ ] Builds correctly.
* [ ] Builds once.
* [ ] Returns cached element.
* [ ] Actual configured behavior works.
* [ ] Children attach correctly.
* [ ] Disposal is safe.

## Layout Component

Additionally:

* [ ] Layout configuration produces observable changes.
* [ ] Child order is correct.
* [ ] Boundary/wrapping behavior is tested.
* [ ] Invalid values are handled.
* [ ] Reconfiguration behavior is tested.

## Reactive Component

Additionally:

* [ ] Initial value is applied.
* [ ] Source changes cause actual UI changes.
* [ ] Multiple updates work.
* [ ] Disposal stops updates.

## Reactive Layout Component

Additionally:

* [ ] Initial reactive configuration is applied.
* [ ] Signal changes update actual layout.
* [ ] Multiple configuration changes work.
* [ ] Invalid reactive values follow validation rules.
* [ ] Disposal stops reactive layout updates.

## Input Component

Additionally:

* [ ] Signal → UI works.
* [ ] UI → Signal works.
* [ ] No feedback loop.
* [ ] Disposal removes binding.

## Structural Component

Additionally:

* [ ] Initial structure is correct.
* [ ] Add works.
* [ ] Remove works.
* [ ] Reorder works.
* [ ] Removed children dispose.
* [ ] Stable keyed children preserve identity.

---

# Before Finishing

## Behavior Quality

* [ ] Every test verifies the behavior named in the test.
* [ ] A broken implementation would cause the test to fail.
* [ ] Tests do not merely check for non-null objects.
* [ ] Reactive tests verify actual updates.

## Lifecycle

* [ ] Created resources are disposed.
* [ ] Disposed components stop reacting.
* [ ] Disposed Effects stop observing.
* [ ] Removed dynamic children are disposed.
* [ ] Reactive layout bindings stop after disposal.

## Edge Cases

* [ ] Boundary values are tested.
* [ ] Invalid values have defined behavior.
* [ ] Repeated configuration is tested where relevant.
* [ ] Reactive invalid values follow the same rules.

## Quality

* [ ] Tests use descriptive names.
* [ ] Tests are isolated.
* [ ] Tests use real Arc objects where practical.
* [ ] Tests avoid implementation details where possible.
* [ ] Tests are deterministic.

## Final Validation

* [ ] Run affected tests.
* [ ] Run related test suites.
* [ ] Check for leaked Effects/listeners.
* [ ] Check for flaky behavior.

---

# Final Principle

The goal is not to prove:

> "The component still exists after calling this method."

The goal is to prove:

> "Calling this API produces the intended observable behavior, reactive changes work correctly, and all reactions stop when the component is disposed."

For Solim, the preferred lifecycle is:

```text
Create
↓
Build
↓
Verify actual state
↓
Update
↓
Verify actual change
↓
Dispose
↓
Update source again
↓
Verify no further reaction
```

If a test would still pass after removing the feature it claims to test, the test is too weak and should be improved.
