## Why

NetworkImage lacks test coverage for its layout, geometry, parent-cell constraints, alignment, and spacing methods. Furthermore, existing test files (NetworkImageComponentTest and NetworkImageTest) suffer from severe code duplication where network fetching, caching, and fallback tests are copied almost verbatim. Finally, NetworkImage has architectural inconsistencies for leaf elements: it carries redundant and shadowing padding/margin logic instead of delegating cleanly to CellConfig and PendingCellConfig, omits setting image.userObject = this (breaking ParentStack cell constraint application), and loses alignment if 	op(), left(), or center() are called prior to parent attachment.

## What Changes

- **Streamline NetworkImage Layout Architecture**:
  - Remove unused leaf-element padding methods (padding(float), paddingX(float), paddingY(float)), pplySpacing(), and internal padTop/Left/Bottom/Right and marginTop/Left/Bottom/Right state fields since images are leaf nodes without children.
  - Do not implement SpacingAware or add gap() support to NetworkImage.
  - Delegate all outer cell spacing cleanly to CellConfig (margin(float), margin(top, left, bottom, right), marginTop, marginBottom, marginLeft, marginRight, marginX, marginY, and reactive variants).
  - Register 	his.image.userObject = this; in constructor so PendingCellConfig.find(child) correctly binds cell constraints (minWidth, maxWidth, minHeight, maxHeight, growX, growY, grow, margin) during ParentStack mounting.
  - Fix declarative alignment (	op(), left(), center()) to buffer into constraints.align...() in addition to updating the parent cell if already attached.
- **Eliminate Test Duplication**:
  - Remove redundant loader, caching, fallback, and reactive URL test copies from NetworkImageComponentTest.java, keeping NetworkImageTest.java as the dedicated test suite for network fetching, caching, and pixel manipulation.
- **Comprehensive Layout Test Suite**:
  - Expand NetworkImageComponentTest.java to thoroughly test all layout methods, element geometry, parent cell constraints, grow behavior, margin configurations, alignment, and ParentStack lifecycle integration using CellAccess assertions.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- solim-widgets: Updates NetworkImage layout requirements to define explicit element sizing, cell constraints, alignment, and cell-margin behavior while removing leaf-element inner padding and SpacingAware coupling.

## Impact

- **Affected Code**: solim-core/src/solim/display/NetworkImage.java, solim-core/src/test/java/solim/display/NetworkImageComponentTest.java.
- **APIs**: Removes redundant padding(...) on NetworkImage (unused in codebase; outer spacing uses margin(...) via CellConfig). NetworkImage layout methods seamlessly integrate with ParentStack and declarative Solim UI without runtime errors.
