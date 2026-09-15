## Why

The current custom music feature uses outdated imperative Arc UI logic, raw Core.settings JSON manipulation, and brittle state tracking. A rewrite is needed to adapt the feature to the modern Solim declarative UI architecture, utilize the new \ConfigGroup\ tracking, and provide reactive, flicker-free updates when adding, removing, or previewing tracks without rebuilding the entire UI.

## What Changes

- Full rewrite of \MusicFeature\ leveraging the modern \Feature\ base class and Solim's \ConfigGroup\.
- Replaces raw UI with declarative \MusicSettingsDialog\ and \MusicSettingsView\ using Solim UI.
- Adopts reactive states (\Signal<Boolean>\ for playing and disabled states) to update individual UI track elements surgically rather than rebuilding the whole window.
- Maintains the integration strategy of keeping a backup of the original \Vars.control.sound.*Music\ lists and modifying the original lists in place, as this is the safest way to ensure Mindustry randomly plays our custom configuration.
- Identifies disabled songs robustly by \	ype + name\.
- Prevents file overwriting conflicts when copying to \Main.musicsDir\ by appending unique identifiers if a name conflict occurs.
- Implements proper i18n for all user-visible strings via \undle.properties\.

## Capabilities

### New Capabilities
- \custom-music-player\: Allows the user to load custom .mp3, .ogg, and .wav files into the ambient, dark, and boss music lists, and dynamically toggle them on or off.

### Modified Capabilities
- (None)

## Impact

- Modifies \MusicFeature.java\ and creates new \MusicSettingsDialog.java\ and \MusicSettingsView.java\ in the modern \eatures/music\ directory.
- Deprecates the old implementation located in \old/mindustrytool/features/music/\.
- Updates \undle.properties\ for full i18n support.
