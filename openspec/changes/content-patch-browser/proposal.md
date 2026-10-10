## Why

Players and content creators currently use Mindustry Tool to discover maps and schematics directly in-game, but there is no in-game browser for **Content Patches** (data patches). Community members create balance tweaks, custom block/unit stats, and mechanics modifications hosted on Mindustry Tool (`https://mindustry-tool.com/en/content-patches`), but players have to manually browse external websites and copy-paste text files. Providing an in-game Content Patch Browser makes discovering, inspecting, copying, and saving data patches seamless.

## What Changes

- Add service endpoints in `MindustryTool.java` to search content patches, fetch patch details, and download raw patch data.
- Create response models `ContentPatchData` and `ContentPatchDetailData` representing content patch summaries and details.
- Add `PatchBrowserFeature` registered under feature ID `patch-browser` with an icon (`Icon.wrench`), keybind, and dialog launch capability.
- Create `PatchBrowserDialog` providing a responsive Solim grid layout, search header, tag filters, sorting, and pagination.
- Create `PatchCard` displaying patch name, type badge (`DATA` vs `DATAPACK`), author, short description, and metrics (downloads, likes).
- Create `PatchDetailDialog` displaying full metadata, author, tags, and a scrollable syntax/code preview of the patch HJSON content.
- Implement actions in `PatchActions`:
  - **View Code**: Open the detail inspection dialog.
  - **Copy to Clipboard**: Copy raw HJSON patch to clipboard.
  - **Save to File**: Export `.hjson` into the game directory (`data/patches/<name>.hjson`).
- Add localized strings for all user-facing text to `bundle.properties`.

## Capabilities

### New Capabilities
- `content-patch-browser`: In-game browsing, search, preview, clipboard copy, and file export for Mindustry Tool content patches.

### Modified Capabilities
- `browsers`: Extends the common browser experience (shared filter, footer, state, and search patterns) to include content patch data browsing.

## Impact

- **Services**: Adds `/api/v4/content-patches` search, detail, and data endpoints to `MindustryTool.java`.
- **UI**: Adds new feature entry to the tool menu and creates new Solim browser views under `features/browser/patch/`.
- **Filesystem**: Adds file export capability to write `.hjson` patches into Mindustry's data directory.
- **Localization**: Adds new translation keys in `assets/bundles/bundle.properties`.
