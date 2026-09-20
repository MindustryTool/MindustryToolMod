## ADDED Requirements

### Requirement: Dialog-Scoped Query Ownership
Queries created for a dialog, including via dialog-owned state holders constructed inside the dialog's component scope, SHALL be disposed automatically when the dialog component disposes, detaching their `QueryCache` observers.

#### Scenario: Open-close cycle releases observers
- **WHEN** a browser dialog is opened and closed
- **THEN** the `QueryCache` observer count for its query key SHALL return to its prior baseline and unobserved entries SHALL become GC-eligible

#### Scenario: State holder created outside scope disposes explicitly
- **WHEN** a state holder owning a `Query` is created outside any component scope
- **THEN** the holder SHALL implement `Disposable` and dispose its `Query` explicitly on teardown
