## 1. Refactor NetworkImage Layout & Spacing Architecture

- [x] 1.1 Remove unused leaf padding methods (padding, paddingX, paddingY), custom applySpacing(), and private padding/margin state fields from NetworkImage.java
- [x] 1.2 Set this.image.userObject = this in NetworkImage constructor so PendingCellConfig.find recognizes it during ParentStack mounting
- [x] 1.3 Update alignment methods (top(), left(), center()) to buffer into constraints.alignTop(), constraints.alignLeft(), and constraints.alignCenter() as well as updating the parent cell if already attached

## 2. Test Deduplication

- [x] 2.1 Remove copy-pasted duplicate tests from NetworkImageComponentTest.java (placeholder, success loading, fallback, reactive URL, dispose reload) which already reside in NetworkImageTest.java and NetworkImageDisposalTest.java
- [x] 2.2 Verify that NetworkImageTest.java and NetworkImageDisposalTest.java pass independently

## 3. Implement Comprehensive NetworkImage Layout Tests

- [x] 3.1 Test element geometry and sizing (size(w, h), size(s), width(w), height(h)) and reactive dimensions (width(Readable), height(Readable), size(Readable)) on standalone NetworkImage
- [x] 3.2 Test element position (x, y, position), origin (origin), visibility (visible), and opacity/alpha (opacity, alpha)
- [x] 3.3 Test parent Table cell size constraints (cell.width, cell.height) and bounds (minWidth, maxWidth, minHeight, maxHeight) via CellAccess
- [x] 3.4 Test parent Table cell grow behavior (growX(), growY(), grow()) via CellAccess
- [x] 3.5 Test parent Table cell margins (margin(m), margin(top, left, bottom, right), directional marginTop/Bottom/Left/Right, marginX/Y, and reactive margins) via CellAccess
- [x] 3.6 Test declarative and imperative alignment (top(), left(), center()) via CellAccess.align
- [x] 3.7 Test declarative ParentStack mounting integration (row(), column(), or ParentStack.push/pop) verifying automatic binding of cell constraints and margins

## 4. Verification & Polish

- [x] 4.1 Run all Solim tests via ./gradlew :solim-core:test and verify zero failures and clean execution
- [x] 4.2 Verify mod compilation via ./gradlew :mod:classes to ensure no regressions in game features
