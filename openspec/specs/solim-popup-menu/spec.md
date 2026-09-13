# solim-popup-menu Specification

## Purpose
Native Solim floating context menu providing reactive provider content, explicit show/hide with stage-coordinate placement, prefer-above/flip/clamp positioning, tap-outside, Back/Escape, and resize dismissal, with headless-safe no-ops.
## Requirements
### Requirement: Reactive provider content
The `Popup` component SHALL render its menu content from a caller-supplied provider function applied to the data passed at show time, rebuilding content on every `show()` call.

#### Scenario: Menu content reflects shown data
- **WHEN** `show(data, x, y)` is called with a data value
- **THEN** the visible menu content is the provider function applied to that data value

#### Scenario: Re-showing hot-swaps content and anchor
- **WHEN** `show()` is called again while the menu is already visible with different data or coordinates
- **THEN** the menu content is rebuilt from the new data and the menu is repositioned at the new anchor without requiring hide first

### Requirement: Explicit show and hide with stage placement
The `Popup` component SHALL expose explicit `show(data, x, y)` and `hide()` controls placing the menu at stage coordinates, independent of the data flow. Setting data never shows the menu by itself.

#### Scenario: Show places menu at anchor
- **WHEN** `show(data, x, y)` is called with a non-null scene
- **THEN** the menu element is added to the scene root positioned at the clamped anchor coordinates

#### Scenario: Hide removes menu
- **WHEN** `hide()` is called while the menu is visible
- **THEN** the menu element is removed from the scene and all menu listeners are detached

### Requirement: Prefer-above anchor positioning with clamping
The `Popup` component SHALL position the menu with its bottom edge at the anchor vertical coordinate (floating above the anchor), flipping below the anchor when there is insufficient space above, and clamping both axes so the menu stays fully within the stage bounds.

#### Scenario: Menu opens above anchor with room
- **WHEN** `show(data, x, y)` is called and the menu fits between the anchor and the top stage edge
- **THEN** the menu bottom edge aligns with the anchor coordinate

#### Scenario: Menu flips below anchor without room
- **WHEN** `show(data, x, y)` is called and the menu does not fit above the anchor
- **THEN** the menu top edge aligns with the anchor coordinate, clamped inside the stage

#### Scenario: Menu stays within stage bounds
- **WHEN** `show(data, x, y)` is called with an anchor near any stage edge
- **THEN** the menu is fully visible inside the stage on both axes

### Requirement: Tap-outside dismissal
The `Popup` component SHALL dismiss the menu when the user touches anywhere outside the menu bounds, swallowing that touch so no underlying element receives it. Touches inside the menu SHALL reach menu children normally.

#### Scenario: Outside tap dismisses and swallows
- **WHEN** the user touches down outside the visible menu bounds
- **THEN** the menu is dismissed and the touch is consumed (underlying elements do not activate)

#### Scenario: Inside tap reaches menu
- **WHEN** the user touches down inside the visible menu bounds
- **THEN** the menu stays open and the touch is delivered to the touched menu child

### Requirement: Back and Escape dismissal
The `Popup` component SHALL dismiss the menu when the Back key (mobile) or Escape key (desktop) is pressed while the menu is visible, consuming the key event so underlying handlers do not also react.

#### Scenario: Escape closes open menu
- **WHEN** Escape is pressed while the menu is visible
- **THEN** the menu is dismissed and the key event is consumed

#### Scenario: Back closes open menu
- **WHEN** the Back key is pressed while the menu is visible
- **THEN** the menu is dismissed and the key event is consumed

### Requirement: Resize dismissal
The `Popup` component SHALL dismiss the menu when a stage resize event fires while the menu is visible, since absolute anchor coordinates do not survive re-layout.

#### Scenario: Resize hides menu
- **WHEN** the stage resizes while the menu is visible
- **THEN** the menu is dismissed

### Requirement: Headless-safe no-ops
The `Popup` component SHALL perform no scene operations when no scene exists, making `show()` and `hide()` safe to call in headless environments.

#### Scenario: Show without scene does nothing
- **WHEN** `show(data, x, y)` is called with no scene available
- **THEN** no exception is thrown and no listeners are attached

#### Scenario: Hide without scene does nothing
- **WHEN** `hide()` is called with no scene available and nothing shown
- **THEN** no exception is thrown

### Requirement: Fluent chainable configuration
The `Popup` component SHALL return its own instance from every configuration and control method (`children`, `rounded`, `border`, `show`, `hide`) so a menu is declared as a single chain.

#### Scenario: Chained configuration yields one instance
- **WHEN** a menu is configured through chained calls (`children(provider)`, `rounded()`, `border()`, `show()`, `hide()`) on a typed instance
- **THEN** each call returns the same single menu instance in order (configuration starts from direct assignment, since generic inference does not propagate through chains rooted at the bare facade call)

### Requirement: Default visible menu chrome
The `Popup` component SHALL render with a default dark rounded menu background so an unstyled menu is visible, while existing `rounded()` and `border()` calls override the chrome.

#### Scenario: Unstyled menu is visible
- **WHEN** a menu is shown without explicit chrome configuration
- **THEN** the menu renders with the default dark rounded background

#### Scenario: Explicit chrome overrides default
- **WHEN** `rounded()` or `border()` is configured before showing
- **THEN** the menu renders with the caller-specified chrome instead of the default
