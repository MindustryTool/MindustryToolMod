## MODIFIED Requirements

### Requirement: Keybind and upgrade behavior unchanged
The system SHALL keep the `smartUpgradeTrigger` (`U`) hover path working on enabled alone while ignoring armed state, SHALL keep upgrade-candidate computation, chain upgrade, max-updates / same-type / traverse-bridges settings, menu layout, tile highlight, and toasts behaving as before, and SHALL additionally provide a Delete option in the popup menu to bulk-deconstruct the connected chain.

#### Scenario: U keybind still opens menu without arming
- **WHEN** the player presses `U` while hovering a valid upgradeable building and the feature is enabled but disarmed
- **THEN** the upgrade menu opens for that tile

#### Scenario: Chain upgrade unaffected
- **WHEN** the player picks a target block from the menu
- **THEN** the connected chain upgrades exactly as before the trigger change

#### Scenario: Menu opens with Delete option even when no upgrade candidates exist
- **WHEN** the player triggers Smart Upgrade on a valid supported building that has zero unlocked upgrade candidates
- **THEN** the popup menu still opens displaying the Delete button instead of aborting with a no-candidates toast

#### Scenario: Selecting Delete queues break plans for the connected chain
- **WHEN** the player clicks the Delete button in the Smart Upgrade popup menu
- **THEN** the system traverses the connected chain up to `max-updates`, queues breaking `BuildPlan`s at each eligible building's root tile on `Vars.player.unit()`, closes the menu, and displays a toast with the deleted block count

#### Scenario: Delete respects Only Replace Exact Block Type
- **WHEN** `only-same-type` is enabled and the player clicks Delete in the popup menu
- **THEN** only connected buildings whose `Block` equals the starting building's `Block` are queued for deconstruction, excluding traversed connectors and different block types or sizes

#### Scenario: Delete includes traversed connectors and any size when Only Replace Exact Block Type is disabled
- **WHEN** `only-same-type` is disabled and the player clicks Delete in the popup menu
- **THEN** connected buildings in the same `BlockGroup` of any footprint size are queued for deconstruction, and if `traverse-bridges` is enabled for a `CONVEYOR` or `CONDUIT` chain, traversed connector buildings (bridges, junctions, routers, sorters, overflow gates/ducts) are also queued for deconstruction

### Requirement: Internationalization
The system SHALL resolve all user-visible Smart Upgrade trigger and action strings (help text, trigger-mode label, armed toast, delete button tooltip, deleted toast, settings title, reset, toasts) from `bundle.properties`, SHALL NOT hardcode display text in Java, SHALL document each new key with a translator comment including placeholder meanings, and SHALL ship matching VI entries.

#### Scenario: Help describes armed single-click
- **WHEN** the player views Smart Upgrade help
- **THEN** the text describes arming via QuickAccess plus single-tap and the `U` keybind

#### Scenario: New keys carry translator comments
- **WHEN** a new user-visible trigger or delete string is introduced
- **THEN** a corresponding key with a descriptive translator comment is added to `bundle.properties` with a matching VI value
