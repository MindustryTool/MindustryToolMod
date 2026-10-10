## Context

Mindustry Tool provides API endpoints (`/api/v4/content-patches`) for community data patches. In Mindustry v159+, data patches modify game properties (blocks, units, items) without full mod files. The mod codebase already contains robust shared browser primitives in `mindustrytool.features.browser.common` used by `MapBrowser` and `SchematicBrowser`. This design introduces `PatchBrowserFeature` mirroring that structure for content patches.

## Goals / Non-Goals

**Goals:**
- Provide an in-game browser for discovering and filtering content patches with pagination, sort, search, and category tags.
- Display patch cards with concise metadata (name, type, description, stats).
- Provide a detailed modal showing the formatted HJSON patch data with copy and export features.
- Provide actions to copy the patch directly to the clipboard and save it as a `.hjson` file in Mindustry's data folder.
- Ensure all text is internationalized via `assets/bundles/bundle.properties`.

**Non-Goals:**
- Creating an in-game patch editor (players use PatchEditor or manual text editors).
- Modifying game classes or dynamically reloading game scripts beyond native HJSON patch data handling.

## Decisions

### 1. Dedicated `features/browser/patch/` Package
- **Decision**: Place the new browser in `mindustrytool.features.browser.patch/` with `PatchBrowserFeature`, `PatchBrowserDialog`, `PatchDetailDialog`, `PatchCard`, and `PatchActions`.
- **Rationale**: Follows the existing separation of `browser/map/` and `browser/schematic/`, reusing common browser components (`BrowserState`, `BrowserLayout`, `BrowserSearchHeader`, `BrowserFooter`, `BrowserFilterDialog`).
- **Alternatives Considered**: Merging into a single unified tabbed browser dialog with maps/schematics. Rejected to keep features modular, independently toggleable, and consistent with the existing feature system.

### 2. Service Endpoints in `MindustryTool.java`
- **Decision**: Add `searchContentPatches(...)`, `findContentPatch(String id)`, and `downloadContentPatch(String id)` to `MindustryTool.java`.
- **Rationale**: Centralizes all Mindustry Tool API calls in the existing facade adhering to project rules.

### 3. Lightweight Card UI
- **Decision**: Since data patches don't have map renders or schematic sprites, cards will display a clean layout featuring:
  - Header: Name with type badge (`DATA` vs `DATAPACK`).
  - Body: Truncated description text.
  - Footer: Category tags and `BrowserStatsBadge` (downloads, likes).
- **Alternatives Considered**: Placeholder graphic image cards. Rejected because text/stats cards are cleaner and avoid wasteful visual noise.

### 4. Patch Actions
- **Decision**: Provide three primary user actions:
  - **View Code**: Opens `PatchDetailDialog` with a scrollable mono-font code view.
  - **Copy to Clipboard**: Copies the raw HJSON patch to the clipboard with an info toast.
  - **Save to File**: Exports the file to `Vars.dataDirectory.child("patches").child(sanitizedName + ".hjson")`.

## Risks / Trade-offs

- **[Risk] Long or invalid patch text**: Large patches could cause UI lag if rendered in an unbounded label.
  - **Mitigation**: Use Solim's scrollable view with `monoLabel` style and max height constraints.
- **[Risk] Filename collision or invalid characters during file export**:
  - **Mitigation**: Sanitize filename removing illegal filesystem characters (`[\\/:*?"<>|]`) and handle I/O exceptions gracefully with a user error toast.
