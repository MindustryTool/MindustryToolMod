## Context

The current custom music feature uses raw Arc UI and direct JSON settings manipulation. It modifies Mindustry's \Vars.control.sound.*Music\ lists to seamlessly inject user music, but the UI is imperative and rebuilding the list is expensive (rebuilds entire dialog on any state change). We are migrating this to the Solim architecture.

## Goals / Non-Goals

**Goals:**
- Migrate UI to Solim declarative style.
- Maintain the strategy of backing up the original \Vars.control.sound\ lists and mutating the actual game lists in-place so standard game random-play behavior works correctly.
- Ensure that the UI updates surgically via reactive \Signal<T>\ values for play states and disable toggles, rather than full dialog rebuilds.
- Identify disabled sounds by a combination of \MusicType\ and track name to avoid namespace collisions.
- Manage custom files safely by copying them into the mod's app directory (\Main.musicsDir\).

**Non-Goals:**
- We will not implement a completely custom audio playback engine; we continue injecting into Mindustry's \Music\ objects.

## Decisions

1. **State & Solim Reactivity:**
   - We will introduce a \TrackState\ class wrapping each \Music\ object.
   - It will contain \Signal<Boolean> isPlaying\ and \Signal<Boolean> isDisabled\.
   - The UI will iterate over these \TrackState\ objects and bind button visuals directly to these signals.
   
2. **File Management:**
   - When importing a track, we copy it to \Main.musicsDir\. If a file with the same name already exists, we will append a unique suffix (like \_1\) to prevent unintentional overwriting.

3. **Config Management:**
   - Instead of manual \Core.settings\ writes, we will use \ConfigValue<Seq<String>>\ via \configGroup()\ for custom paths and disabled tracks.
   - Disabled tracks will be saved as \<Type>_<Name>\ (e.g. \AMBIENT_song1\) to ensure uniqueness across lists.

## Risks / Trade-offs

- **Risk**: Modifying the game's internal \Seq<Music>\ lists could conflict with other mods doing the same thing.
  - **Mitigation**: This is an accepted risk. Our \onDisable()\ hook restores our captured backup of the original tracks, which is the most polite fallback available.
