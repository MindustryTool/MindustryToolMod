## MODIFIED Requirements

### Requirement: Static style application

A style SHALL be applied as a capability-typed prop declared through `.props(...)`, not through an untyped `style(Object)` modifier. `style` SHALL be overloaded per host family so the argument's static type selects the overload and its required capability: for example `style(Label.LabelStyle)` requires the label capability, `style(SolimButtonStyle)` requires the button capability, and `style(TextField.TextFieldStyle)` requires the field capability. Applying the whole style to the underlying Arc element SHALL be immediate, and no CSS parsing SHALL be involved. `SolimButtonStyle` SHALL remain a `final` class that does not extend Arc's `ButtonStyle`, and no Arc style class SHALL be re-parented.

#### Scenario: Static style apply

- **WHEN** `button(...).props(style(Styles.PRIMARY))` is called where `Styles.PRIMARY` is a `SolimButtonStyle`
- **THEN** the button overload is selected, the underlying Arc element has the primary style applied, and `getStyle()` reflects it

#### Scenario: A style for the wrong host is rejected

- **WHEN** `style(labelStyle)` is passed to `props(...)` on a host that does not require the label capability
- **THEN** the code does not compile

#### Scenario: The untyped style prop is absent

- **WHEN** the public prop factories are inspected
- **THEN** no `style(Object)` or `style(Readable<?>)` untyped factory exists

### Requirement: Reactive style binding

A reactive style SHALL be declared as a capability-typed prop through `.props(...)`, accepting a `Readable<T>` of the host family's style type. It SHALL apply the current style immediately, subscribe to future changes, and on each change re-apply the entire new style to the Arc element without diffing. The binding SHALL be owned by the host and disposed with it.

#### Scenario: Reactive style toggle

- **WHEN** `button(...).props(style(darkMode.map(v -> v ? Styles.PRIMARY : Styles.GHOST)))` is declared and `darkMode` becomes true
- **THEN** the button style switches from `GHOST` to `PRIMARY` via a full re-apply

#### Scenario: Reactive style is disposable

- **WHEN** the host carrying the style prop is disposed and the signal changes
- **THEN** the element style remains at its previous value

#### Scenario: Reactive overload still selects by static type

- **WHEN** a reactive style is declared whose value type is `Readable<LabelStyle>`
- **THEN** it resolves to the label overload and requires the label capability
