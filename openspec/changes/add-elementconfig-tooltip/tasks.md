## 1. Solim Core Tooltip Support

- [x] 1.1 Add static, reactive, and builder `.tooltip()` default methods and helper tooltip removal logic to `ElementConfig`
- [x] 1.2 Deduplicate tooltip methods in `Button` to inherit from `ElementConfig`
- [x] 1.3 Add behavioral unit tests in `solim-core` (`ElementConfigTooltipTest`) verifying static tooltips, reactive updates, custom builder configuration, and listener replacement

## 2. Mod UI Migration

- [x] 2.1 Refactor `GeneralSettingsView.java` to use declarative `.tooltip()` calls directly on checkboxes and remove intermediate `Row` variables
- [x] 2.2 Remove unnecessary single-child row wrappers and clean up unused imports (`arc.scene.ui.Tooltip`, `solim.layout.Row`) in `GeneralSettingsView.java`
- [x] 2.3 Verify `PrettyChatSettingsView.java` to adopt declarative `.tooltip(Cons<Table>)` if applicable

## 3. Verification

- [x] 3.1 Run `./gradlew :solim-core:test` to verify all Solim unit tests pass
- [x] 3.2 Run `./gradlew :mod:classes` to verify mod compilation succeeds with zero warnings or errors
