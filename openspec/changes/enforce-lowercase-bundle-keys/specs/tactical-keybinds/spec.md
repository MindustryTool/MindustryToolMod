## MODIFIED Requirements

### Requirement: Callout Tactical Menu Keybind
The Callout feature SHALL register a configurable keybind named `keybind.calloutmenu` defaulting to `KeyCode.unset`.
Activating the keybind SHALL open the tactical callout dialog.

#### Scenario: Trigger callout menu via keybind
- **WHEN** the user presses the key bound to `keybind.calloutmenu` while Callout feature is enabled
- **THEN** the callout tactical dialog is displayed

### Requirement: Grief Alert Jump Keybind
The Grief Alert feature SHALL register a configurable keybind named `keybind.griefalertjump` defaulting to `KeyCode.unset`.
Activating the keybind while an active alert exists SHALL jump the camera to the alerted coordinates and trigger visual pulse highlight.

#### Scenario: Trigger grief alert jump via keybind
- **WHEN** an alert is active and the user presses the key bound to `keybind.griefalertjump`
- **THEN** the camera centers on the alert tile, camera detaches from player, and the banner is dismissed

### Requirement: Block Stats Display Toggle Keybind
The Block Stats Display feature SHALL register a configurable toggle keybind named `keybind.blockstatstoggle` defaulting to `KeyCode.unset`.
Activating the keybind SHALL toggle the enabled state of the feature.

#### Scenario: Toggle block stats display via keybind
- **WHEN** the user presses the key bound to `keybind.blockstatstoggle`
- **THEN** the feature enabled state is inverted and overlays update accordingly