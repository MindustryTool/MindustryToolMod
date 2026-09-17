# custom-music-player Specification

## Purpose

Allow users to load custom `.ogg`, `.mp3`, and `.wav` tracks into Mindustry's ambient, dark, and boss music rotations and to replace the single-slot menu and editor music, with independently toggleable tracks whose disabled and playing states are persisted and derived reactively from the game's own playback.

## Requirements

**Source: rewrite-music-feature**

### Requirement: Track Injection

The system SHALL append custom audio files (`.ogg`, `.mp3`, `.wav`) to Mindustry's Ambient, Dark, and Boss music rotations by mutating the game's own music lists in place.

#### Scenario: Custom track joins a music rotation

- **WHEN** the user imports a custom track and the affected music type is loaded
- **THEN** the track is appended to the corresponding game music list alongside the captured original tracks

#### Scenario: Originals remain in the rotation

- **WHEN** a music type is loaded
- **THEN** the original game tracks remain in the list followed by the enabled custom tracks

### Requirement: File Persistence

The system SHALL copy imported files to the mod's internal storage (`Main.musicsDir`) and SHALL NOT overwrite an existing file with the same name.

#### Scenario: File copied under its original name

- **WHEN** the user imports a file whose name does not already exist in `Main.musicsDir`
- **THEN** the file is copied with its original name

#### Scenario: Name collision given a unique suffix

- **WHEN** the user imports a file whose name already exists in `Main.musicsDir`
- **THEN** a unique suffix (`_1`, `_2`, ...) is appended so the existing file is preserved

### Requirement: Disabled Toggles

The system SHALL allow independently disabling any original or custom track, including tracks that share a display name, and disabled tracks SHALL NOT play during gameplay.

#### Scenario: Disabled track does not play

- **WHEN** the user disables a track and the affected music type is (re)loaded
- **THEN** that track is skipped and never played

#### Scenario: Same-named tracks toggled independently

- **WHEN** an original and a custom track share the same display name
- **THEN** each can be disabled without affecting the other

### Requirement: Disabled Persistence

The system SHALL persist the disabled set as a single list through `ConfigGroup`, keyed by music type and original/custom role, as the single source of truth, and SHALL prune entries for removed or missing custom tracks.

#### Scenario: Disabled state survives restart

- **WHEN** the user disables a track and the game restarts
- **THEN** the track remains disabled

#### Scenario: Stale entries pruned

- **WHEN** a custom track is removed or its file is missing
- **THEN** its disabled-list entry is pruned when the affected music type is loaded

### Requirement: Reactivity

The system SHALL update the UI reactively when a track's disabled state or playback changes, without a full dialog rebuild, and SHALL derive disabled state from the persisted list rather than duplicate it.

#### Scenario: Toggling updates a single row

- **WHEN** the user toggles a track's disabled state
- **THEN** only that track's row updates and the dialog is not rebuilt

#### Scenario: Playback updates the indicator

- **WHEN** the game's playback changes
- **THEN** the playing indicator on the affected rows updates through reactive bindings

### Requirement: Playback State

The system SHALL derive each track's playing state from `Vars.control.sound.getCurrent()` so it reflects the game's actual playback, including natural track end, random rotation, and boss/dark selection.

#### Scenario: State follows natural playback

- **WHEN** the game naturally ends the current track or rotates to a new one
- **THEN** the playing indicator follows `getCurrent()` without timers or duplicated state

#### Scenario: Boss or dark selection reflected

- **WHEN** the game selects boss or dark music
- **THEN** the matching loaded track row is marked as playing

### Requirement: Playback Control

The system SHALL route play through `Vars.control.sound.playMusic(music, true)` so playback uses the game's fade, volume, and mute behavior and interrupts the current track, and SHALL stop via `Vars.control.sound.stop()`.

#### Scenario: Play interrupts the current track

- **WHEN** the user presses play on a track
- **THEN** the game player is asked to play it, interrupting any currently playing track

#### Scenario: Stop preserves game playback state

- **WHEN** the user presses stop
- **THEN** playback is stopped through `Vars.control.sound.stop()` and the game's playback state is preserved

### Requirement: Reload Integration

The system SHALL re-capture the original tracks and re-apply the custom and disabled configuration when `SoundControl.reload()` fires `MusicRegisterEvent`.

#### Scenario: Configuration re-applied after reload

- **WHEN** `SoundControl.reload()` fires `MusicRegisterEvent`
- **THEN** the mod re-captures the original tracks and re-applies the custom and disabled entries

#### Scenario: Injected tracks survive a sound reload

- **WHEN** the game reloads its sound system
- **THEN** injected custom tracks are not lost from the rotations

### Requirement: I18n

The system SHALL load all user-visible UI strings (buttons, tooltips, dialog headers) from `bundle.properties`.

#### Scenario: Dialog strings resolve from the bundle

- **WHEN** the music settings UI is displayed
- **THEN** its headers, buttons, and tooltips resolve from translation keys rather than hardcoded text

#### Scenario: New keys carry translator comments

- **WHEN** a new user-visible UI string is introduced
- **THEN** a corresponding key with a descriptive translator comment is added to `bundle.properties`

### Requirement: Menu & Editor Replacement

The system SHALL replace the single-slot menu and editor music with custom tracks by reassigning `Musics.menu` and `Musics.editor`, choose a random enabled custom track on each menu/editor entry, fall back to the vanilla track when no custom track is enabled, and restore the original tracks when the feature is disabled.

#### Scenario: Random enabled track chosen on entry

- **WHEN** the player enters the menu or editor with at least one enabled custom slot track
- **THEN** a random enabled track is assigned, avoiding an immediate repeat

#### Scenario: Fallback to vanilla track

- **WHEN** no custom menu or editor track is enabled
- **THEN** the captured vanilla `Musics.menu` / `Musics.editor` track is used

#### Scenario: Originals restored on disable

- **WHEN** the feature is disabled
- **THEN** the original menu and editor tracks are restored

### Requirement: Clean Restore

The system SHALL restore the original state of `Vars.control.sound.*Music`, `Musics.menu`, and `Musics.editor` when the feature is disabled.

#### Scenario: Music rotations restored

- **WHEN** the feature is disabled
- **THEN** the captured original ambient, dark, and boss lists are restored and custom tracks are removed

#### Scenario: Menu and editor slots restored

- **WHEN** the feature is disabled
- **THEN** `Musics.menu` and `Musics.editor` reference their original tracks

## Known Limitations

- Maps or planets that define their own music rules (`state.rules.ambientMusic`, `planet.ambientMusic`) bypass the injected lists, so the feature has no effect on those.
- Playback is suppressed when `musicvol` is 0 and is overridden by menu/editor/planet music outside an active game.
