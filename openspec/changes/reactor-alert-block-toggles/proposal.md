## Why

Reactor Alert currently warns about every explosive reactor with no way to opt out per block type. Players who routinely build safe, familiar reactors (or play mod packs with many explosive blocks) either endure noise or disable the whole feature and lose protection against the reactors they do care about.

## What Changes

- Add a "Monitored blocks" section to the Reactor Alert settings dialog listing every detected explosive block as a labeled row (block icon, localized name, explosion radius, checkbox).
- Persist one on/off toggle per block, keyed by block name, defaulting to ON for newly discovered blocks (fail-open).
- Gate alert detection on the per-block toggle in addition to the existing explosive-reactor predicate, radius, and cooldown.
- Add All/None bulk buttons to enable or mute every listed block at once.
- Discover the block list lazily by scanning loaded content with the existing explosive predicate, so modded reactors appear without hardcoding.

## Capabilities

### New Capabilities

- (None)

### Modified Capabilities

- `reactor-alert`: the explosive-reactor detection requirement changes from "alert for every explosive reactor" to "alert only for explosive reactors whose per-block toggle is enabled"; adds configurable per-block monitoring requirement.

## Impact

- Feature code: `mod/src/mindustrytool/features/reactoralert/` (`ReactorAlertFeature`, `ReactorAlertSettingsView`, possibly dialog).
- Translation bundle: `assets/bundles/bundle.properties` plus all locale bundles (new section label, All/None labels).
- Settings storage: new per-block boolean keys under the feature's config namespace.
- No changes to alert presentation (chat/toast), radius, cooldown, or builder attribution.
