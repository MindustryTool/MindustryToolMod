## 1. Models & Services

- [ ] 1.1 Create `ContentPatchData.java` model for summary list items
- [ ] 1.2 Create `ContentPatchDetailData.java` model for detailed items with HJSON data
- [ ] 1.3 Add `searchContentPatches`, `findContentPatch`, and `downloadContentPatch` methods to `MindustryTool.java`

## 2. Localization & Actions

- [ ] 2.1 Add translation keys and descriptions for Content Patch Browser to `bundle.properties`
- [ ] 2.2 Create `PatchActions.java` with clipboard copy and file export to `data/patches/`

## 3. UI Components

- [ ] 3.1 Create `PatchCard.java` displaying name, type badge, description, category tags, and stats
- [ ] 3.2 Create `PatchDetailDialog.java` displaying complete metadata, tags, and scrollable mono code viewer
- [ ] 3.3 Create `PatchBrowserDialog.java` integrating search header, filter dialog, reactive grid of cards, and pagination footer
- [ ] 3.4 Create `PatchBrowserFeature.java` extending `Feature` with `patch-browser` ID and registering dialog trigger

## 4. Verification & Testing

- [ ] 4.1 Run `:checkBundles` to ensure all bundle keys have required comments and formatting
- [ ] 4.2 Run `./gradlew check` to ensure Java 8 compatibility and compilation succeed
