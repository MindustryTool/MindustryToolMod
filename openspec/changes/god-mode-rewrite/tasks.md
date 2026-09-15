## 1. Localization & Foundation

- [x] 1.1 Add god mode translation keys and comments to `assets/bundles/bundle.properties`
- [x] 1.2 Define `GodModeProvider` interface and provider selection logic

## 2. Providers Implementation

- [x] 2.1 Implement `InternalGodModeProvider` with direct Mindustry API calls
- [x] 2.2 Implement `JSGodModeProvider` with Rhino-compatible `/js` commands and fixed player lookup

## 3. Position Picker Component

- [x] 3.1 Implement `MapPositionPicker` for interactive map tile coordinate selection

## 4. Dialogs (Solim UI)

- [x] 4.1 Implement `GodModeTeamDialog` for player and team selection
- [x] 4.2 Implement `GodModeItemsDialog` with search, target team, count presets, and add action
- [x] 4.3 Implement `GodModeUnitsDialog` with search, count presets, team, position picker, spawn, and kill all actions
- [x] 4.4 Implement `GodModeEffectsDialog` with search, duration input, apply, and clear actions
- [x] 4.5 Implement `GodModeCoreDialog` with core block picker, team, position picker, terrain validation, and place action

## 5. HUD & Feature Integration

- [x] 5.1 Implement `GodModeHudView` with floating draggable Solim panel and fog toggle switch
- [x] 5.2 Update `GodModeFeature` with configuration, lifecycle management, provider switching, and HUD mounting
- [x] 5.3 Verify Java 8 compatibility, Solim architecture rules, and compile check
