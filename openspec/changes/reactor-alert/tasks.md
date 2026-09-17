## 1. Feature Scaffolding

- [ ] 1.1 Create `mod/src/mindustrytool/features/reactoralert/ReactorAlertFeature.java` extending `Feature`.
- [ ] 1.2 Configure `FeatureMetadata`: id `reactor-alert`, icon `FileIcon.of("triangle-alert.png")`, order `23`, `enabledByDefault(true)`, `quickAccess(true)`.
- [ ] 1.3 Define a `ConfigGroup` `floatValue("radius", 10f)` in the feature.
- [ ] 1.4 Register `new ReactorAlertFeature()` in `Main.java` feature registration.

## 2. Detection Logic

- [ ] 2.1 Subscribe to `BlockBuildBeginEvent` in the feature and ignore events where `breaking` is true.
- [ ] 2.2 Resolve the constructed block from `ConstructBuild.current`, guarding on `event.tile.build instanceof ConstructBuild`.
- [ ] 2.3 Implement the explosive-reactor predicate: `block.flags.contains(BlockFlag.reactor) || block.explosionRadius > 0f`.
- [ ] 2.4 Resolve the builder's `Player` via `event.unit.getPlayer()`; skip when it equals `Vars.player`.
- [ ] 2.5 Fall back to `event.team.coloredName()` when the builder has no resolvable player.
- [ ] 2.6 Compute center-to-center Euclidean distance in tiles to each `Vars.state.teams.cores(event.team)` core and take the nearest.
- [ ] 2.7 Emit the alert only when the nearest core is within the configured radius (default 10).
- [ ] 2.8 Add a global 3-second cooldown gating alert emission.

## 3. Alert Presentation

- [ ] 3.1 Build the message with `Core.bundle.format` containing the builder, distance, and reactor localized name.
- [ ] 3.2 Wrap the message in red color markup and show it via `Vars.ui.chatfrag.addMessage(...)`.
- [ ] 3.3 Guard against an unavailable chat fragment so presentation never throws.

## 4. Settings UI

- [ ] 4.1 Create `ReactorAlertSettingsView` extending `BaseComponent` with a radius slider from 1 to 30 bound to `radiusConfig`.
- [ ] 4.2 Create `ReactorAlertSettingsDialog` extending `SolimDialog` hosting the view, and return it from `getSettingDialog()`.

## 5. Internationalization

- [ ] 5.1 Add `feature.reactor-alert.name` and `feature.reactor-alert.description` keys with translator comments.
- [ ] 5.2 Add the settings label key for the radius slider with a translator comment.
- [ ] 5.3 Add the alert message key with documented placeholders for builder, distance, and reactor name.

## 6. Asset

- [ ] 6.1 Add `assets/icons/triangle-alert.png` (asset supplied by the requester).

## 7. Verification

- [ ] 7.1 Compile the mod (`:mod:compileJava`) with no errors.
- [ ] 7.2 In singleplayer/host, verify an alert appears when a reactor begins construction near a core within the radius.
- [ ] 7.3 Verify no alert is shown for the local player's own reactor placement.
- [ ] 7.4 Verify the alert is suppressed when placed beyond the configured radius and after changing the radius in settings.
- [ ] 7.5 Verify a second qualifying placement within 3 seconds is suppressed by the cooldown.
