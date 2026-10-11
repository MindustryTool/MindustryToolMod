## Context

`ArmyControlPopup` (`width(unit(70))`) and `ArmyControlHudView` (`width(unit(65))`) currently duplicate the same 5-block vertical stack: title header, three full-text action buttons, `ActiveSelectionBar` (full `localizedName + x count` chips + section label + empty text), full-width `Add Group` button, and `ControlGroupList` (section label + empty text + full `name + (count) + previews` rows + per-row trash). At ~7 rows tall the three text buttons overlap (see reported screenshot: `Select Map / Select Screen / Deselect All` colliding in 280px), and every label/empty row costs height players cannot spare during RTS micro. Selection, recall, prune, and command-mode mechanics (`Vars.control.input.selectedUnits`, `input.commandMode`) are solid and unchanged — this is a pure display-density rework. Stakeholders: mobile players (small viewport, touch, no hover) and desktop players (popup overlapping playfield).

## Goals / Non-Goals

**Goals:**
- Fit all army actions in one icon toolbar row: select-map, select-screen, deselect-all, add-group, close.
- Reduce chips and groups to icon + count density; keep tap-to-exclude (chips) and tap-to-recall + trash-to-delete (groups).
- Remove all non-interactive chrome: title header, section labels, empty placeholder rows; collapse empty sections.
- Shrink container toward `unit(50-55)` and reuse one compact content component from both popup and HUD.
- Reuse existing bundle keys as tooltips; add no new user-visible strings.

**Non-Goals:**
- Changing selection/recall/prune/command-mode logic or group persistence (groups stay ephemeral per world load).
- Renaming groups, reordering groups, or adding group capacity settings.
- Adding new icons/assets beyond `mindustry.gen.Icon` + unit `uiIcon` drawables.
- Restyling settings dialog (`ArmyControlSettingsView` untouched).

## Decisions

### 1. Single shared compact content component
Build one Solim component (e.g. `ArmyControlCompactBar`) rendering toolbar + chips + groups, embedded by both `ArmyControlPopup::buildContent` and `ArmyControlHudView::build`. Chosen over editing popup and HUD separately because they are line-for-line duplicates today (`ArmyControlPopup.java:69-118` vs `ArmyControlHudView.java:50-98`) and would drift again. Popup keeps `QuickAccessPopupHelper.show` anchoring and stay-open dismissal; HUD keeps draggable position/scale shell — only inner content is shared.

### 2. Icon-only toolbar with existing bundle strings as tooltips
Five `button().style(WebStyles.ghost()).size(unit(11))` icon buttons with `unit(6)` icons and `.tooltip(Core.bundle.get(...))` reusing `feature.army-control.action.select-map / select-screen / deselect-all / add-group` plus close. Alternatives considered: abbreviated text (`Map / Scr / None`) — still overflows in DE/ZH and costs width; two-row text buttons — fixes overlap but doubles height against the explicit save-space goal. Icon-only is language-independent and narrowest; cost is discoverability, mitigated by tooltips on desktop and fixed RTS-conventional order on mobile.

### 3. Chips: unit icon + count, name in tooltip
Replace `text(type.localizedName + " x" + count)` (`ActiveSelectionBar.java:71`) with `image(type.uiIcon).size(unit(5)) + text(count)`, keeping `WebStyles.filterChip()`, `wrap().gap(unit(1.5f))`, tap-to-`deselectUnitType`, and tooltip combining localized name + `hint.deselect-chip`. Alternative (short name, no count) loses quantities needed for army judgment; icon+count preserves both signals at minimum width.

### 4. Groups: unit icons + count + trash, no names
Replace per-row `text(name + " (" + total + ")")` + preview-icon strip (`ControlGroupList.java:81-90`) with the group's living-unit icon summary + total count as the recall button label, keeping `WebStyles.secondary()` recall + `WebStyles.danger()` `unit(9)` trash with `button.delete` tooltip. Add-group moves to toolbar `+`; no separate full-width row. Alternative (keep names) was voted down in Q&A for width; alternative (numbered `1-5` grid) was rejected in favor of keeping explicit trash for touch clarity. Tradeoff accepted: groups without custom names are distinguishable only by composition.

### 5. Strip chrome, collapse empty, close in toolbar
Delete title header row, both section-label rows, and both empty-state rows; `dynamic()` blocks render nothing when their signal is empty. Close (`Icon.cancel`) becomes the trailing toolbar icon instead of a header button. Alternative (floating corner close, inline `No groups — tap +` hint) costs an overlay layer or half-row for marginal discoverability; collapse-entirely wins on the stated maximum-saving goal.

### 6. Narrower container
Shrink content width from `unit(70)/unit(65)` to `unit(50-55)` with existing `padding(unit(2.5f)) / gap(unit(2))` and `SECTION_BG / SECTION_BORDER` tokens unchanged. No new styles; standard `unit(11)/unit(6)` button/icon sizes per project conventions preserve touch targets.

## Risks / Trade-offs

- **[Risk] Icon ambiguity (map vs screen)** → Mitigation: pick most conventional `Icon` pair available, fix order map-screen-clear-add-close in both modes, rely on tooltips; verify on device screenshot before merge.
- **[Risk] Mobile discoverability with zero labels** → Mitigation: no new onboarding; order + tooltips + existing feature help text carry meaning. Accepted by user vote for maximum saving.
- **[Risk] Groups indistinguishable without names** → Mitigation: icon-summary + count still differentiate common cases (e.g. air vs ground squads); documented tradeoff from Q&A round 2.
- **[Risk] 5-icon toolbar crowding at small HUD scale** → Mitigation: shared component inherits existing `ModSettings.effectiveScale` path; keep `gap(unit(1))` minimum and verify at 0.8x–1.2x scale.
- **[Risk] Popup/HUD refactor regression (stay-open, drag, resize-keep-in-screen)** → Mitigation: shell logic untouched; only inner content swapped. Existing `ArmyControlFeatureTest` / `ActiveSelectionBarTest` updated rather than rewritten.
