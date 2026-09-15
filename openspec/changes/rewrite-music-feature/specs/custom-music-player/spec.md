# Custom Music Player

## Requirements

1. **Track Injection:** The mod MUST be able to append custom audio files (\.ogg\, \.mp3\, \.wav\) to Mindustry's Ambient, Dark, and Boss music rotations.
2. **Persistence:** Imported files MUST be copied to the mod's internal storage (\Main.musicsDir\). File names MUST NOT overwrite existing files.
3. **Toggle Original & Custom:** Users MUST be able to independently disable any original game track or any custom track. Disabled tracks MUST NOT play during gameplay.
4. **Reactivity:** Toggling a track's disabled state, or previewing/pausing a track, MUST update the UI reactively without causing a full dialog rebuild.
5. **Previewing:** Users MUST be able to preview a track by pressing a play button in the UI.
6. **I18n:** All UI strings (buttons, tooltips, dialog headers) MUST be loaded from \undle.properties\.
7. **Clean Restore:** The mod MUST restore the original state of \Vars.control.sound.*Music\ when the feature is disabled.
