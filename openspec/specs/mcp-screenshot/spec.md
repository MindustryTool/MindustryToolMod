## ADDED Requirements

### Requirement: Screenshot capture on the render thread
The `take_screenshot` MCP tool SHALL capture the current game framebuffer via `ScreenUtils` and encode it as PNG. By default it captures the full frame; when all of `x`, `y`, `width`, `height` are provided it captures that region instead. Capture SHALL run on the render thread via `Core.app.post` with a bounded wait; when no render thread is available the tool SHALL fail with a user-facing error.

#### Scenario: Successful capture returns PNG metadata
- **WHEN** `take_screenshot` is called with no arguments while the game is running
- **THEN** the result contains `mimeType` = `image/png`, positive `width`/`height`, positive `bytes`, and a non-empty `image` data URL starting with `data:image/png;base64,`

#### Scenario: Capture without render thread fails cleanly
- **WHEN** `take_screenshot` is called with no `Core.app` or graphics context (e.g. headless unit test)
- **THEN** the tool throws `MCPException` describing that screenshot capture requires a running game

#### Scenario: Capture timeout fails cleanly
- **WHEN** the render-thread handoff does not complete within the timeout
- **THEN** the tool throws `MCPException` describing the timeout instead of blocking forever

### Requirement: Optional region capture
The tool SHALL accept optional integer arguments `x`, `y`, `width`, `height` (framebuffer pixels, bottom-left origin) defining a capture region. When none are provided the full frame is captured. A partially out-of-bounds region SHALL be clamped to the framebuffer; a fully out-of-bounds or non-positive region SHALL fail with a user-facing error.

#### Scenario: Region capture returns region-sized image
- **WHEN** `take_screenshot` is called with a fully in-bounds `x`, `y`, `width`, `height`
- **THEN** the returned `width`/`height` equal the requested region size (before downscaling) and the result contains a `region` object echoing the effective rect

#### Scenario: Partially out-of-bounds region is clamped
- **WHEN** `take_screenshot` is called with a region extending past the framebuffer edge
- **THEN** the capture succeeds with the region clamped to the visible bounds and the `region` echo reflects the clamped rect

#### Scenario: Partial region arguments rejected
- **WHEN** `take_screenshot` is called with only some of `x`, `y`, `width`, `height`
- **THEN** the tool throws `MCPException` requiring all four together

#### Scenario: Fully out-of-bounds or empty region rejected
- **WHEN** `take_screenshot` is called with a region entirely outside the framebuffer or with non-positive `width`/`height`
- **THEN** the tool throws `MCPException` describing the invalid region

### Requirement: 1 MB size enforcement via downscaling
The PNG payload SHALL NOT exceed `maxBytes` (default 1_048_576 bytes). When the native-resolution PNG is larger, the tool SHALL iteratively downscale and re-encode until the payload fits or attempt limits are reached, and SHALL report the final dimensions with a `scaled` flag.

#### Scenario: Small frame returned unscaled
- **WHEN** the native PNG is already within `maxBytes`
- **THEN** the result has `scaled` = false and `width`/`height` equal to the captured framebuffer size

#### Scenario: Large frame scaled down to fit
- **WHEN** the native PNG exceeds `maxBytes`
- **THEN** the result has `scaled` = true, `bytes` ≤ `maxBytes`, and `width`/`height` smaller than the native framebuffer while preserving aspect ratio

#### Scenario: Custom maxBytes respected
- **WHEN** `take_screenshot` is called with `maxBytes` = 200000
- **THEN** the returned `bytes` is ≤ 200000 (scaling more aggressively as needed)

#### Scenario: Custom maxWidth pre-scale applied
- **WHEN** `take_screenshot` is called with `maxWidth` = 640 on a wider framebuffer
- **THEN** the returned `width` is ≤ 640

### Requirement: Base64 JSON delivery in the standard tool envelope
The screenshot SHALL be delivered inline as base64 text inside the existing `ObjectNode` tool result, requiring no transport or protocol changes.

#### Scenario: Result shape for agents
- **WHEN** `take_screenshot` succeeds
- **THEN** the result node contains string field `image` (data URL), string field `mimeType`, integer fields `width`, `height`, `bytes`, and boolean field `scaled` (plus a `region` object echoing the effective rect only when a region was requested)

#### Scenario: Result travels through tools/call
- **WHEN** invoked via JSON-RPC `tools/call` with `name` = `take_screenshot`
- **THEN** the response `result.content[0]` is a text item containing the tool JSON including the `image` data URL
