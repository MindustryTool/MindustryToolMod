## ADDED Requirements

### Requirement: Declarative Backdrop Blur Modifier
The Solim UI framework SHALL provide a declarative `backdropBlur(float radius)` modifier applicable to any container, card, dialog, or popup. When applied, the component SHALL sample and blur the screen content beneath its bounding box prior to drawing its background tint and foreground children.

#### Scenario: Element applies backdrop blur
- **WHEN** a Solim component declares `.props(backdropBlur(8f))`
- **THEN** the screen area behind the component is rendered with a Gaussian blur of the specified radius, and the component's children are drawn sharply on top.

#### Scenario: Default backdrop blur radius
- **WHEN** a Solim component declares `.props(backdropBlur())` without an explicit radius argument
- **THEN** a sensible default blur radius (e.g., 8f) is applied.

### Requirement: Incremental Screen Capture for Layered Backdrop Blur
The blur pipeline SHALL sample the current backbuffer at the moment of the component's draw call so that preceding UI layers and the game world beneath the component are blurred.

#### Scenario: Modal dialog over active UI
- **WHEN** a modal dialog with `backdropBlur` opens on top of an active HUD or game screen
- **THEN** the active HUD elements and game world beneath the dialog modal are captured and blurred together.

#### Scenario: Clipped container within scroll pane
- **WHEN** a card with `backdropBlur` is inside a scroll pane or clipped container
- **THEN** the screen capture region is constrained to the active scissor bounds and does not sample outside the clip area.

### Requirement: Declarative Content Blur Modifier
The Solim UI framework SHALL provide a declarative `contentBlur(float radius)` modifier that post-processes the component and its children into a blurred texture.

#### Scenario: Privacy masking of sensitive content
- **WHEN** a container declares `.props(contentBlur(6f))`
- **THEN** the container and all of its child elements are rendered into an offscreen buffer and drawn blurred to the screen.

### Requirement: Zero Steady-State Frame Allocations
The blur rendering engine SHALL reuse pre-allocated ping-pong framebuffers and texture objects, and SHALL NOT allocate objects on the Java heap during steady-state frame rendering.

#### Scenario: Steady-state render loop execution
- **WHEN** components with backdrop blur or content blur are drawn every frame during continuous gameplay
- **THEN** zero heap memory is allocated by the blur pipeline during the draw phase.

### Requirement: Low-End Hardware and Configuration Fallback
The blur system SHALL detect OpenGL ES framebuffer support and check user preferences, falling back gracefully to standard alpha-blended backgrounds when blur is unsupported or disabled.

#### Scenario: Unsupported OpenGL ES device or disabled setting
- **WHEN** blur is disabled in configuration or the graphics device does not support the required framebuffers
- **THEN** components with `backdropBlur` render with their fallback background color and tint without throwing an error or rendering black quads.
