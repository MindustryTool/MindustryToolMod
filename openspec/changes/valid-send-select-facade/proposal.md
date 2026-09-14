## Why

The chat composer's validation result is computed on every keystroke but read by nobody: the send button only tracks `isSending`, so invalid input (empty, over-long) shows an enabled send button and fails only after tap. Separately, `SolimSelect` has no `solim.UI` facade (`switch` already has `switchToggle`; a literal `switch()` is impossible in Java), leaving the widget unreachable from mod code.

## What Changes

- Wire the chat send button's enabled state to the message field's reactive `valid()` combined with `isSending`, so the button disables while sending or while input is invalid. The imperative guard in `onSend()` stays as defense in depth.
- Add `solim.UI.select(Signal<T>, List<T>)` facade mirroring the existing input facades (construct, attach to `ParentStack`, return component). No `switch()` facade: `switch` is a Java keyword, `switchToggle(Signal<Boolean>)` already covers it.

## Capabilities

### New Capabilities

- None. Both items extend existing capabilities.

### Modified Capabilities

- `chat-feature`: composer send button is enabled only while not sending and input is valid.
- `solim-declarative-ui`: `UI` input facades cover `select` (switch remains `switchToggle`).

## Impact

- Affected code: `mod` `ChatInputView` (send gating); `:solim` `UI.java` (new `select` facade); new facade test in `:solim`.
- Behavior change: send button now disables on empty/invalid input instead of failing after tap with a toast. Empty-content toast path in `onSend()` becomes unreachable via the button (kept for Enter-key and safety).
- No API removals; `SolimSelect`/`Switch` internals untouched.
