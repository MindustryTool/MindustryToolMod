## 1. Config & State Setup

- [x] 1.1 Implement \TrackState\ class with \Signal<Boolean> isPlaying\ and \Signal<Boolean> isDisabled\.
- [x] 1.2 In \MusicFeature.java\, replace old fields with \ConfigValue<Seq<String>>\ for ambientPaths, darkPaths, bossPaths, and disabledTracks.
- [x] 1.3 Add all necessary i18n keys to \ssets/bundles/bundle.properties\.

## 2. Core Logic

- [x] 2.1 Implement \captureOriginalMusic\ and \estoreOriginalMusic\ to backup and restore \Vars.control.sound.*Music\ lists safely.
- [x] 2.2 Implement \loadMusicType\ to clear the game's internal music \Seq\, append originals, append custom files, and skip disabled ones.
- [x] 2.3 Implement robust file copying logic in \ddTrack\ that appends a suffix (e.g. \_1\, \_2\) if a filename conflict occurs in \Main.musicsDir\.
- [x] 2.4 Hook up the logic so changes to \TrackState.isDisabled\ update the \disabledTracks\ config and trigger a reload of the music type.

## 3. Solim UI Implementation

- [x] 3.1 Create \MusicSettingsDialog.java\ as a \SolimDialog\.
- [x] 3.2 Create \MusicSettingsView.java\ using Solim declarative layout to replicate the Ambient, Dark, and Boss lists.
- [x] 3.3 Create the file-picker logic in the UI to allow adding custom tracks.
- [x] 3.4 Bind the play/pause and disable toggle buttons in the UI to the \TrackState\ reactive signals so they update instantly without a dialog rebuild.
