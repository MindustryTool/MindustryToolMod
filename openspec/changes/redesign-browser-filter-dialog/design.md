## Context

`BrowserFilterDialog` (380 lines) is a Solim-declarative modal showing sort options, planet toggles, tag categories, and block toggles. It currently uses `Styles.togglet` (plain Mindustry toggle buttons) while the rest of the browser uses WebStyles. Tags and planets are fetched from the API every time the dialog opens via `FilterContent` constructor, with no caching. Each section uses fixed column counts (2 portrait, 4 landscape for tags) regardless of viewport width or label length.

## Goals / Non-Goals

**Goals:**
- Unify all toggle buttons with `WebStyles.ghostText()` for visual consistency
- Cache tag categories and planet data in static signals — fetch once, persist for app lifetime
- Compute grid columns dynamically from viewport width and longest label, not fixed breakpoints
- Use `WebStyles.dangerText()` for the "Clear all" button

**Non-Goals:**
- Fixing the planet filter split-brain (local vs server-side)
- Changing the double search bar semantics
- Optimizing block list rebuild on keystroke
- Adding TTL or invalidation to the cache

## Decisions

### Decision 1: Ghost toggles for all filter options
- **Approach**: Replace `Styles.togglet` with `WebStyles.ghostText()` on sort, planet, tag, and block buttons. The `.checked()` binding provides the selected visual state (subtle bg highlight).
- **Rationale**: Ghost toggles are transparent at rest, with a subtle white overlay on selection. They match the browser's lightweight design language and feel like a control panel rather than heavy form elements.
- **Alternatives Considered**: `WebStyles.outlineText()` (too heavy for many small buttons), keeping `Styles.togglet` (inconsistent with design system).

### Decision 2: Static signal cache — fetch once, live forever
- **Approach**: Two static `Signal` fields on `FilterContent`: `cachedTags` and `cachedPlanets`. A `static boolean` flag per signal tracks whether data has been fetched. First dialog open fetches and populates; subsequent opens read directly.
- **Rationale**: Tags and planets are reference data that rarely change during a session. Static signals are the simplest caching mechanism — zero config, works with `dynamic()`, no TTL to tune.
- **Alternatives Considered**: TTL-based cache (unnecessary complexity for reference data), no cache (wasteful network calls on every show).

### Decision 3: Viewport-based dynamic column calculation
- **Approach**: Compute column count per section using `max(1, floor(availableWidth / minColWidth))` where `minColWidth = longestLabelWidth + padding(2) + gap(1)`. Clamp to a reasonable range. Each section uses its own longest label width.
- **Rationale**: Fixed 2/4 columns waste space on wide viewports and may overflow on narrow ones. Dynamic calculation adapts to any viewport size and label length.
- **Alternatives Considered**: Fixed breakpoints (not flexible enough), CSS flexbox-style wrapping (Solim grid doesn't support auto-wrap).

### Decision 4: DangerText for clear all
- **Approach**: Replace `Styles.defaultb` with `WebStyles.dangerText()` on the "Clear all filters" button.
- **Rationale**: Clearing all filters is a destructive action (removes user selections). Red text signals this clearly, matching the danger pattern used elsewhere.
- **Alternatives Considered**: `WebStyles.ghostText()` (too subtle for destructive action), `WebStyles.danger()` (too heavy for a text button).

## Risks / Trade-offs

- [Static cache never invalidates] → Acceptable for reference data (tags, planets). If data changes, user restarts the app.
- [Dynamic column calculation adds complexity] → The helper method is ~5 lines. Each section passes its longest label width. Net complexity is minimal.
- [Ghost toggles may be too subtle for some users] → The `.checked()` binding provides visible feedback. The style is already proven in the browser's action buttons.
