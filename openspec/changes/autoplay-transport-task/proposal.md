## Why

Players frequently need to shuttle items manually between cores/storage and remote turrets, factories, or containers that lack conveyor infrastructure. Adding an autonomous **Item Transport Task** (`TransportTask`) to Autoplay lets the player's unit automatically ferry selected items from configured source buildings (targets) to destination buildings, complete with in-world selection and floating item-configuration badges.

## What Changes

- Introduce `TransportTask` (`id = "transport"`) in `AutoplayFeature` (default priority above `MiningTask`, completing the 10-task 2×5 HUD strip).
- Add continuous selection controls (**Select Targets**, **Select Destinations**, and **Clear All**) to both the Autoplay HUD/Popup (visible when the Transport task is enabled) and the `TransportTask` collapsible settings card in `AutoplaySettingsView`.
- Render world-anchored Solim floating UI badges over selected target and destination buildings:
  - Automatically hide badges when camera zoom scale is below `0.5` (`Vars.renderer.getScale() < 0.5f`).
  - Support both **Target (Transfer)** and **Destination (Receive)** roles (including dual-role buildings) with a compact badge view (selected item icons, edit/expand toggle, remove button) and an expandable multi-item picker.
  - Filter item choices to items valid for the building and auto-select valid items for specialized blocks (factories, turrets, drills, generators), while starting empty with the picker expanded for general storage blocks (Cores, Vaults, Containers, Unloaders).
- Implement `TransportAI` (`BaseAutoplayAI`) with rate-limited `Call.requestItem` and `Call.transferInventory`, prioritizing destinations with the lowest current item stock (breaking ties by closest distance) and returning unaccepted held items to the closest Core.
- Persist at most **one** map's transport configuration at a time in settings using a composite map identification key (`mapKey`), preventing unbounded storage growth across maps.

## Capabilities

### New Capabilities
- `item-transport-task`: Autonomous item transport task, in-world target/destination building selection, zoom-aware floating item configuration UI, and single-slot map-keyed persistence.

### Modified Capabilities
- `autonomous-gameplay-ai`: Register `TransportTask` in `AutoplayFeature.DEFAULT_ORDER` and expose transport selection controls in the Autoplay HUD and popup when the task is enabled.

## Impact

- `mod/src/mindustrytool/features/autoplay/AutoplayFeature.java`: Register `TransportTask`, hook world lifecycle/tap/update events, and manage floating UI overlay lifecycle.
- `mod/src/mindustrytool/features/autoplay/AutoplayHudView.java` & `AutoplayPopup.java`: Display the transport selection action bar when `TransportTask` is enabled.
- `mod/src/mindustrytool/features/autoplay/tasks/TransportTask.java` (and supporting UI/model classes): Implement target/destination state, smart item filtering, floating world badges, single-map persistence, and `TransportAI`.
- `assets/bundles/bundle.properties`: Add localization keys with descriptive comments for all new task names, statuses, buttons, and tooltips.
