## 1. Media Source Primitives & Infrastructure

- [ ] 1.1 Create `ImageSource` in `solim.display` with factory methods (`of(Drawable)`, `of(TextureRegion)`, `of(String url)`, `of(Fi file)`)
- [ ] 1.2 Create `VideoSource` in `solim.display.video` with factory methods (`of(String url)`, `of(Fi file)`)
- [ ] 1.3 Extract image caching and decoding into `ImageCache` in `solim.display` (moving memory cache, mask scaling, and disk cache logic)
- [ ] 1.4 Introduce `ImageCapability` trait interface in `solim.modifier`

## 2. Enhance SolimImage & Deprecate/Remove NetworkImage

- [ ] 2.1 Update `SolimImage` to support `ImageSource`, `placeholder`, `fallback`, `rounded`, `scaling`, and asynchronous loading
- [ ] 2.2 Wire `SolimImage` lifecycle with `loadGeneration` tracking and reactive updates for `Readable<ImageSource>`
- [ ] 2.3 Expose cache management facades on `SolimImage` delegating to `ImageCache`
- [ ] 2.4 Delete `NetworkImage.java`

## 3. Modifiers & Solim UI Facade

- [ ] 3.1 Update `Values.java` to route `src(...)`, `placeholder(...)`, `fallback(...)`, and `rounded(...)` for `ImageCapability` and `VideoCapability`
- [ ] 3.2 Update `SolimVideo` to support `VideoSource` and `src(...)` configuration
- [ ] 3.3 Update `UI.java` to expose overloaded `image(...)` methods and redirect `networkImage(...)` to `image(...)`

## 4. Callers Migration & Tests

- [ ] 4.1 Migrate `ChatAvatar.java` and tests from `NetworkImage` to `SolimImage`
- [ ] 4.2 Migrate and consolidate image component tests (`SolimImageComponentTest`, `NetworkImageComponentTest`, `NetworkImageTest`, `NetworkImageDisposalTest`, `NetworkImageDecodeDeterminismTest`) into `SolimImage` tests
- [ ] 4.3 Add unit tests verifying `ImageSource` and `VideoSource` resolution and reactive switching
- [ ] 4.4 Run `./gradlew test` to ensure full test suite passes
