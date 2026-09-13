## Why

Currently, loading states throughout the mod (such as in `SchematicBrowserDialog`, `MapBrowserDialog`, and `AuthLoginDialog`) display plain static text labels ("Loading..."). Replacing static text labels with an animated, rotating loading spinner using the existing `assets/icons/loader-circle.png` icon provides a modern, polished visual indicator that clearly signals active network and background processing.

## What Changes

- Create a reusable `Loader` Solim component (`mindustrytool.components.Loader`) in the `mod` module that renders `loader-circle.png` and animates its continuous rotation around its exact center.
- Provide centered container helper methods (`Loader.centered()`) to allow clean full-viewport/full-dialog vertical and horizontal centering.
- Replace static loading text labels with the animated loader across the mod's dialogs:
  - `SchematicBrowserDialog`: Center spinning loader in the dialog body during initial and page loading.
  - `MapBrowserDialog`: Center spinning loader in the dialog body during initial and page loading.
  - `AuthLoginDialog`: Center spinning loader in the dialog while generating the login URL.
  - `BrowserFilterDialog`: Replace tag loading text with a compact inline loader.

## Capabilities

### New Capabilities
- `animated-loader`: Reusable animated spinner component using `loader-circle.png` with smooth center-origin rotation and full-screen / container centering support.

### Modified Capabilities
<!-- None -->

## Impact

- New file: `mod/src/mindustrytool/components/Loader.java`
- Modified dialogs:
  - `mod/src/mindustrytool/features/browser/schematic/SchematicBrowserDialog.java`
  - `mod/src/mindustrytool/features/browser/map/MapBrowserDialog.java`
  - `mod/src/mindustrytool/services/auth/AuthLoginDialog.java`
  - `mod/src/mindustrytool/features/browser/common/BrowserFilterDialog.java`
- No breaking changes or external API changes.
