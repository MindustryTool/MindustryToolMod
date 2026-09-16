## MODIFIED Requirements

### Requirement: Double disposal is safe
Calling `dispose()` more than once on any `Disposable` in `solim` and `solim-*` packages SHALL be a no-op on subsequent calls, and `isDisposed()` SHALL return `true` once disposed. This extends the previous `BaseComponent`-only guarantee to every covered type, including leaf components that previously inherited the default `false`.

#### Scenario: Second dispose() call
- **WHEN** `dispose()` is called a second time
- **THEN** no exception is thrown, no resources are disposed again, and `isDisposed()` returns `true`
