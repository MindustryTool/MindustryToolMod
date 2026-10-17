## MODIFIED Requirements

### Requirement: Declarative SolimVideo Component

`SolimVideo` SHALL be a declarative Solim component created via `solim.UI.video(...)` supporting static and reactive URL sources (`Readable<String>`), and declaring its configuration through `.props(...)`: sizing, rounded corner masking, and the media properties `autoplay`, `loop`, `volume`, `muted`, and `controls`. `autoplay`, `duration`-equivalent timing, and the overlay icons SHALL take plain values because they are read once; `loop`, `volume`, `muted`, and `controls` SHALL accept reactive sources. `muted` SHALL accept a `Signal<Boolean>` and bind two-way, so the overlay's mute toggle writes back to the caller's signal, and SHALL route through the component's internal mute state so there is a single source of truth. The chained setters `autoplay(boolean)`, `loop(boolean)`, `volume(float)`, `muted(boolean)`, `controls(...)`, `url(...)`, `placeholder(...)`, `fallback(...)`, `scaling(...)`, and the overlay-icon methods SHALL NOT exist. When the component is disposed, all allocated GL textures, pixel buffers, `Music` instances, and decode worker tasks SHALL be released immediately.

#### Scenario: Declarative creation and playback

- **WHEN** `solim.UI.video("https://.../demo.mp4").props(autoplay(true))` is constructed in a Solim tree
- **THEN** the component SHALL initiate loading and display frames upon download completion

#### Scenario: Reactive URL update

- **WHEN** a reactive `Readable<String>` URL property changes to a new video URL
- **THEN** the current playback SHALL stop, previous resources SHALL be cleaned, and the new video SHALL begin loading

#### Scenario: Mute toggle writes back

- **WHEN** the video's mute property is bound to a `Signal<Boolean>` and the user clicks the overlay mute button
- **THEN** the signal's value updates to reflect the new mute state

#### Scenario: Component disposal releases resources

- **WHEN** a `SolimVideo` component is removed from the active UI hierarchy and disposed
- **THEN** the underlying `VideoPlayer`, `Music`, GL texture, and background decode threads SHALL be cleanly stopped and disposed

### Requirement: Configurable Controls Toggle

`SolimVideo` SHALL support disabling the control overlay through the `controls` property declared in `.props(...)`, taking a plain `boolean` or a `Readable<Boolean>`. When controls are disabled (`false`), the control overlay elements and hover listeners SHALL NOT be displayed, allowing the component to function as an uncluttered looping motion graphic or banner. The chained `controls(...)` methods SHALL NOT exist.

#### Scenario: Disabling controls hides all overlay elements

- **WHEN** `video(url).props(controls(false))` is declared
- **THEN** no play/pause button, scrub bar, volume toggle, or hover overlay SHALL be rendered over the video

#### Scenario: Reactive control visibility toggle

- **WHEN** a reactive `Readable<Boolean>` control property switches from `true` to `false`
- **THEN** the control overlay SHALL be removed from display immediately
