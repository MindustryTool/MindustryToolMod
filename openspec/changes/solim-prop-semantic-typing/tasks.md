## 1. Trait markers

- [x] 1.1 Add the semantic trait markers in `solim.modifier` (`TextCapability`, `LabelCapability`, `ButtonCapability`, `CheckableCapability`, `WrappableCapability`, `FieldCapability`, `SliderCapability`, `SelectCapability`, `ScrollCapability`, `VideoCapability`, `IconCapability`), each extending `ElementCapability`
- [x] 1.2 Declare the honoured traits on each host so the compile-time set matches the property it actually applies

## 2. Typed style prop

- [x] 2.1 Add `style(Label.LabelStyle)` / `style(Readable<Label.LabelStyle>)` requiring `LabelCapability`
- [x] 2.2 Add `style(SolimButtonStyle)` / reactive requiring `ButtonCapability`
- [x] 2.3 Add the `TextField.TextFieldStyle`, `ScrollPane.ScrollPaneStyle`, `List.ListStyle`, and `Slider.SliderStyle` overloads requiring their traits
- [x] 2.4 Delete `style(Object)` and `style(Readable<?>)`
- [x] 2.5 Confirm `SolimButtonStyle` is untouched and no Arc style class is re-parented

## 3. Semantic props retyped

- [x] 3.1 `checked(...)` requires `CheckableCapability` (implemented by `Button`, `Checkbox`, `Switch`)
- [x] 3.2 `icon(...)` requires `IconCapability` (`Button`, `Image`/`Icon`)
- [x] 3.3 `wrap(...)`, `ellipsis(...)`, `fontScale(...)` require `WrappableCapability`
- [x] 3.4 `text(...)` requires `TextCapability` and preserves its host-dependent meaning (label, button content, field placeholder)
- [x] 3.5 Retype the media props (`url`, `placeholder`, `fallback`, playback) to require `VideoCapability` / `IconCapability` as appropriate

## 4. Fail fast for the residue

- [x] 4.1 Replace the silent no-op path for a capability-proven property with an `IllegalArgumentException` naming the host and the value class
- [x] 4.2 Keep the no-op only for a genuinely absent (`null`) value, never for a mismatch
- [x] 4.3 Confirm no user-visible message is hardcoded: any thrown message intended for humans uses a bundle key

## 5. Call-site fallout

- [x] 5.1 Build `solim-core`; resolve every `style(...)` call site to a typed overload
- [x] 5.2 Build `mod`; resolve every style call site and every now-invalid `checked`/`icon`/`wrap`/`fontScale`/`text` misuse the compiler rejects

## 6. Tests

- [x] 6.1 Add a rejection test per trait (e.g. a `Label.LabelStyle` on a button, `checked` on an element-only host)
- [x] 6.2 Add a multi-host trait test proving the same trait-typed property applies on `Button`, `Checkbox`, and `Switch`
- [x] 6.3 Add a fail-fast test proving a value-dependent mismatch throws instead of no-op'ing
- [x] 6.4 Add a test proving the typed `style(...)` overload resolves from the argument's static type
- [x] 6.5 Run the full `solim-core` suite

