## ADDED Requirements

### Requirement: Map Rules Feature Registration
The mod SHALL register a `MapRulesFeature` under `mindustrytool.features.rules` that extends `Feature`, is accessible from the QuickAccess HUD bar, and persists its configuration.

#### Scenario: Feature registered and present in QuickAccess
- **WHEN** the mod initializes on client load
- **THEN** `MapRulesFeature` is registered in `FeatureManager` with `quickAccess(true)` and appears on the QuickAccess HUD bar when enabled.

#### Scenario: QuickAccess click opens rules dialog
- **WHEN** the player clicks the Map Rules icon in the QuickAccess HUD bar during gameplay
- **THEN** `MapRulesDialog` opens displaying the active map's rules.

---

### Requirement: Categorized Rules Inspection
The `MapRulesDialog` SHALL display active game rules organized by logical categories with a responsive search filter.

#### Scenario: Viewing rule categories
- **WHEN** the player opens `MapRulesDialog`
- **THEN** rules are grouped under distinct categories: Multipliers, Waves & Spawns, Banned Content, Environment & Visuals, and Mechanics.

#### Scenario: Searching for specific rules or content
- **WHEN** the player enters text into the search bar
- **THEN** the view filters displayed rules and banned content to entries matching the query in name, description, or content identifier.

---

### Requirement: "Modified Only" Diff Filter
The system SHALL compare current active game rules against default baseline rules (`new Rules()`) and allow filtering down to only modified rules.

#### Scenario: Filtering to modified rules
- **WHEN** the player selects the `[★ Modified Only]` filter chip
- **THEN** only rules whose values differ from default baseline rules are displayed.

#### Scenario: No rules modified
- **WHEN** the player selects `[★ Modified Only]` on a map with default rules
- **THEN** an informative empty state indicates all rules match defaults.

---

### Requirement: Role-Based Editing Authority
The system SHALL govern rule editability according to the player's connection context (Singleplayer, Host, Multiplayer Client Non-Admin, Multiplayer Client Admin).

#### Scenario: Editing in Single Player
- **WHEN** the player modifies a rule while playing singleplayer
- **THEN** `Vars.state.rules` is updated directly and immediately takes effect in the local world.

#### Scenario: Editing when Hosting
- **WHEN** the host modifies a rule while hosting a game
- **THEN** `Vars.state.rules` is updated and `Call.setRules(Vars.state.rules)` is broadcast to synchronize all connected clients.

#### Scenario: Inspecting as Multiplayer Client (Non-Admin)
- **WHEN** a non-admin client opens `MapRulesDialog` on a multiplayer server
- **THEN** gameplay rules are presented in read-only mode with a status badge indicating client read-only mode, while visual rules (fog, lighting) remain toggleable locally.

#### Scenario: Remote Editing as Multiplayer Client (Admin)
- **WHEN** an authenticated admin client modifies a rule on a multiplayer server
- **THEN** the change is executed remotely via `/js` chat command updating server state and broadcasting `Call.setRules`.

---

### Requirement: Reset to Baseline Rules
The system SHALL provide an action to restore active game rules back to the original rules of the loaded map.

#### Scenario: Resetting modified rules
- **WHEN** the player clicks "Reset to Defaults" and confirms
- **THEN** active rules revert to the original map rules snapshot and changes are applied/synchronized.
