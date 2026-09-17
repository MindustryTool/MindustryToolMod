## Context

Legacy `old/mindustrytool/IconUtils` reflects `Iconc.class.getDeclaredFields()` and caches `Character` fields; legacy `IconBrowserDialog` filters by field name and copies via `Core.app.setClipboardText`. It is imperative Arc code, unreachable from the current UI, and lives under `old/` which must not be modified. The modern precedent is `GodModeItemsDialog`: `Signal<String>` search + `Readable.map` filter + `dynamic(filtered, list -> wrap()...)` chips inside a `SolimDialog` with `maxWidth`, centered column, and capped scroll. `mindustry.gen.Icon` holds `Drawable`s (not copyable as text) while `mindustry.gen.Iconc` holds `char` glyphs rendered by the game font and pasteable into chat — so the dialog sources `Iconc`. Feature icons follow the `feature-icons` spec: `assets/icons/*.png` (24x24 white RGBA) via `FileIcon.of`, and no smile/emoji asset exists yet.

## Goals / Non-Goals

**Goals:**
- Standalone Emoji feature opening a searchable `Iconc` browser from feature card and QuickAccess HUD.
- Case-insensitive field-name filter with empty query showing all glyphs.
- Flowing `wrap()` chip grid showing glyph + field name; click copies glyph char with toast feedback.
- Full i18n for title, search, toast, empty state, feature name/description/help.

**Non-Goals:**
- Insert-into-chat-input mode (clipboard only; chat hookup is a future change).
- Browsing `Icon` drawables or any non-`Iconc` source.
- Debounced/network search, pagination, categories, or favorites.
- Changes to existing features or `old/` code.

## Decisions

- **Source `Iconc`, not `Icon`**: only `Iconc` chars are copy-pasteable text (`String.valueOf(char)`). `Icon` drawables would render via `icon()` but give users nothing to paste. Alternative (both tabs) rejected as double scope for no stated use case.
- **Cache reflection once in a provider**: static init at class load (same as `IconUtils.iconcs`), reflecting once and selecting non-null `Character` values. Alternative (reflect per dialog open) rejected — wasteful and risks UI-thread jank.
- **Render glyphs with `text()`, not `icon()`**: Solim `icon()` takes a `Drawable`; font glyphs need `text(String.valueOf(char)))` so the game font renders them, paired with `text(fieldName)`.
- **Standalone `features/emoji/` feature**: matches `requested.md` "Emoji list" as its own utility and keeps chat untouched. Alternative (chat input button only) rejected — narrower discovery; both entry points rejected — two integrations for one dialog.
- **Direct `Signal.map` filter, no debounce**: local list of a few hundred items filters instantly; `BrowserSearchHeader` debounce exists for network calls and would add pointless latency here.
- **`dynamic(filtered, ...)` + `wrap()` chips**: follows `GodModeItemsDialog` precedent; `reactiveGrid` is keyed for stateful rows, while icon chips are stateless flow items where `wrap()` auto-flow is the requirement.
- **Copy + toast, dialog stays open**: `Core.app.setClipboardText` + `Vars.ui.showInfoFade` lets users copy several glyphs in one session. Copy-and-close rejected — worse for multi-pick browsing.
- **New `smile.png` Lucide asset**: clearest metaphor; reusing `sparkles`/`message-circle` rejected as vague or chat-misleading. `FileIcon.of` falls back to `Icon.book` if missing.

## Risks / Trade-offs

- [Risk] Some `Iconc` chars may not render in Solim `text()` (font/atlas gaps) → Mitigation: visual check in-game; glyph cell still shows field name so item stays identifiable and copyable.
- [Risk] ~200+ chips in one `wrap()` could feel heavy → Mitigation: cap scroll height (GodMode-style fixed scroll size), stateless chips, no per-chip effects.
- [Risk] Reflection breaks if `Iconc` field types change → Mitigation: filter `instanceof Character` with try/catch per field (as legacy code does), yielding empty list rather than crash.
- [Risk] `smile.png` missing/mis-sized → Mitigation: follow 24x24 white RGBA convention; `FileIcon.of` fallback keeps card renderable.
