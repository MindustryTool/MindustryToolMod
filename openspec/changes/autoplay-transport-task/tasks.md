## 1. Data Model, Smart Filtering & Single-Slot Persistence

- [x] 1.1 Create `TransportNode` model (`pos`, `isTarget`, `isDestination`, `transferItems`, `receiveItems`, `expanded` signals) and single-slot map-keyed serializer (`mapKey` + node entries) in `mindustrytool.features.autoplay.tasks`
- [x] 1.2 Implement smart item filtering and default selection helpers for Target (transfer) and Destination (receive) roles (auto-selecting valid items for specialized blocks like turrets/crafters/drills/generators, and starting empty with expanded picker for general storage like Cores/Vaults/Containers/Unloaders)
- [x] 1.3 Hook `WorldLoadEvent` and `StateChangeEvent` to restore persisted nodes when the active map's `mapKey` matches the stored `mapKey` and reset in-memory nodes when switching to a different map

## 2. TransportTask & TransportAI Implementation

- [x] 2.1 Implement `TransportTask` (`id = "transport"`) with continuous selection mode state (`NONE`, `TARGET`, `DESTINATION`), `TapEvent` building selection handler, and `Escape`/`Back` cancellation
- [x] 2.2 Implement demand-driven `(Target, Destination, Item)` matching in `TransportTask.update(Unit)` prioritizing the destination with the lowest current stock of the needed item (ties broken by closest distance)
- [x] 2.3 Implement `TransportAI` (`BaseAutoplayAI`) with rate-limited `Call.requestItem` and `Call.transferInventory`, plus Core deposit / inventory clear fallback when held items are no longer accepted by any destination

## 3. World-Anchored Floating UI & Autoplay Integration

- [x] 3.1 Build declarative Solim floating badge UI (`TransportFloatingOverlay` / `TransportNodeView`) anchored to `Core.camera.project(build.x, build.y)` with compact role badges, expandable multi-item chip pickers, remove buttons, and automatic hiding when `Vars.renderer.getScale() < 0.5f`
- [x] 3.2 Register `TransportTask` in `AutoplayFeature.DEFAULT_ORDER` (above `MiningTask`) and manage floating overlay lifecycle on feature/task enable and disable
- [x] 3.3 Add `Select Targets`, `Select Destinations`, and `Clear All` buttons to `AutoplayHudView` / `AutoplayPopup` (when `TransportTask` is enabled) and to `TransportTask.buildSettings(AutoplayFeature)`

## 4. Localization & Verification

- [x] 4.1 Add translation keys with descriptive comments in `assets/bundles/bundle.properties` for `TransportTask` name, status messages, selection buttons, floating badge labels, and tooltips
- [x] 4.2 Run `./gradlew checkBundles compileJava test` and write unit tests for `TransportTask` node persistence, smart item filtering, and destination priority matching
