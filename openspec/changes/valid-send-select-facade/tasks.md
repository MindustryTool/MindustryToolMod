## 1. Reactive send gating in ChatInputView

- [x] 1.1 Compute `canSend` from `messageText` and `isValidInput` as a `Computed` over `!isSending` (tracked `get()`, not `peek()`).
- [x] 1.2 Keep the imperative `isValidInput` guard in `onSend()` and verify `:mod` compiles plus existing `solim-core` input tests still pass.

## 2. Select facade in solim.UI

- [x] 2.1 Add `UI.select(Signal<T>, List<T>)` after `switchToggle` (construct via `SolimSelect.of`, attach `selectBox()` to `ParentStack`, return component) with `solim.input.SolimSelect` import.
- [x] 2.2 Add facade test in `:solim` `ArcFacadeTest` (attach inside pushed `Table`, assert initial text and attachment).
- [x] 2.3 Verify `:solim` tests pass and confirm no new user-visible text and Java 8 compatibility per project rules.
