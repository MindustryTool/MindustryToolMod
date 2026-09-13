## Context

The mod currently uses static text labels (`Core.bundle.get("browser.loading")`, `auth.login.loading`, etc.) to communicate loading states to the player. An asset `assets/icons/loader-circle.png` exists in the repository but has not yet been utilized.

## Goals / Non-Goals

**Goals:**
- Create an animated `Loader` component in `mindustrytool.components.Loader` that renders `loader-circle.png` and spins continuously around its exact center.
- Ensure the spinner rotates without wobbling by dynamically maintaining `setOrigin(Align.center)` and rotating via `Time.delta`.
- Provide a clean, declarative `Loader.centered()` helper that fills available space and centers the spinner both vertically and horizontally.
- Replace static loading text labels across dialogs (`SchematicBrowserDialog`, `MapBrowserDialog`, `AuthLoginDialog`, and `BrowserFilterDialog`) with the centered animated loader.

**Non-Goals:**
- Creating complex multi-step animated sequences or custom shaders.
- Modifying backend fetch logic.

## Decisions

### 1. Dedicated `Loader` Component in `mindustrytool.components`
- **Decision**: Implement `Loader` as a `BaseComponent` in package `mindustrytool.components`.
- **Rationale**: `loader-circle.png` is located in the mod assets and loaded via `FileIcon.of("loader-circle.png")`. Encapsulating it in `mindustrytool.components` adheres to the feature-oriented mod architecture without polluting core Solim with mod-specific assets.

### 2. Centered Origin & Rotation
- **Decision**: On each frame update, enforce `img.setOrigin(Align.center)` and decrement rotation:
  ```java
  img.update(() -> {
      img.setOrigin(Align.center);
      img.rotation -= speed * Time.delta;
  });
  ```
- **Rationale**: In Arc, `setOrigin(Align.center)` computes the origin from the element's current width and height. Setting it inside `update()` ensures that even after layout resizing, the origin stays strictly in the center, eliminating any visual wobble or off-center rotation.

### 3. Screen Centering Helpers
- **Decision**: Expose `Loader.centered()` and `Loader.centered(float size)` which return `row().grow().center().children(() -> new Loader(size))`.
- **Rationale**: Dialog content areas in `SchematicBrowserDialog` and `MapBrowserDialog` use `.grow()` on their dynamic containers. Returning a centered row ensures the spinner occupies the entire body area and is positioned at the exact visual center of the dialog.

## Risks / Trade-offs

- **[Risk] Missing texture if mod root is uninitialized in tests** → In unit tests where `FileIcon.of` cannot find the file, `FileIcon` falls back to `Icon.book` or a dummy drawable without crashing.
  *Mitigation*: `FileIcon.of` already handles fallback gracefully.
