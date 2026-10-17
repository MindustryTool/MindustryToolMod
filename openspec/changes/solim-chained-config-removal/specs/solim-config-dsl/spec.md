## ADDED Requirements

### Requirement: Configuration is declared only through props

Configuration of a Solim host SHALL be declared exclusively through `.props(...)`. A chained configuration method SHALL NOT exist for any value the host renders, lays out, or exposes as state; the only chained methods that remain SHALL be content (appends a child), behaviour (registers a callback or performs an imperative action), structure (`children`/`items`/`tab`/`show`/`hide`), and accessors (getters). Properties SHALL be applied after any fluent content declared before them, and when two properties in the same `props(...)` call write the same target the later one SHALL win.

#### Scenario: A configuration setter is absent

- **WHEN** the public API of a host is inspected for a configuration setter such as `Text.wrap(boolean)`, `Button.checked(Readable<Boolean>)`, `SolimVideo.autoplay(boolean)`, `SolimCollapser.collapsed(boolean)`, or `Grid.columns(int)`
- **THEN** the setter does not exist, and the equivalent property factory is declared instead

#### Scenario: Content and behaviour stay fluent

- **WHEN** a button is declared as `button().text("Save").icon(Icon.add).onClick(this::save)`
- **THEN** all three calls compile, because content and behaviour are not configuration

#### Scenario: Later property wins

- **WHEN** a single `props(...)` call contains two properties writing the same target
- **THEN** the later property is the effective value

#### Scenario: Props apply after fluent content

- **WHEN** fluent content is declared and configuration is declared in `props(...)`
- **THEN** the configuration applies to the host after the content exists, so the configured value is the effective one

## REMOVED Requirements

### Requirement: Chained and Props modifiers have a defined precedence

**Reason**: The requirement existed only while both configuration styles were live during migration. With "Configuration is declared only through props", there is no chained configuration modifier to have precedence over, so the ordering rule between a chained modifier and `props(...)` no longer describes any supported API.

**Migration**: Replace `.gap(4f)` with `.props(gap(4f))` and any chained configuration modifier with its property factory argument; the only remaining ordering rule is that later properties in one `props(...)` call win.
