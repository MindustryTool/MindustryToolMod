# Solim Internal Architecture & Technical Reference Manual

Solim is a declarative, fine-grained reactive UI framework designed specifically for **Mindustry game mods** on top of Arc Scene2D.

Unlike web frameworks or backend component trees, Solim operates inside a **single-threaded, retained-mode game engine environment (Java 8 runtime compatibility)**. It eliminates manual widget updates and complex event listener plumbing by providing:
1. **Zero-Virtual-DOM retained updates**: Signals update existing Arc Scene2D widgets directly in-place.
2. **Ambient lifecycle ownership**: Bindings, effects, child components, and listeners created during component construction are automatically tracked and disposed in reverse order (LIFO).
3. **Single-frame batched invalidations**: Schedulable effects are enqueued and deduplicated to run at most once per Mindustry frame (`Trigger.update`), preventing cascading re-render loops and visual jitter.
4. **Strict modular isolation**: Engine internals are quarantined in `:solim-runtime`, completely separate from `:solim-core` and the public facade `:solim`.

---

## 1. Architectural Blueprint & Module Separation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                :mod (Consumer)                              │
│         Uses declarative solim.UI.* facades and Mindustry game logic.       │
│         NEVER references :solim-runtime classes (physically excluded).       │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ depends on
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           :solim (Public Facade)                            │
│                     UI.java (Static factory methods)                        │
└──────────────────┬───────────────────────────────────────┬──────────────────┘
                   │ depends on                            │ depends on
                   ▼                                       ▼
┌──────────────────────────────────────┐ ┌────────────────────────────────────┐
│      :solim-core (Public APIs)       │ │  :solim-runtime (Internal Engine)  │
│  BaseComponent, Signal, Computed,    │ │  AttachmentStack, OwnershipContext,    │
│  Effect, Binding, Column, Row,       │ │  ReactiveContext, SignalDispatcher,│
│  ReactiveGrid, Button, Text, etc.    │ │  StructuralReconciler              │
└──────────────────┬───────────────────┘ └─────────────────┬──────────────────┘
                   │ depends on                            │ depends on
                   └───────────────────┬───────────────────┘
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       :solim-api (Minimal Contracts)                        │
│   Component, Disposable, ReactiveObserver, SchedulableEffect, SpacingAware   │
└─────────────────────────────────────────────────────────────────────────────┘
```

| Module | Access Level | Description |
|---|---|---|
| `:solim-api` | Public | Foundational interfaces with zero dependencies (`Component`, `Disposable`, `ReactiveObserver`, `SchedulableEffect`, `SpacingAware`). |
| `:solim-runtime` | Internal engine | Stack-based contexts, reconciliation algorithms, and frame-rate event loop dispatchers. **Hidden from mods**. |
| `:solim-core` | Public framework | Reactive primitives, layout containers, Arc Scene2D widget wrappers, modifier mixins, and configuration bridges. |
| `:solim` | Public facade | Single-entry static facade (`solim.UI`) providing mod developer ergonomics. |
| `:solim-mcp` | Debug tool | Non-invasive debug server using pure reflection to introspect running Solim UI hierarchies and signal graphs via WebSocket / JSON-RPC 2.0. |

---

## 2. Core Public Contracts (`:solim-api`)

### 2.1 `Disposable`
```java
public interface Disposable {
    void dispose();
    default boolean isDisposed() { return false; }
}
```
- **Role**: Universal cleanup handle. Every effect, listener, subscription, and component implements `Disposable`.
- **Contract**: `dispose()` must be idempotent. Calling it multiple times must not throw or cause secondary side effects.

### 2.2 `Component`
```java
public interface Component extends Disposable {
    Element element();
    @Override default void dispose() {}
}
```
- **Role**: Unit of UI composition owning an underlying Arc `Element`.
- **Contract**: Calling `element()` returns the root Arc Scene2D widget. Calling `element()` on a disposed component throws an `IllegalStateException`.

### 2.3 `ReactiveObserver`
```java
public interface ReactiveObserver {
    void addDependency(Object observable);
    void invalidate();
}
```
- **Role**: Represents a node in the reactive dependency graph (`Computed`, `Effect`).
- **Contract**:
  - `addDependency(Object)`: Called during evaluation when a `Signal` or `Computed` is accessed via `.get()`.
  - `invalidate()`: Called by an upstream dependency when its value changes, notifying this observer that its state is stale.

### 2.4 `SchedulableEffect`
```java
public interface SchedulableEffect {
    boolean isDisposed();
    void clearPending();
    void runPending();
}
```
- **Role**: Bridge between reactive invalidations and the single-frame `SignalDispatcher`.
- **Contract**: Allows `SignalDispatcher` to batch, deduplicate, and execute deferred effects on the main Mindustry thread.

### 2.5 `SpacingAware`
```java
public interface SpacingAware {
    void applySpacing();
}
```
- **Role**: Implemented by widgets (`Text`, `SolimImage`) whose cell padding/margin depends on post-construction layout hierarchy resolution.

---

## 3. The Internal Engine (`:solim-runtime`)

The engine relies on thread-local-free ambient stacks because Mindustry runs its entire UI on a single main thread.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Ambient Runtime Stacks                             │
├────────────────────────────────┬────────────────────────────────────────────┤
│         AttachmentStack            │ Manages active layout container for        │
│    Deque<AttachmentStack.Entry>    │ declarative child attachment.              │
├────────────────────────────────┼────────────────────────────────────────────┤
│       OwnershipContext         │ Tracks the active BaseComponent.build()    │
│  Deque<Consumer<Disposable>>   │ scope to automatically own child resources.│
├────────────────────────────────┼────────────────────────────────────────────┤
│       ReactiveContext          │ Tracks active ReactiveObserver (Computed   │
│   Deque<ReactiveObserver>      │ or Effect) collecting .get() dependencies. │
└────────────────────────────────┴────────────────────────────────────────────┘
```

---

### 3.1 `AttachmentStack`
Manages implicit parent-child hierarchy construction for declarative blocks like `column(() -> { ... })`.

#### Fields
| Field | Type | Role & Purpose | Why It Exists |
|---|---|---|---|
| `stack` | `Deque<Entry>` | Stack of active parent `Table`s and their `Attacher` strategies. | Enables nested declarative syntax (`row(() -> { column(() -> { ... }); });`) without explicitly passing parent references. |
| `pendingComponents` | `Map<Table, List<Component>>` | Stash of components instantiated during parent's `children()` block. | Ensures child components are **fully constructed** before their `.element()` is resolved and attached to Arc Scene2D. |
| `tableAttachers` | `Map<Table, Attacher>` | Custom cell attachment rules mapped by `Table`. | Allows `Column`, `Row`, `Grid`, and `Card` to dictate how children are added (e.g., calling `.row()` after vertical items). |
| `cellConfigurator` | `BiConsumer<Cell<?>, Element>` | Global hook executed whenever an element is attached to a cell. | Automatically applies `PendingCellConfig` constraints and triggers `GapContainer.respace()`. |

#### Inner Classes
- `AttachmentStack.Attacher`: Functional interface `Cell<?> attach(Table parent, Element child)` defining container-specific placement logic.
- `AttachmentStack.Entry`: Stores the `Table` and its `Attacher` pair.

#### Key Mechanics & Lifecycle
1. **`push(Table parent, Attacher attacher)`**: Pushes container onto `stack`.
2. **`attachToParent(Element child)`**: If `stack` is not empty, peeks the current table, attaches pending components first, then attaches `child` using the active `attacher`.
3. **`isolate(Supplier<T> supplier)`**: Saves the current stack, clears it during `supplier.get()`, and restores it in a `finally` block. **Prevents accidental parent-child hijacking** when creating standalone dialogs or detached components.
4. **`registerPendingComponent(Component component, Table parent)`**: Registers a child component to be attached only when `parent` is popped from the stack.

---

### 3.2 `OwnershipContext`
Enables automatic, ambient resource ownership without manual `.own()` boilerplate.

#### Fields
| Field | Type | Role & Purpose | Why It Exists |
|---|---|---|---|
| `stack` | `Deque<Consumer<Disposable>>` | Stack of resource registrars belonging to currently building `BaseComponent` instances. | Enables nested components to capture their own effects, bindings, and sub-components automatically. |
| `paused` | `int` | Re-entrancy counter for temporarily disabling auto-ownership. | Used by `StructuralReconciler` so recycled or cached components are not accidentally re-registered into a temporary scope. |

#### Key Mechanics
```java
// Inside BaseComponent.element():
OwnershipContext.push(this::own);
try {
    cached = build();
} finally {
    OwnershipContext.pop();
}
```
Whenever an `Effect`, `Binding`, or child `Component` is created:
```java
OwnershipContext.register(disposable);
```
If a parent is currently building, its registrar captures the disposable. When that parent is later disposed, all captured disposables are freed automatically.

---

### 3.3 `ReactiveContext`
Stack-based dynamic dependency tracking engine.

#### Fields
| Field | Type | Role & Purpose | Why It Exists |
|---|---|---|---|
| `stack` | `Deque<ReactiveObserver>` | Stack of observers currently evaluating (`Computed` or `Effect`). | Tracks fine-grained dependencies dynamically as `.get()` is invoked during computation. |

#### Key Mechanics
1. **`track(Object observable)`**: When `signal.get()` or `computed.get()` is executed, it calls `ReactiveContext.track(this)`. If an observer is on the stack, it records the dependency.
2. **`untracked(Supplier<T>)`**: Suspends dependency collection while running code that should not trigger reactive re-evaluations (used inside reconcilers and event dispatchers).

---

### 3.4 `SignalDispatcher`
Batches reactive effect execution to the Mindustry frame lifecycle.

```
Signal Mutation (signal.set(x))
       │
       ▼
Invalidate Observers (effect.invalidate())
       │
       ▼
Enqueue Effect in SignalDispatcher (SignalDispatcher.enqueue(this))
       │
       ▼ (Awaiting next Mindustry Trigger.update frame event)
SignalDispatcher.flush()
       │
       ├─► Loop batch in FIFO order
       ├─► Re-evaluate effect.runPending()
       └─► Cascade Guard (Aborts if passes > MAX_CASCADE_DEPTH)
```

#### Fields
| Field | Type | Role & Purpose | Why It Exists |
|---|---|---|---|
| `MAX_CASCADE_DEPTH` | `int = 100` | Safety threshold for recursive reactive evaluations. | Protects the game engine from crashing when an infinite signal loop occurs (`Effect A` sets `Signal B`, which triggers `Effect B`, setting `Signal A`). |
| `queue` | `Queue<SchedulableEffect>` | FIFO queue of invalidated effects waiting to execute. | Batches all updates so an element only recalculates once per frame even if multiple signals change simultaneously. |
| `registered` | `boolean` | Tracks whether `Events.run(Trigger.update, ...)` has been attached to Mindustry. | Guarantees idempotent registration. |
| `flushing` | `boolean` | Re-entrancy lock during queue flushing. | Prevents nested flushes while running pending effects. |

#### Key Mechanics
- In Mindustry, game logic updates occur on `Trigger.update`.
- When a signal changes, observers are marked dirty. Computeds invalidate their downstreams, while Effects enqueue themselves into `SignalDispatcher`.
- At the start of the frame, `SignalDispatcher.flush()` drains the queue, executing each effect's `runPending()`.
- If an effect triggers another effect, the loop processes cascades up to 100 iterations. If 100 is exceeded, it clears the queue and logs an error to prevent freezing the game.

---

### 3.5 `StructuralReconciler<K, C extends Component>`
Keyed identity reconciliation engine for dynamic lists (`ForEach`, `ReactiveGrid`, `VirtualList`).

#### Fields
| Field | Type | Role & Purpose | Why It Exists |
|---|---|---|---|
| `activeComponents` | `Map<K, C>` | Map of currently active components keyed by item identity `K`. | Preserves component state and Arc widget instances across data array mutations. |
| `disposed` | `boolean` | Reconciler disposal status. | Prevents double disposal and memory leaks. |

#### Reconciliation Algorithm (`reconcile`)
1. Iterates through new items and extracts each item's key `K`.
2. **Key exists in `activeComponents`**: Reuses existing component instance directly. No rebuild.
3. **Key is new**: Executes `factory.apply(item)` in an isolated context (`AttachmentStack.isolate()` + `withoutAutoOwnership()`), eagerly forces `.element()`, and registers it in `nextComponents`.
4. **Key was removed**: Any key in `activeComponents` that is absent from `nextComponents` is immediately disposed via `comp.dispose()`.
5. Swaps `activeComponents` with `nextComponents`.

---

## 4. The Reactive Engine (`:solim-core/reactive`)

### 4.1 `Signal<T>`
A mutable reactive value and primary source of truth.

```
┌────────────────────────────────────────────────────────┐
│                      Signal<T>                         │
├────────────────────────────────────────────────────────┤
│ - value: T                                             │
│ - listeners: List<Consumer<T>> (Immediate callbacks)   │
│ - observers: Set<ReactiveObserver> (Computeds/Effects) │
└────────────────────────────────────────────────────────┘
```

#### Fields
| Field | Type | Role & Purpose | Why It Exists |
|---|---|---|---|
| `value` | `T` | The current data payload. | Stores the state value. |
| `listeners` | `List<Consumer<T>>` | Direct manual subscriptions registered via `.subscribe()`. | For external code needing eager callbacks on change. |
| `observers` | `Set<ReactiveObserver>` | Dependent `Computed`s and `Effect`s tracking this signal. | Invalidation propagation graph. |

#### Key Methods
- `get()`: Returns current value and registers dependency with `ReactiveContext.current()`.
  > [!WARNING]
  > Calling `signal.get()` during component `build()` extracts a static snapshot once and breaks reactivity! Pass `Signal` or `Readable` directly to component properties, or use `.map()`.
- `peek()`: Returns current value **without** registering a reactive dependency. Ideal inside `onClick` handlers.
- `set(T newValue)`: Compares `Objects.equals(value, newValue)`. If different, assigns new value, notifies `listeners`, and calls `observer.invalidate()` on all registered observers.
- `update(Function<T, T> updater)`: Convenience method: `set(updater.apply(value))`.

#### Example
```java
Signal<Integer> count = Signal.of(0);

// Incrementing
count.update(c -> c + 1);

// Peeking inside an action
button("Print", () -> Log.info("Current count: " + count.peek()));
```

---

### 4.2 `Computed<T>`
A lazily-evaluated, memoized reactive derivation with automatic dynamic dependency tracking.

#### Fields
| Field | Type | Role & Purpose | Why It Exists |
|---|---|---|---|
| `supplier` | `Supplier<T>` | Derivation logic function. | Computes the value from upstream signals. |
| `cachedValue` | `T` | Last computed value. | Memoizes result to avoid redundant calculations. |
| `hasValue` | `boolean` | True if computed at least once. | Differentiates between uncomputed state and a computed `null`. |
| `dirty` | `boolean` | True if dependencies changed and re-evaluation is needed. | Enables lazy recomputation only when read. |
| `computing` | `boolean` | Re-entrancy guard flag. | Detects circular computation loops (`A -> B -> A`) and throws `IllegalStateException`. |
| `dependencies` | `Set<Object>` | Upstream signals/computeds currently observed. | Stored to allow unsubscribing when dependencies change dynamically. |
| `collecting` | `Set<Object>` | Temporary dependency bucket during recomputation. | Populated via `ReactiveContext.track()`. |
| `observers` | `Set<ReactiveObserver>` | Downstream computeds/effects depending on this computed. | Propagates invalidation down the graph. |
| `listeners` | `List<Consumer<T>>` | Manual consumers subscribed to changes. | Triggers eager recompute when listeners exist. |

#### Dynamic Dependency Collection Example
```java
Signal<Boolean> showDetails = Signal.of(false);
Signal<String> details = Signal.of("Heavy Details");
Signal<String> summary = Signal.of("Brief Summary");

Computed<String> display = new Computed<>(() -> {
    if (showDetails.get()) {
        return details.get(); // 'details' is tracked ONLY when showDetails is true
    } else {
        return summary.get(); // 'summary' is tracked ONLY when showDetails is false
    }
});
```
When `showDetails` flips to `false`, `Computed` automatically unsubscribes from `details`, preventing wasteful invalidations.

---

### 4.3 `Effect`
Executes side effects in response to dependency changes, with automatic cleanup and frame batching.

#### Fields
| Field | Type | Role & Purpose | Why It Exists |
|---|---|---|---|
| `runnable` | `Runnable` | Simple effect action. | Executed on evaluation. |
| `supplierWithCleanup` | `Supplier<Runnable>` | Effect returning a cleanup action. | Allows effects to register teardown logic (e.g. unregistering timers). |
| `cleanupConsumer` | `Consumer<Cleanup>` | Consumer accepting a multi-cleanup registry. | Supports registering multiple teardown tasks. |
| `dependencies` | `Set<Object>` | Upstream reactive dependencies. | Allows auto-unsubscribing and resubscribing. |
| `cleanups` | `List<Runnable>` | Teardown actions from previous executions. | Executed before the next effect run and on disposal. |
| `running` | `boolean` | Execution guard flag. | Prevents nested re-entrancy. |
| `pending` | `boolean` | Scheduled execution flag. | Prevents enqueuing duplicate entries in `SignalDispatcher`. |
| `disposed` | `boolean` | Cleanup status. | Halts all future executions. |

#### Lifecycle
1. **Initial Run**: Executes immediately upon creation within `BaseComponent.build()`. Automatically owned by `OwnershipContext`.
2. **Invalidation**: When any dependency changes, `invalidate()` is called:
   - Sets `pending = true`.
   - Calls `SignalDispatcher.enqueue(this)`.
3. **Dispatch**: On next frame, `SignalDispatcher` runs `effect.runPending()`:
   - Runs all previous `cleanups`.
   - Tracks new dependencies via `ReactiveContext.push(this)`.
   - Re-runs the effect action.

#### Example with Cleanup
```java
Effect.of(() -> {
    Log.info("Selected player: " + selectedPlayerId.get());
    Timer.Task task = Timer.schedule(this::ping, 1f, 1f);
    
    // Cleanup runnable executed before next run or on component disposal
    return task::cancel;
});
```

---

### 4.4 `TwoWayBinding<T>`
Synchronizes an Arc Scene2D widget and a reactive `Signal<T>` bidirectionally without feedback loops.

#### Fields
| Field | Type | Role & Purpose | Why It Exists |
|---|---|---|---|
| `updating` | `boolean` | Synchronization lock flag. | **Crucial**: Prevents recursive loops (`Signal -> Widget -> Listener -> Signal -> ...`). |
| `effect` | `Effect` | Signal-to-widget reactive effect. | Updates the widget when signal changes. |
| `listenerDisposable` | `Disposable` | Handle to the widget change listener. | Unregisters the widget listener on disposal. |

#### Anti-Feedback Flow
```
User interacts with Widget
       │
       ▼
Widget Listener fires
       │
       ├─► Is `updating` true? Yes -> ABORT (programmatic update in progress)
       └─► Is `updating` false?
             │
             ├─► Check equality: widgetValue != signal.peek()
             └─► signal.set(widgetValue)
                   │
                   ▼
             Effect triggers (Signal -> Widget)
                   │
                   ├─► Checks equality: signalValue != widgetValue (Equal! Skips setter)
                   └─► If different: sets `updating = true`, updates widget, resets `updating = false`
```

---

## 5. Layout Containers & The Modifier System

### 5.1 Three-Tier Modifier Architecture
Modifiers in Solim are partitioned across three distinct mixin interfaces to maintain clear separation of layout roles:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             Modifier Architecture                           │
├───────────────────────┬───────────────────────┬─────────────────────────────┤
│      CellConfig       │      TableConfig      │        ElementConfig        │
│   (Parent Cell Box)   │    (Inner Container)  │       (Target Widget)       │
├───────────────────────┼───────────────────────┼─────────────────────────────┤
│  - minWidth/minHeight │  - align/center/top   │  - width/height/size        │
│  - maxWidth/maxHeight │  - padding/paddingTop │  - x/y/position             │
│  - growX/growY/grow   │  - gap/respace        │  - visible/opacity/alpha    │
│  - margin/marginX/Y   │  - rounded/border     │  - onClick/draggable/tooltip│
│                       │  - background         │  - name                     │
└───────────────────────┴───────────────────────┴─────────────────────────────┘
```

---

### 5.2 `PendingCellConfig`
Buffers parent-cell settings before the element is attached to an Arc `Table`.

#### Fields
| Field | Type | Role & Purpose | Why It Exists |
|---|---|---|---|
| `prefWidth` / `prefHeight` | `Readable<Float>` | Preferred cell dimensions. | Applied via `cell.width()` / `cell.height()`. |
| `minWidth` / `minHeight` | `Readable<Float>` | Minimum cell constraints. | Applied via `cell.minWidth()` / `cell.minHeight()`. |
| `maxWidth` / `maxHeight` | `Readable<Float>` | Maximum cell constraints. | Applied via `cell.maxWidth()` / `cell.maxHeight()`. |
| `growX` / `growY` | `boolean` | Sizing expansion flags. | Configures `cell.growX()` and `cell.growY()`. |
| `padTop/Left/Bottom/Right` | `Readable<Float>` | Outer margins. | Applied as `cell.pad()` on parent table. |
| `align` | `Integer` | Cell content alignment. | Configures `cell.align()`. |

#### Automatic Hook via `AttachmentStack`
When `PendingCellConfig` class loads:
```java
AttachmentStack.setCellConfigurator((cell, child) -> {
    PendingCellConfig config = find(child);
    if (config != null) {
        List<Disposable> effects = config.applyToCell(cell);
        for (Disposable effect : effects) {
            OwnershipContext.register(effect);
        }
    }
    Table t = cell.getTable();
    if (t != null && t.userObject instanceof GapContainer) {
        ((GapContainer) t.userObject).respace();
    }
});
```
Every time a child is added, its cell configuration is applied immediately, and reactive bindings (e.g. reactive width/margins) are automatically registered into `OwnershipContext`.

---

### 5.3 `GapContainer` & Directional Sibling Spacing
Solim uses **sibling-aware gap spacing** instead of naive margins.

#### The Problem with Naive Margins
In traditional Scene2D tables, setting padding on elements causes outer edges to bulge or leaves unwanted gaps before the first or after the last item.

#### Solim Solution
- **First visible child receives 0 gap padding.**
- **Every subsequent visible child receives gap padding on its leading edge (`padLeft` for horizontal, `padTop` for vertical).**
- **Hidden children (`visible = false`) have all padding zeroed out automatically.**

```
Row with gap(8):
┌──────────────┐          ┌──────────────┐          ┌──────────────┐
│ Child 1 (vis)│◄──8px───►│ Child 2 (vis)│◄──8px───►│ Child 3 (vis)│
│  padLeft: 0  │          │ padLeft: 8   │          │ padLeft: 8   │
└──────────────┘          └──────────────┘          └──────────────┘
```

#### Algorithms
1. `spaceAttachedCell(table, cell, direction, gap)`: O(1) incremental check when adding an element. Checks if any previous cell is visible; if so, applies leading gap to the new cell.
2. `applySpacing(table, direction, gap)`: Full pass called when children change visibility or structure.
3. `applyGridSpacing(table, columns, gap)`: 2D grid spacing where columns > 0 receive `padLeft(gap)` and rows > 0 receive `padTop(gap)`.

---

### 5.4 Primary Layout Components

#### `Column`
Vertical layout container wrapping an Arc `Table`.
- **Attacher Strategy (`Column.ATTACHER`)**:
  - Adds child to table.
  - If `Ui.isExpanding(child)` is true, sets `cell.growY()`.
  - Calls `cell.row()` to advance to the next line.
  - Invokes `GapContainer.spaceAttachedCell(table, cell, Direction.VERTICAL, gap)`.

#### `Row`
Horizontal layout container wrapping an Arc `Table`.
- **Attacher Strategy (`Row.ATTACHER`)**:
  - Adds child to table.
  - If `Ui.isExpanding(child)` is true, sets `cell.growX()`.
  - Does **not** call `cell.row()`.
  - Invokes `GapContainer.spaceAttachedCell(table, cell, Direction.HORIZONTAL, gap)`.

#### `Card`
Vertical container based on `Column.ATTACHER`, styled as a distinct surface card. Includes defaults for background, borders, and rounded corners.

#### `Grid`
Static grid with fixed or reactive column count:
- Uses `table.row()` whenever `cellCount % columns == 0`.
- Automatically reflows all children when `columns` signal changes.

#### `ReactiveGrid<T, K>`
Keyed, responsive 2D grid that dynamically recalculates item widths:
- **`tableWidth`**: Signal updated in `layout()` and `sizeChanged()`.
- **`itemWidth`**: `Computed<Float>` calculating:
  $$\text{itemWidth} = \frac{\text{availableWidth} - (\text{cols} - 1) \times \text{gap}}{\text{cols}}$$
- **`context`**: Exposes `GridItemContext` (`itemWidth()`, `columnCount()`) to child factories.
- Uses `StructuralReconciler` to preserve item identity and insert trailing spacer cells to keep rows aligned.

#### `VirtualList<T, K>`
High-performance virtualized vertical list:
- Only renders items intersecting the visible viewport plus an `overscan` buffer (default 3 items).
- Uses binary search (`findFirstVisible`, `findLastVisible`) over accumulated `yOffsets` to determine visible index range in $O(\log N)$ time.
- Preserves mounted item components via `StructuralReconciler`.

---

## 6. Base Component Model (`BaseComponent`)

Every custom Solim component should extend `BaseComponent`.

```
┌────────────────────────────────────────────────────────┐
│                     BaseComponent                      │
├────────────────────────────────────────────────────────┤
│ - cached: Element (Lazily built Arc Element)           │
│ - componentName: String (Element name / debug ID)      │
│ - disposables: List<Disposable> (Owned resources)      │
│ - disposed: boolean (Disposal flag)                    │
└────────────────────────────────────────────────────────┘
```

### Fields Deep-Dive
| Field | Type | Role & Purpose | Why It Exists |
|---|---|---|---|
| `cached` | `Element` | The built Arc Scene2D element. | Caches the element so `build()` is executed exactly once on first access to `.element()`. |
| `componentName` | `String` | Debug identifier. | Assigned to `element.name` for MCP inspection and Arc Scene2D querying. |
| `disposables` | `List<Disposable>` | List of all owned resources (bindings, effects, listeners, child components). | Automatically cleaned up in **LIFO** (reverse registration) order on disposal. |
| `disposed` | `boolean` | Lifecycle state flag. | Guarantees disposal idempotency and throws fast if reused after disposal. |

### Lifecycle Rules
1. **Lazy Execution**: `build()` is **not** called in the constructor. It is triggered only when `element()` is called.
2. **Ambient Scope**: Inside `element()`, `OwnershipContext.push(this::own)` activates. Any `Effect`, `Binding`, or child component instantiated during `build()` is automatically added to `disposables`.
3. **LIFO Disposal**: Disposes resources in reverse order so downstream resources are torn down before upstream dependencies.
4. **Auto-Naming**: If `name(...)` is not explicitly set, Solim generates a default name: `"solim-" + componentName + "-" + elementName`.

---

## 7. Interactive & Display Components

### 7.1 `Button`
Pure Scene2D button wrapper with comprehensive declarative controls:
- **Click & Long Click**:
  - `onClick(Runnable)`: Installs `ClickListener`.
  - `onLongClick(long durationMs, Runnable)`: Tracks press duration via `Time.millis()` and fires on threshold.
  - `stopClickPropagation(boolean)`: Controls whether event propagation stops at this button.
- **States**: Reactive `.enabled()`, `.checked()`, `.visible()`, `.color()`, `.style()`.
- **Styling**: Native integration with `RoundedDrawable` and `SolimButtonStyleBuilder`.

### 7.2 `Text`
High-performance label wrapper:
- Accepts `String`, `Signal<String>`, `Computed<String>`, or `Readable<String>`.
- Reactive font scaling (`fontScale()`), color (`color()`), wrapping (`wrap()`), and ellipsis (`ellipsis()`).
- Implements `SpacingAware` to synchronize padding/margins with parent table cells.

### 7.3 `SolimImage` & `NetworkImage`
- `SolimImage`: Wraps Arc `Image` with reactive `Drawable`, scaling mode (`Scaling.fit`, `Scaling.stretch`), and color tinting.
- `NetworkImage`: Asynchronously fetches remote images via `mindustrytool.services.Request`, decodes them on the Arc thread, caches textures, and mounts them reactively.

### 7.4 Overlays (`SolimDialog` & `Hud`)
- **`SolimDialog`**:
  - Wraps Mindustry `BaseDialog`.
  - Deferred content compilation: `contentBuilder` is executed inside a `BaseComponent` when `.dialog()` or `.show()` is first invoked.
  - Built-in `addCloseButton()`, `closeOnBack()`, and responsive `maxWidth(500f)`.
- **`Hud`**:
  - Floating non-modal overlay added directly to `Core.scene`.
  - Root element uses `Touchable.childrenOnly` so clicks outside the HUD pass through to the game.
  - Built-in drag handling: `draggable(handleElement, xSignal, ySignal)`.
  - Screen clamp: `keepInScreen()` automatically prevents the HUD from spilling beyond the display viewport on window resize events.

---

## 8. Complete End-to-End Examples

### 8.1 Custom Reactive Component Extending `BaseComponent`
```java
public class CounterWidget extends BaseComponent {
    private final Signal<Integer> count = Signal.of(0);

    @Override
    protected Element build() {
        Computed<String> labelText = count.map(c -> "Current Count: " + c);
        Computed<Color> labelColor = count.map(c -> c > 5 ? Color.scarlet : Color.white);

        return row().gap(unit(2)).children(() -> {
            text(labelText)
                .color(labelColor);

            button("+", () -> count.update(c -> c + 1))
                .size(unit(10));

            button("Reset", () -> count.set(0))
                .enabled(count.map(c -> c > 0));
        }).element();
    }
}
```

### 8.2 Responsive Keyed Grid with `ReactiveGrid`
```java
public class ItemPickerView extends BaseComponent {
    private final Signal<Seq<Item>> items = Signal.of(new Seq<>());
    private final Signal<Integer> columns = Signal.of(4);

    @Override
    protected Element build() {
        return column().grow().children(() -> {
            // Header
            row().growX().children(() -> {
                text("Select Item").growX();
                button("Refresh", this::loadItems);
            });

            // Reactive Grid
            reactiveGrid(
                columns,
                items,
                item -> item.id, // Stable identity key
                (item, ctx) -> new ItemCard(item, ctx.itemWidth())
            ).gap(unit(2)).grow();
        }).element();
    }

    private void loadItems() {
        items.set(Vars.content.items());
    }
}
```

### 8.3 Draggable Floating HUD
```java
public class MiniMapHud extends Hud {
    private final Signal<Float> posX = Signal.of(100f);
    private final Signal<Float> posY = Signal.of(100f);

    public MiniMapHud() {
        x(posX);
        y(posY);
        toFrontOnTouch();

        children(() -> {
            card().padding(unit(2)).gap(unit(1)).children(() -> {
                // Header serves as the drag handle
                row().growX().children(() -> {
                    text("MiniMap").growX();
                    icon(Icon.move).draggable(this, posX, posY);
                });

                // Content
                image(Core.atlas.find("minimap-placeholder"))
                    .size(unit(40));
            });
        });
    }
}
```

---

## 9. Best Practices & Invariant Checklist

### ❌ Anti-Patterns to Avoid
1. **Never call `signal.get()` inside `build()`**:
   - Calling `.get()` in `build()` extracts a static snapshot once and severs all reactivity.
   - Pass the `Signal`/`Readable` directly to the component or use `.map()`.
   - Use `signal.peek()` if an untracked read is genuinely required (e.g. inside an `onClick` callback).
2. **Never manually instantiate or update Scene2D widgets imperatively**:
   - Do not call `new Table()` or `new Label()` when Solim abstractions exist (`row()`, `column()`, `text()`).
3. **Never call `BaseComponent.own()` from mod code**:
   - Ambient ownership handles this automatically via `OwnershipContext`.
4. **Never use static `grid()` for dynamic items**:
   - Always use `reactiveGrid(...)` with a stable key extractor.

### ✅ Code Review Invariants
- [ ] Every user-visible text string uses a `bundle.properties` translation key.
- [ ] No Java 9+ standard library APIs are used (must remain Java 8 runtime compatible).
- [ ] Subscribed effects and event listeners are properly registered with `OwnershipContext` or `BaseComponent.listen()`.
- [ ] All HTTP calls use `mindustrytool.services.Request`.
- [ ] Dialogs call `maxWidth(500f)` and center content.
