## Why

The current pathfinding visualization feature relies on outdated architecture, doesn't use the modern Solim UI framework, and lacks correct behavior in client mode. A full rewrite is needed to bring it up to standard (following \AGENTS.md\ rules like internationalization, Java 8 compatibility, and declarative Solim UI) and to correctly trace paths in client mode by hooking into the game's native pathfinding systems (\Vars.pathfinder\ for core-bound units and \Vars.controlPath\ for commanded units) to provide 100% accuracy.

## What Changes

- Full rewrite of \PathfindingFeature\ using the modern \Feature\ base class and \ConfigValue<T>\.
- **BREAKING**: Removes the \.development(true)\ flag and \enabledByDefault\ is set to false.
- Creates declarative \PathfindingSettingsDialog\ and \PathfindingSettingsView\ using Solim UI instead of direct Arc widgets.
- Paths for ally units are now drawn, but the player's own unit is ignored.
- **Client Mode Fix**: Instead of failing to draw paths in client mode (because the game doesn't preload flow fields for enemies on clients), the mod will intentionally invoke \pathfinder.getField()\ and \controlPath.getPathPosition()\ to force computation and accurately replicate server-side pathfinding on the client.
- Implements solid team colors for path lines rather than the old fading effect, making it consistent.
- Heavy optimizations including frame budgets, frustum culling, and deduplication through \PathfindingCacheManager\.
- Proper i18n support for all user-facing strings (e.g., config labels, tooltips, dialogs) added to \undle.properties\.

## Capabilities

### New Capabilities
- \pathfinding-visualization\: Shows movement paths for both wave-bound units (via global flow fields) and commanded units (via HPA* control paths) on both host and client.

### Modified Capabilities
- (None)

## Impact

- Creates: \PathfindingFeature.java\, \PathfindingCache.java\, \PathfindingCacheManager.java\, \PathfindingSettingsDialog.java\, \PathfindingSettingsView.java\ in the modern \eatures/pathfinding/\ directory.
- Deprecates: The old \PathfindingDisplay\ and related classes in the \old/\ package (these will be ignored per project rules, but they are fully superseded).
- Adds keys to \ssets/bundles/bundle.properties\.
- Affects rendering overhead in the \Trigger.draw\ phase, mitigated by extensive caching and culling.
