## ADDED Requirements

### Requirement: LeafComponent base class for primitive widgets
Solim SHALL provide an abstract base class `LeafComponent<E extends Element, SELF extends LeafComponent<E, SELF>>` in `solim.core` implementing `Component`, `CellConfig<SELF>`, and `ElementConfig<SELF>`. The constructor SHALL automatically bind the underlying Arc `Element` to a `SolimToken`, register the component with `ComponentContext`, and register pending component attachment with `ParentStack.current()`.

#### Scenario: Automatic token registration on construction
- **WHEN** a subclass of `LeafComponent` is instantiated with an Arc `Element`
- **THEN** `SolimToken.getComponent(element)` returns the component instance and `SolimToken.get(element).cellConfig` returns its `PendingCellConfig`

#### Scenario: Automatic parent stack registration
- **WHEN** a subclass of `LeafComponent` is instantiated while a parent table is on `ParentStack`
- **THEN** the component is registered as pending on the parent table without manual component author code

#### Scenario: Inherited cell and element modifiers
- **WHEN** `.width(100f)`, `.growX()`, or `.pad(8f)` is called on a `LeafComponent`
- **THEN** the modifiers are recorded in its `PendingCellConfig` and applied to the parent cell upon attachment
