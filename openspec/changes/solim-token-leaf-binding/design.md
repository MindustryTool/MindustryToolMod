## Context

Arc UI (`arc.scene.Element`) provides a single untyped `public Object userObject` field. Solim historically used this field to associate raw Arc elements with Solim metadata:
- `PendingCellConfig.find(element)` inspected `element.userObject` to discover cell sizing and padding constraints.
- `Ui.isExpanding(child)` checked if `child.userObject` equaled `"expanding"` or `Boolean.TRUE`.
- `GapContainer.respace(table)` and layout attachers checked if `table.userObject instanceof GapContainer`.
- `Spacer`, `Text`, and `SplitBar` assigned bare string `"expanding"` to `userObject`.

This legacy pattern created silent failures (missing `userObject = this` in constructors), metadata clobbering (bare strings overwriting component references), mount-time overhead (dropping the known component in `ParentStack` and searching for it later), and technical debt across the codebase.

## Goals / Non-Goals

**Goals:**
- Completely remove all legacy `userObject` patterns, bare string markers, and ad-hoc casts across Solim.
- Eliminate the possibility of forgetting element-to-component binding by creating an abstract `LeafComponent<E, SELF>` base class.
- Eliminate the mount-time "scavenger hunt" by updating `ParentStack` to pass `(Cell<?>, Element, Component)` directly to `CellConfigurator`.
- Standardize all element metadata in a structured `SolimToken` envelope on `Element.userObject` that cleanly preserves external user data in `userPayload`.
- Remove hundreds of lines of duplicate delegation code across leaf widgets (`SolimImage`, `NetworkImage`, `Badge`, `Text`, etc.).
- Update all tests and MCP inspection tools to use `SolimToken`.

**Non-Goals:**
- Replace composite component architecture (`BaseComponent`); `BaseComponent` continues managing declarative child composition.
- Remove Arc's `Element.userObject` field at the engine level; wrapping it in `SolimToken` retains O(1) field lookup without hash map overhead in the 60fps game loop.

## Decisions

### 1. Structured `SolimToken` Envelope
Instead of storing raw strings (`"expanding"`) or raw `Component` references directly on `element.userObject`, wrap them strictly in `SolimToken`:
```java
public final class SolimToken {
    public @Nullable Component component;
    public @Nullable PendingCellConfig cellConfig;
    public boolean expanding;
    public @Nullable Object userPayload;

    public static SolimToken getOrCreate(Element el);
    public static @Nullable SolimToken get(Element el);
    public static @Nullable Component getComponent(Element el);
    public static void bind(Element el, Component comp);
    public static void bind(Element el, Component comp, PendingCellConfig config);
    public static void setExpanding(Element el, boolean expanding);
    public static boolean isExpanding(@Nullable Element el);
}
```
*Rationale:* `SolimToken` cleanly isolates Solim's layout and component metadata into typed fields. External mod or game payloads are stored in `userPayload` without collisions.

### 2. Direct Component Passing in `ParentStack`
`ParentStack` already stores `List<Component>` in `pendingComponents`. When attaching pending components, `ParentStack` holds both `Component` and `Element`:
```java
@FunctionalInterface
public interface CellConfigurator {
    void configure(Cell<?> cell, Element child, @Nullable Component component);
}
```
*Rationale:* Passing the `Component` directly to `CellConfigurator` eliminates reverse traversal during attachment. `PendingCellConfig.find(comp != null ? comp : child)` immediately configures the cell.

### 3. `LeafComponent<E extends Element, SELF extends LeafComponent<E, SELF>>` Base Class
Introduce `LeafComponent` in `solim.core` implementing `Component`, `CellConfig<SELF>`, and `ElementConfig<SELF>`.
```java
public abstract class LeafComponent<E extends Element, SELF extends LeafComponent<E, SELF>>
        implements Component, CellConfig<SELF>, ElementConfig<SELF> {
    protected final E element;
    protected final PendingCellConfig constraints = new PendingCellConfig();

    protected LeafComponent(E element) {
        this.element = element;
        SolimToken.bind(element, this, constraints);
        ComponentContext.register(this);
        Table parent = ParentStack.current();
        if (parent != null) {
            ParentStack.registerPendingComponent(this, parent);
        }
    }
}
```
*Rationale:* Subclasses only pass their Arc element to `super(element)`. Token binding, lifecycle registration, pending attachment, and all `CellConfig`/`ElementConfig` delegator methods are inherited automatically.

### 4. Complete Elimination of Legacy Code and Fallbacks
- **No legacy string tags**: Replace all `"expanding"` assignments (`Spacer`, `Text`, `SplitBar`) with `SolimToken.setExpanding(element, true)`. In `Ui.isExpanding(child)` and `LayoutInspector`, check `SolimToken.isExpanding(child)` and delete the raw string checks.
- **No legacy ad-hoc casts**: Replace all `table.userObject instanceof GapContainer` checks in `Column`, `Row`, `Card`, `Grid`, `SolimCollapser`, `PendingCellConfig`, and `ElementConfig` with `GapContainer.respace(table)`. `GapContainer.respace` resolves the container via `SolimToken.getComponent(table)`.
- **No legacy recursive fallback**: In `PendingCellConfig.find`, resolve via `target` or `SolimToken`. Delete the untyped `return find(el.userObject);` recursion.
- **No legacy test assertions**: Update tests in `solim-core`, `solim-mcp`, and `mod` to assert on `SolimToken.getComponent(el)` or `SolimToken.isExpanding(el)`.

## Risks / Trade-offs

- **[Risk] Existing external code or tests casting `element.userObject` directly to `Component`**
  - *Mitigation:* Cleanly migrate all tests in the repository to use `SolimToken.getComponent(element)`. If external callers access `userObject`, they should use `SolimToken.getComponent(el)`.
- **[Risk] High volume of delegation methods in `LeafComponent`**
  - *Mitigation:* Centralizing them in `LeafComponent` removes identical boilerplate from `SolimImage`, `NetworkImage`, `Badge`, `Button`, `Text`, etc.
