## MODIFIED Requirements

### Requirement: Default element names are locale-stable
When a component does not supply an explicit name, the framework SHALL assign a default element name derived from the component type and element type. The name derivation SHALL NOT depend on the JVM's default locale, so that a runtime with a locale whose case-mapping rules differ from the root locale produces the same element names as every other runtime.

#### Scenario: Names are identical across locales
- **WHEN** components are constructed under the root locale and under a locale with different case-mapping rules, such as a Turkish locale
- **THEN** the generated element names are identical in both runs

#### Scenario: Explicit names still win
- **WHEN** a component is given an explicit name
- **THEN** that name is used verbatim and no default derivation is applied

#### Scenario: Pre-existing element names are preserved
- **WHEN** an element already carries a name before the component adopts it
- **THEN** that existing name is preserved rather than overwritten

#### Scenario: Default names are still machine-discoverable
- **WHEN** no explicit name is supplied
- **THEN** the generated name carries a recognisable framework prefix and both the component type and element type, so that debugging and introspection tooling can locate the element by name