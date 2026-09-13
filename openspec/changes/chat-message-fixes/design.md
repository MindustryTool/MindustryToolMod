## Context

Players sharing links, media, and text in chat experience several usability limitations:
1. Network image loading in `solim-core` (`NetworkImage.java`) has a 10-second timeout (`HttpConnection.timeout = 10000`). Large PNG textures (e.g. ~1MB) hosted on external image servers like `postimg.cc` can take 15–20s on normal player connections. When timeout triggers, it falls back to `Icon.cancel` ("X" icon). Furthermore, requests lack a `User-Agent` header, causing some CDNs to reject or throttle connections.
2. URLs posted in chat are parsed and rendered as raw text. Players cannot click or open them, and there is no visual affordance indicating clickable links. Clicking an external URL in a game mod must prompt user confirmation to avoid accidental navigation or malicious links.
3. In schematic messages, `ChatMessageListView.buildSchematicCard` places action buttons (`Info`, `Export`, `Edit`, `Use`) in the top row alongside the schematic name. This cramps the header row and separates actions from the actual content. Moving action buttons below the schematic preview image makes the card intuitive and clean.
4. `ChatMessageHeightCalculator` currently uses `SCHEMATIC_CARD_HEIGHT = 180f`. Moving buttons beneath the preview image alters the card height to:
   - Header title row: 24f
   - Header-to-preview gap: 4f
   - Schematic preview image: 140f
   - Preview-to-actions gap: 4f
   - Actions row: 28f
   - Card vertical padding: 12f (6f top + 6f bottom)
   - Total: 24 + 4 + 140 + 4 + 28 + 12 = 212f.
   Keeping `SCHEMATIC_CARD_HEIGHT = 212f` ensures exact parity between virtualized list calculations and rendered element dimensions.
5. Currently, clicking a chat message constructs and opens `MessageActionDialog`, an Arc `BaseDialog` modal that obscures the whole screen with a dark background scrim. This is disruptive for fast-paced gameplay and creates unnecessary modal state.

## Goals / Non-Goals

**Goals:**
- Increase `NetworkImage` HTTP request timeout to 30,000ms (30s) and supply a standard `User-Agent` header.
- Detect URLs in text chat messages and highlight them using `WebStyles.Colors.PRIMARY`.
- Make chat links clickable: clicking presents a Solim confirmation dialog displaying the destination URL, a security advisory, and "Open" / "Cancel" buttons. On confirm, open the URL using `Core.app.openURI(url)`.
- Restructure `buildSchematicCard` so action buttons sit in a dedicated row below the 140px schematic preview.
- Update `ChatMessageHeightCalculator.SCHEMATIC_CARD_HEIGHT` to `212f` and verify virtual list height alignment with test coverage.
- Replace `MessageActionDialog` with a lightweight, shared floating action context menu anchored near the clicked message providing Copy, Reply, and Translate.
- Replace translation dialog viewer with in-place translation rendering inside the chat message bubble.
- Ensure all user-facing strings are localized in `bundle.properties`.

**Non-Goals:**
- Rich markdown parsing or arbitrary HTML rendering in chat messages.
- Modifying chat server protocol or payload schemas.
- In-game embedded web browser (links open externally in system default browser).

## Decisions

### Decision 1: NetworkImage Timeout and Request Headers
- Increase default timeout in `NetworkImage.java` from `10000` to `30000` (30 seconds).
- Add header `User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) MindustryToolMod` to requests.
- *Rationale*: Most image CDNs (postimg, imgur, etc.) handle requests from recognizable User-Agents without aggressive throttling or blocking, and 30s allows larger textures (~1-5MB) to download comfortably over mobile or slower broadband connections without premature failure.

### Decision 2: Link Detection & Interactive Click Dialog
- Use regex pattern `https?://[^\s<>"'{}|\\^`]+` to identify URLs within chat messages.
- In `ChatMessageListView.java`, split message text into plain text segments and URL segments.
- URLs are styled with `WebStyles.Colors.PRIMARY` and underlined or highlighted, and wrapped in a clickable Solim element.
- Clicking prompts `SolimDialog` confirmation modal:
  - Title: `Core.bundle.get("chat.link.dialog.title")` ("Open External Link")
  - Body: `Core.bundle.format("chat.link.dialog.prompt", url)` ("Are you sure you want to open this link in your browser?\n\n{0}")
  - Buttons: Cancel and Open (`Core.app.openURI(url)`).
- *Rationale*: Direct automatic opening of links can be a security vulnerability or cause accidental browser focus switches during gameplay. A confirmation dialog protects players and informs them of the exact URL.

### Decision 3: Schematic Card Layout Restructure
- Vertical structure:
  1. Header row: Schematic name + icon (growX, 24px height).
  2. Spacing gap (4px).
  3. Image button: 140px height schematic preview.
  4. Spacing gap (4px).
  5. Action buttons row: 28px height, containing Info, Export, Edit, Use buttons with gap 4px.
- *Rationale*: Places action buttons in natural ergonomic order below the preview image, avoiding title truncation in the header.

### Decision 4: Height Calculator Parity
- Update `ChatMessageHeightCalculator.SCHEMATIC_CARD_HEIGHT` from `180f` to `212f`.
- Card padding is 6px top + 6px bottom (12px). Header is 24px, gap 4px, image 140px, gap 4px, buttons 28px. 24 + 4 + 140 + 4 + 28 + 12 = 212px.
- *Rationale*: Virtualized lists in Solim rely on exact height calculations to determine visible ranges and scroll offsets. Any mismatch causes jitter, blank spaces, or clipping.

### Decision 5: Shared Floating Action Context Menu
- Implement a shared floating menu popup hosted in `ChatOverlayHudView` or `ChatMessageListView` above the virtual list.
- Because list items live inside a scroll pane with scissor clipping, the shared popup is rendered in an overlay layer above the scroll pane to prevent edge clipping.
- The popup state is reactive: `Signal<MessageActionState> actionState = Signal.of(null)` storing the target `ChatMessage` and target anchor coordinates (x, y).
- When a message card is clicked:
  - Calculate the card's relative position in the chat container.
  - Set `actionState` to open the compact floating popup menu directly above or near the message card.
- Menu contains:
  - **Copy**: copies content to clipboard and dismisses popup.
  - **Reply**: invokes `store.setReplyTarget(message)` and dismisses popup.
  - **Translate**: triggers translation for the message and dismisses popup.
- Touching anywhere outside the popup dismisses it.

### Decision 6: In-Place Message Translation
- When Translate is triggered, a translation request is dispatched asynchronously (via `TranslationFeature` or `MindustryTool.translate`).
- Once translated text arrives, `store.setTranslation(message.getId(), result)` is called.
- The text message component observes `store.translation(message.getId())` reactively:
  - When translated text is present, it toggles or displays the translated text directly in-place inside the message bubble with a small translated indicator.
- Supersedes `MessageActionDialog.java`.

## Risks / Trade-offs

- **[Risk] Slower feedback on broken/unreachable image URLs** → With a 30s timeout, completely dead URLs will take 30s before displaying the fallback cancel icon.
  *Mitigation*: The loading placeholder/spinner displays while loading so users know a fetch is underway; 30s is industry standard for HTTP image fetching.
- **[Risk] Context popup clipped near edges of chat window** → If a message is at the very top or bottom of the visible chat window, the popup might extend beyond the chat boundary.
  *Mitigation*: Clamp the popup's X/Y coordinates to ensure it always renders within visible bounds of the chat container.
