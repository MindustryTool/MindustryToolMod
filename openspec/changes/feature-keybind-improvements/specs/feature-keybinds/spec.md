## ADDED Requirements

### Requirement: Keybind Direct Mutation and State Notification
The system SHALL provide programmatic mutation and reset methods on `FeatureKeybind` that update the underlying Arc `KeyBind`, persist changes to settings, and fire reactive change notifications for listening UI components.

#### Scenario: Programmatic rebind updates persisted settings
- **WHEN** a `FeatureKeybind` is rebound to a new `KeyCode`
- **THEN** the bound `KeyBind` value SHALL be updated, saved via `KeyBind#save()`, and its reactive key signal SHALL emit the new `KeyCode`

#### Scenario: Programmatic reset clears custom binding
- **WHEN** a `FeatureKeybind` reset method is invoked
- **THEN** `KeyBind#resetToDefault()` SHALL be executed, and the reactive key signal SHALL emit the default `KeyCode`
