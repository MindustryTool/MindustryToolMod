## 1. Assets and Localization

- [ ] 1.1 Add `assets/icons/smile.png` Lucide smile icon (24x24 white RGBA).
- [ ] 1.2 Add `feature.emoji.*` bundle keys (title, search placeholder, empty state, copied toast with `{0}`, feature name/description/help) with translator comments.

## 2. Glyph Source

- [ ] 2.1 Create cached `Iconc` reflection provider pairing field names with glyph chars, skipping unreadable/non-character fields.

## 3. Emoji Dialog UI

- [ ] 3.1 Create `EmojiDialog` SolimDialog shell with `maxWidth`, centered column, close button, and localized title.
- [ ] 3.2 Add search bar bound to a `Signal<String>` filtering entries by case-insensitive field-name match.
- [ ] 3.3 Render filtered entries as `wrap()` chips inside capped scroll, each chip showing glyph text plus field-name text with an empty-state message.
- [ ] 3.4 Implement chip click to copy glyph char via `Core.app.setClipboardText` with localized toast feedback.

## 4. Feature Wiring and Verification

- [ ] 4.1 Create `EmojiFeature` metadata (name/description keys, `FileIcon.of("smile.png")`, QuickAccess entry) opening `EmojiDialog`.
- [ ] 4.2 Verify Java 8 runtime compatibility, `arc.util.Nullable` usage, imports, ternary style, and no `signal.get()` in `build()`.
