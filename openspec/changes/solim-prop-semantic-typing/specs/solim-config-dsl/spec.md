## ADDED Requirements

### Requirement: Semantic capabilities restrict a property to the hosts that honour it

Beyond the target capability a property writes, a property MAY require a semantic capability describing that the host honours it. Semantic capabilities SHALL be trait marker interfaces in `solim.modifier` extending `ElementCapability`, implemented by every host that honours the corresponding property family. A property whose validity relative to a host is a host property SHALL require a trait, so applying it to a host that does not implement the trait is a compile error. Where a property is honoured by more than one widget family, the property SHALL require the trait those families share, so that "valid on A or B" is expressed as a shared trait rather than a union.

#### Scenario: A trait property is rejected by a host without the trait

- **WHEN** a property requiring a trait, such as `checked(...)` requiring `CheckableCapability`, is passed to `props(...)` on a host that does not implement the trait
- **THEN** the code does not compile

#### Scenario: A shared trait covers several widget families

- **WHEN** a property that is honoured by `Button`, `Checkbox`, and `Switch` is declared
- **THEN** it requires the trait all three implement, and compiles on any of them without a per-widget overload or a union type

#### Scenario: Traits compose with target capabilities

- **WHEN** a host both hosts children and accepts a trait property
- **THEN** it declares the container capability and the trait together, and one `props(...)` call carries both its layout and trait properties

### Requirement: A capability-proven property never silently no-ops

When a property reaches application, it SHALL produce its documented effect or throw a descriptive error. A property SHALL NOT silently do nothing because the host cannot honour it. A silent no-op SHALL occur only when the property's value is genuinely absent; a mismatch between the value and the host SHALL throw. A thrown error intended to be read by a user SHALL use a translation key rather than a hardcoded message.

#### Scenario: A mismatch throws

- **WHEN** a property whose validity depends on the runtime class of its value is applied to a host that does not accept that value class
- **THEN** application throws a descriptive error naming the host and the value class, rather than silently doing nothing

#### Scenario: An absent value is a no-op

- **WHEN** a property is given a `null` value where absence is meaningful
- **THEN** it performs no mutation and does not throw
