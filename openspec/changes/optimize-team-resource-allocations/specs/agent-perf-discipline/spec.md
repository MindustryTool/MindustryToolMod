## MODIFIED Requirements

### Requirement: Frame loops are allocation-free

Code that executes on a per-frame trigger SHALL NOT allocate heap memory during steady-state execution. Specifically, it SHALL NOT: box primitives (`Float`/`Integer` via `signal().peek()`, `Boolean.TRUE.equals` on a boxed read, or any autoboxing), concatenate strings, create lambdas that capture mutable state, allocate `Seq`/`ObjectMap`/collections, allocate iterators over collections that are not already iterator-free (prefer `each(Boolf)` / `each(Cons)` with pre-allocated scratch fields), or perform dictionary or reflection lookups whose result does not change between frames.

Scratch values SHALL be reused across frames via pre-allocated fields, `arc.util.Tmp` (`Tmp.r1`, `Tmp.c1`), or static lookup tables.

#### Scenario: Config read in a frame loop does not box
- **WHEN** a `Trigger.draw` or `Trigger.update` handler reads a float or boolean configuration value
- **THEN** the value SHALL be read into a primitive field or a pre-boxed value, and SHALL NOT allocate a new wrapper object per frame

#### Scenario: Per-frame label does not concatenate
- **WHEN** a frame loop renders a formatted label such as a countdown
- **THEN** the label SHALL be computed only when its displayed value changes, or SHALL be written into a reused `GlyphLayout`, and SHALL NOT build a new `String` per frame

#### Scenario: Per-frame bundle lookup is hoisted
- **WHEN** a frame loop renders a static or rarely-changing localized string
- **THEN** the string SHALL be resolved once outside the frame loop or refreshed on state change, and SHALL NOT call `Core.bundle.get(...)` per frame

#### Scenario: Allocation is asserted by test
- **WHEN** a feature implements a `Trigger.draw` handler that draws every frame
- **THEN** a test SHALL assert zero bytes allocated on the render thread across a simulated render loop, using `com.sun.management.ThreadMXBean.getThreadAllocatedBytes`, in the manner of the existing `RenderLoopAllocationTest`

#### Scenario: Team resource HUD elements assert zero allocation
- **WHEN** `SplitBar.draw()` and `TeamResourceHudView` update handlers run during steady-state gameplay
- **THEN** a test using `com.sun.management.ThreadMXBean.getThreadAllocatedBytes` SHALL assert zero heap bytes allocated across simulated frames
