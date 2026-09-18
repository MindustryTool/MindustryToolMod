## Context

NetworkImage is Solim's component for displaying asynchronously fetched network images. It implements CellConfig<NetworkImage> and ElementConfig<NetworkImage>. However:
1. 	his.image.userObject = this; was missing from its constructor, causing PendingCellConfig.find(child) to return null when elements were mounted inside ParentStack, breaking declarative cell constraints (minWidth, maxWidth, growX, etc.).
2. It carried legacy leaf-element padding methods (padding, paddingX, paddingY), a custom pplySpacing() method, and private padding/margin fields that shadowed CellConfig's margin methods and caused conflict with Arc's cell padding.
3. Alignment methods (	op(), left(), center()) operated only on image.parent directly without buffering into PendingCellConfig.align...(), dropping alignments declared prior to attachment.
4. NetworkImageComponentTest.java copied over 60% of tests from NetworkImageTest.java verbatim, while having 0% coverage for layout methods.

## Goals / Non-Goals

**Goals:**
- Strip out unused inner padding methods, private spacing state fields, and custom pplySpacing() from NetworkImage.
- Do not implement SpacingAware and do not add gap() support to NetworkImage.
- Wire NetworkImage cleanly to CellConfig and PendingCellConfig for all outer cell spacing (margin).
- Ensure 	his.image.userObject = this; is set on construction.
- Enable declarative alignment (	op(), left(), center()) to buffer into constraints and apply to parent cell.
- Prune all copy-pasted duplicate tests from NetworkImageComponentTest.java.
- Write a comprehensive, zero-duplication layout test suite asserting real observable Arc state (image dimensions and CellAccess cell properties).

**Non-Goals:**
- Changing image loading, caching, disk storage, or rounded mask math (these remain covered by NetworkImageTest.java).
- Adding container-level features (images are strictly leaf widgets).
- Introducing breaking changes to external feature components (verified that mod code only uses size() and 	op().left()).

## Decisions

- **Remove padding/applySpacing/SpacingAware from NetworkImage**:
  - *Rationale*: An image has no children. In Arc Scene UI, cell padding is outer margin around the element. Having both internal pad* and margin* fields that both map to cell.pad() causes double-counting and overwrites PendingCellConfig. Outer spacing cleanly belongs in CellConfig.margin(...).
  - *Alternatives considered*: Implementing SpacingAware and keeping padding(). Rejected because padding is conceptually invalid for leaf nodes without inner content.
- **Store Alignment in PendingCellConfig**:
  - *Rationale*: Solim UI is declarative. Modifiers such as .top().left() are frequently chained upon component construction *before* the component is added to an Arc Table cell. Delegating to constraints.alignTop(), constraints.alignLeft(), and constraints.alignCenter() guarantees the alignment is preserved and applied when mounted.
- **Deduplicate Test Responsibilities**:
  - NetworkImageTest.java: Solely tests network loading, cache lookup, fallback, disk cache, and rounded pixel masks.
  - NetworkImageDisposalTest.java: Solely tests disposal safety and signal unbinding.
  - NetworkImageComponentTest.java: Solely tests component geometry, layout modifiers, cell constraints, and ParentStack mounting.

## Risks / Trade-offs

- **[Risk] Existing callers might rely on NetworkImage.padding()** → *Mitigation*: Codebase inspection confirmed zero usages of padding(...) on NetworkImage. Production code (ChatAvatar) only uses size() and 	op().left().
- **[Risk] Modifiers called before vs after Table mount** → *Mitigation*: Alignment and size methods will apply both to constraints (for pending attachment) and to the parent Cell directly if already attached.
