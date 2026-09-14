## Context

`ChatInputView.build()` defines `canSend = isSending.map(s -> !s)` at the top and uses it for the send button's `enabled()`, while the message field's `.validator(this::isValidInput)` feeds a `valid` signal nobody reads. `onSend()` re-checks validity imperatively, so invalid input shows an enabled button and fails after tap. Separately, `solim.UI` (the `:solim` public facade mod code must use) has `switchToggle` but no `select`, leaving `SolimSelect` unreachable from mod code. (`switch()` itself is impossible — Java keyword.)

## Goals / Non-Goals

**Goals:**
- Send button disabled while sending or while input is invalid, driven reactively by the field's existing `valid()` signal.
- `solim.UI.select(Signal<T>, List<T>)` facade matching existing input-facade conventions.
- Keep the imperative `onSend()` guard as defense in depth (Enter-key path, race safety).

**Non-Goals:**
- No changes to `SolimTextField` validator machinery, `TwoWayBinding`, `Switch`/`SolimSelect` mirrors, or dead `signal` fields (separate cleanup).
- No new attach helper: the field keeps its existing in-`children()` creation site.
- No behavior change to `switchToggle`.

## Decisions

- **Move `canSend` into the row's `children()` block, composed from the captured field.** `canSend` is used exactly once (send button `enabled()`), after the `textField(...)` chain in the same block. Capturing that chain into a local (`SolimTextField input = textField(...)...;`) and defining `canSend` as a `Computed` over `!isSending && input.valid()` keeps creation order guaranteeing the field exists before the button's eager `Effect` first reads it (`Effect.create` runs immediately). Rejected: hoisting field construction above the tree (attachment would land on the wrong parent — `build()` runs outside the row's scope) and single-element-array holders (order-fragile, ugly).
- **Initial state falls out correctly.** `validator()` recomputes `valid` from current text at set time, so empty input yields invalid → button starts disabled. Previously it started enabled and failed via toast.
- **`select` facade mirrors `checkbox`/`switchToggle`.** Construct via `SolimSelect.of`, attach `selectBox()` to `ParentStack`, return the component. `java.util.List` is already imported in `UI.java`; only `solim.input.SolimSelect` is added. Placed directly after `switchToggle`.
- **Facade test goes in `:solim` `ArcFacadeTest`.** Push a `Table`, call `UI.select` with a 2-option signal, assert attachment plus initial text — same style as existing facade tests; `Switch`/`SolimSelect` already construct headless in `solim-core` tests.

## Risks / Trade-offs

- [Risk] Button disabled state now depends on `valid()` signal timing → Mitigation: `validator()` sets `valid` synchronously at bind time and on every change; `Computed` re-evaluates on either source; `onSend()` guard retained regardless.
- [Risk] `select` generic inference at call sites (`UI.select(signal, options)`) → Mitigation: same shape as existing generic grid facades (`forEach`, `ReactiveGrid.of`); call sites can add explicit type witness if inference fails.
