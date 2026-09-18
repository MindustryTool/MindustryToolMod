## Context

Currently, `BrowserState` maintains a hardcoded `PAGE_SIZE = 20`. In `SchematicBrowserDialog` and `MapBrowserDialog`, cards have fixed square dimensions (`unit(58f)` = 232px) and layout gap `unit(4f)` = 16px. On high-resolution desktop screens (e.g. 1440p or 4K), 10 or more columns fit horizontally, and 4 or more rows fit vertically. Returning only 20 items populates only 2 rows, leaving an awkward, massive empty void below the cards.

The system needs to calculate how many cards can fit on the user's screen at once, and set the API query page size to that capacity, clamped between 20 (min) and 100 (max).

## Goals / Non-Goals

**Goals:**
- Dynamically determine viewport capacity ($\text{cols} \times \text{rows}$) based on current viewport dimensions (`dvw`, `dvh`).
- Clamp the query page size between 20 (minimum) and 100 (maximum).
- Re-query seamlessly whenever viewport card capacity changes (such as on window resize or initial open).
- Extract shared card layout calculations (`calculateColumns`, `calculateRows`, `calculatePageSize`) into a unified `BrowserLayout` class to eliminate duplicated geometry math between `SchematicBrowserDialog` and `MapBrowserDialog`.
- Maintain full compatibility with Java 8 runtime, Solim reactive conventions, and Mindustry backend search APIs.

**Non-Goals:**
- Changing backend API pagination contracts (the backend already supports `size` up to 100).
- Virtualizing the card grid (infinite scroll / virtualization is handled in separate work).
- Modifying card internal layout or actions.

## Decisions

### Decision 1: Shared `BrowserLayout` utility for responsive grid geometry
Create `mindustrytool.features.browser.common.BrowserLayout` holding layout constants and pure calculation methods:
- `CARD_WIDTH = unit(58f)` (232px)
- `CARD_HEIGHT = unit(69f)` (276px = 232px preview + 8px gap + 36px action row)
- `CARD_GAP = unit(4f)` (16px)
- `HORIZONTAL_PADDING = unit(4f)` (16px on each side, total 32px)
- `VERTICAL_OVERHEAD = unit(44f)` (176px = title bar ~48px + dialog padding 16px + search header 44px + footer 40px + gaps 16px)

**Capacity formula:**
- $\text{cols} = \max(1, \lfloor (\text{availableWidth} + \text{CARD\_GAP}) / (\text{CARD\_WIDTH} + \text{CARD\_GAP}) \rfloor)$
- $\text{rows} = \max(1, \lfloor (\text{availableHeight} + \text{CARD\_GAP}) / (\text{CARD\_HEIGHT} + \text{CARD\_GAP}) \rfloor)$
- $\text{capacity} = \text{cols} \times \text{rows}$
- $\text{pageSize} = \min(100, \max(20, \text{capacity}))$

*Rationale:* Consolidating constants and formulas in `BrowserLayout` guarantees identical, bug-free behavior between schematic and map browsers.

### Decision 2: Reactive `pageSize` signal in `BrowserState`
In `BrowserState<T>`:
- Replace static `public static final int PAGE_SIZE = 20` with a reactive `Signal<Integer> pageSize = Signal.of(PAGE_SIZE_MIN)`.
- Provide `pageSize()` (readable signal), `getPageSize()`, and `setPageSize(int size)`.
- In `setPageSize(int size)`: clamp to $[20, 100]$. If the clamped value differs from current, update signal and call `resetPage()`.
- Include `pageSize.get()` in `BrowserState.start()`'s `autoFetch` effect so changes to `pageSize` trigger a refetch of page 0.

*Rationale:* Integrates with Solim's declarative reactivity. When dialog initializes or window resizes past a capacity threshold, the browser updates automatically without imperative plumbing.

### Decision 3: Reactive binding in `BrowserContent`
In `BrowserContent` (in both `SchematicBrowserDialog` and `MapBrowserDialog`):
- Bind `Computed<Integer> calculatedPageSize` to `viewportWidth` and `viewportHeight` (`dvw(100f)`, `dvh(100f)`).
- Use `effect(() -> state.setPageSize(calculatedPageSize.get()))`.
- Because integer division only changes when crossing card boundaries (~248px horizontal or ~292px vertical), resizing by a few pixels will not trigger state updates or network queries.

*Rationale:* Natural Solim lifecycle ownership. The effect is disposed when the dialog content is disposed.

## Risks / Trade-offs

- **[Risk] High-resolution displays fetching 100 items might increase load time slightly** → Backend returns item metadata (JSON) quickly, and image loading is already asynchronous/lazy via network image components. Clamping to 100 ensures response payloads stay small.
- **[Risk] Window dragging on desktop could trigger multiple queries** → Integer thresholding prevents triggers unless a full column/row boundary is crossed; when dialog is closed, `state.stop()` disables fetching completely.
