## Why

Currently, Grief Alert sends text warnings to Mindustry's in-game chat (or shows a standard non-interactive toast when offline). Players cannot quickly locate where the offending placement happened without manually typing or searching tile coordinates. Shifting notifications completely away from chat to an interactive, clickable Solim UI banner allows players to instantly focus their camera on the suspected grief placement and view it with high visual clarity.

## What Changes

- **Remove Chat & Toast Notifications**: Grief Alert no longer sends text messages to `chatfrag` or standard toasts to `hudfrag`.
- **Clickable Solim HUD Banner**: Render a top-centered alert banner on the HUD whenever an explosive reactor placement is detected near a core.
- **Camera Navigation on Click**: Clicking the banner immediately shifts `Core.camera.position` to the world coordinates of the target block and prevents immediate camera reset.
- **Dismiss Interaction**: Provide an `[✕]` close button on the banner to dismiss it without moving the camera, or auto-hide after 7 seconds.
- **Offending Location Indicator**: Snapping the camera to the target tile draws a temporary pulsing highlight marker at the block's world coordinates for 3 seconds.

## Capabilities

### New Capabilities
*(None)*

### Modified Capabilities
- `grief-alert`: Modifies notification presentation requirement from chat messages/toasts to an interactive, clickable Solim HUD alert with camera navigation and visual marker.

## Impact

- `mod/src/mindustrytool/features/griefalert/GriefAlertFeature.java`: Remove `chatfrag.addMessage` / `hudfrag.showToast` logic; maintain active alert state (`Signal<@Nullable GriefAlertInfo>`); implement `jumpToAlert` and world marker rendering loop.
- `mod/src/mindustrytool/features/griefalert/GriefAlertHudView.java`: New Solim HUD component displaying the interactive alert banner.
- `assets/bundles/bundle.properties`: Add translation keys for alert banner labels/actions if needed while keeping existing strings clean.
