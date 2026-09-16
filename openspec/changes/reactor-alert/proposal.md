## Why

Explosive reactors (notably the Thorium Reactor, explosion radius 19 tiles) can be placed next to a core and, when they overheat or are destroyed, wipe out the core and the surrounding base. On shared servers there is currently no warning when another player starts building an explosive reactor dangerously close to a core.

## What Changes

- Add a new `ReactorAlertFeature` (id `reactor-alert`, display name "Reactor Alert"), registered in `Main`, enabled by default and exposed in Quick Access.
- Detect explosive reactors using the union predicate `block.flags.contains(BlockFlag.reactor) || block.explosionRadius > 0f`, so thorium, impact, flux, neoplasia and modded explosive reactors are all covered.
- Listen to `BlockBuildBeginEvent` so the alert fires while the reactor is still a construction and can still be stopped; resolve the target block from `ConstructBuild.current`.
- Ignore the local player's own placements; when the builder has no attributable player (AI/drone), attribute the alert to the builder's team.
- Measure center-to-center Euclidean distance in tiles between the reactor and the cores of the builder's team; warn when the nearest core is within the configured radius (default 10 tiles).
- Throttle repeat warnings with a global cooldown of 3 seconds.
- Display the warning through `Vars.ui.chatfrag.addMessage(...)` using red color markup, containing the builder (player name or team name), the distance, and the reactor's localized name.
- Add a settings dialog with a configurable alert radius slider from 1 to 30 tiles.
- Add all required translation keys with translator comments to `assets/bundles/bundle.properties`.
- Add the `assets/icons/triangle-alert.png` icon asset used by the feature.

## Capabilities

### New Capabilities
- `reactor-alert`: Warns when another player begins constructing an explosive reactor within a configurable distance of a core.

### Modified Capabilities
- (None)

## Impact

- Feature registration: `mod/src/mindustrytool/Main.java`.
- New feature package: `mod/src/mindustrytool/features/reactoralert/`.
- Translation bundle: `assets/bundles/bundle.properties`.
- New icon asset: `assets/icons/triangle-alert.png`.
- Reads Mindustry world/event APIs: `BlockBuildBeginEvent`, `ConstructBlock.ConstructBuild`, `BlockFlag`, `Vars.state.teams`, `Vars.ui.chatfrag`.