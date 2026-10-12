## MODIFIED Requirements

### Requirement: Vanilla Controls Menu Integration
The system SHALL register all feature keybinds under the `mindustrytool` category with translatable bundle keys for display in Mindustry's standard "Rebind Keys" dialog. Every registered keybind SHALL have a corresponding `keybind.<name>.name` entry in the base bundle, and the category SHALL have a `category.mindustrytool.name` entry, so that no keybind label resolves through the dialog's fallback text.

#### Scenario: Display keybind in Mindustry controls menu
- **WHEN** a player opens Mindustry's vanilla Keybinds dialog (Settings -> Controls -> Rebind Keys)
- **THEN** all feature keybinds SHALL appear grouped under the "MindustryTool" section with localized names

#### Scenario: Every keybind has a localized label
- **WHEN** the Keybinds dialog is opened and every registered feature keybind is listed
- **THEN** each keybind SHALL display the text of its `keybind.<name>.name` bundle entry rather than a fallback derived from the keybind identifier

#### Scenario: Category header resolves from a bundle entry
- **WHEN** the Keybinds dialog renders the section header for the feature keybinds
- **THEN** the header SHALL read "MindustryTool" as provided by `category.mindustrytool.name`

#### Scenario: Rebind takes effect immediately
- **WHEN** a player rebinds a key in the vanilla dialog and closes the dialog
- **THEN** the feature SHALL respond to the newly assigned key without requiring a restart