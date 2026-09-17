## Why

Players have no in-game way to browse Mindustry's `Iconc` font glyphs for use in chat, schematics, and labels. An old imperative `IconBrowserDialog` exists only in legacy `old/` code and is unreachable from the current Solim UI, so this resurrects it as a modern searchable Emoji dialog.

## What Changes

- Add standalone Emoji feature (`features/emoji/`) with feature card and QuickAccess HUD entry using a new Lucide `smile.png` icon.
- Add `EmojiDialog` (SolimDialog): search bar filtering by `Iconc` field name, `wrap()` flowing grid of glyph + field-name chips, click any chip copies the glyph char to clipboard with a toast.
- Cache the `Iconc` reflection result once in a small provider (same approach as legacy `IconUtils`: reflect `Iconc.class.getDeclaredFields()`, keep `Character` fields).
- Add `assets/icons/smile.png` (Lucide smile, 24x24 white RGBA) and new `feature.emoji.*` bundle keys with translator comments.

## Capabilities

### New Capabilities
- `emoji-dialog`: searchable Iconc glyph browser dialog with copy-to-clipboard interaction.

### Modified Capabilities
- `feature-icons`: add `smile.png` to the available Lucide icon assets and assign it to the Emoji feature metadata.

## Impact

- New code under `mod/src/mindustrytool/features/emoji/` (feature, dialog, icon provider); no changes to existing features.
- New asset `assets/icons/smile.png` loaded via `FileIcon.of`, with `Icon.book` fallback.
- New bundle keys in `assets/bundles/bundle.properties`; no API or protocol changes.
