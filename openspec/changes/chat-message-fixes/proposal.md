## Why

Chat messages currently face three usability and visual issues:
1. Network images hosted on external CDNs (such as `postimg.cc` or similar image hosts) often fail to load and show a red "X" fallback (`Icon.cancel`) due to an aggressive 10-second timeout and missing standard `User-Agent` headers.
2. URLs shared in chat messages are rendered as plain static text without visual emphasis or click interactivity, making it inconvenient and unsafe for players to open external links.
3. Schematic message cards place action buttons (Info, Export, Edit, Use) in the top title row beside the schematic name, causing visual clutter and awkward button placement; furthermore, placing action buttons below the preview image requires an accurate height update in `ChatMessageHeightCalculator` to avoid virtualized chat list clipping.

## What Changes

- **Extended Network Image Timeout & User-Agent**: Increase the default HTTP timeout in `solim.display.NetworkImage` from 10 seconds to 30 seconds and inject a standard User-Agent header (`MindustryTool/<version>`) so CDN image downloads are not throttled or prematurely aborted.
- **Link Detection, Highlighting & Confirmation Dialog**: Detect URLs in chat text messages, colorize them with the design system primary color (`WebStyles.Colors.PRIMARY`), and open a Solim confirmation dialog with translated prompts before opening the link via `Core.app.openURI(url)`.
- **Schematic Card Layout Restructure**: Move preview action buttons in schematic chat cards to a dedicated row below the schematic preview image.
- **Synchronized Schematic Height Calculation**: Update `ChatMessageHeightCalculator.SCHEMATIC_CARD_HEIGHT` from `180f` to `212f` (accounting for title row, gaps, 140px image, 28px action buttons row, and card padding) to maintain exact layout parity in the virtual list.
- **Localization**: Add translatable bundle strings for the external link confirmation dialog.

## Capabilities

### New Capabilities

### Modified Capabilities
- `solim-network-image`: Add HTTP timeout resilience and standard User-Agent request headers to prevent CDN download timeouts and 403/throttling responses.
- `chat-message-group-layout`: Add clickable link detection with confirmation dialog in text messages, relocate schematic card action buttons below the preview image, and update `SCHEMATIC_CARD_HEIGHT` to match the new layout dimensions.

## Impact

- `solim-core/src/solim/display/NetworkImage.java`: Increase timeout constant to 30000ms and add request header.
- `mod/src/mindustrytool/features/chat/ui/ChatMessageListView.java`: Update text message rendering to parse links and show external link confirmation dialog; reorder schematic card elements so action buttons sit below the preview image.
- `mod/src/mindustrytool/features/chat/layout/ChatMessageHeightCalculator.java`: Adjust `SCHEMATIC_CARD_HEIGHT` constant from 180f to 212f.
- `assets/bundles/bundle.properties`: Add translation keys for external link warning/confirmation dialog.
- Tests in `mod/test/mindustrytool/features/chat/`: Verify updated schematic card height calculation and link parsing.
