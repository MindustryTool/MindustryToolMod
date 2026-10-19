## 1. Core Blur Pipeline

- [ ] 1.1 Create `BlurRenderer` in `solim.graphics` managing pooled `PingPongBuffer` instances and lazy FBO resizing
- [ ] 1.2 Implement downsampled sub-rect backbuffer capture using `glCopyTexSubImage2D` with proper scissor clipping and UV orientation
- [ ] 1.3 Implement multi-pass Gaussian blur execution via `GaussianBlurFilter` with GL ES support and fallback checks

## 2. Modifiers & Solim Capabilities

- [ ] 2.1 Implement `BlurCapability` trait and define backdrop and content blur data structures
- [ ] 2.2 Integrate backdrop blur rendering hook into container/card draw pipelines before child rendering
- [ ] 2.3 Implement content blur container hook rendering children to an offscreen buffer before blitting
- [ ] 2.4 Expose `backdropBlur(float)` and `contentBlur(float)` in `solim.modifier.Values` and `Props`

## 3. Solim Facade & Overlay Integration

- [ ] 3.1 Expose `backdropBlur` and `contentBlur` in public `solim.UI` facade
- [ ] 3.2 Add backdrop blur support to `SolimDialog` and `Popup` overlays
- [ ] 3.3 Add global setting/configuration toggle for blur with fallback to alpha tint

## 4. Testing & Verification

- [ ] 4.1 Add unit tests for blur modifier registration, prop application, and capability contracts
- [ ] 4.2 Add headless test verifying graceful fallback on headless/unsupported GL environments
- [ ] 4.3 Add zero-allocation frame loop test verifying no heap allocations occur during steady-state blur drawing
