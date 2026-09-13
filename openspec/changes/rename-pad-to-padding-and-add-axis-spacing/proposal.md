## Why

In Solim UI, spacing methods currently suffer from inconsistent naming and missing axis-based conveniences:
1. Some components and utilities expose abbreviated `pad()` / `padTop()` / `padBottom()` / `padLeft()` / `padRight()` methods alongside or instead of the canonical `padding()` method, creating API ambiguity and inconsistency across the framework.
2. Setting spacing along horizontal (X: left + right) or vertical (Y: top + bottom) axes currently requires verbose 4-parameter calls (e.g. `.padding(0, x, 0, x)` or `.margin(y, 0, y, 0)`), which clutters declarative UI code.

Standardizing entirely on full `padding()` naming and introducing dedicated `paddingX`, `paddingY`, `marginX`, and `marginY` methods will streamline layout declarations, eliminate abbreviated legacy aliases, and make the spacing API consistent and modern.

## What Changes

- **BREAKING**: Remove all abbreviated `pad()`, `padTop()`, `padBottom()`, `padLeft()`, and `padRight()` methods from `ElementModifiers`, `Column`, `Row`, and any other Solim components. All callers must use full `padding()` variants.
- **ADD**: Add `paddingX(float x)` and `paddingY(float y)` to `ElementModifiers`, `Column`, `Row`, `Text`, `SolimImage`, `NetworkImage`, `Container`, and `Card`.
- **ADD**: Add `marginX(float x)`, `marginY(float y)`, `marginX(Readable<Float> x)`, and `marginY(Readable<Float> y)` to `LayoutModifiers` (equipping all layout containers and components implementing `LayoutModifiers`).
- **ADD**: Add `marginX` and `marginY` methods (including static and reactive overloads where appropriate) to `ElementModifiers`, `Column`, `Row`, `Text`, `SolimImage`, `NetworkImage`, and `Button`.
- **MIGRATE**: Update any remaining occurrences of `.pad()` in non-legacy mod code and Solim test suites to `.padding()`.

## Capabilities

### New Capabilities
<!-- No brand new capabilities; this refines existing spacing and modifier specifications -->

### Modified Capabilities
- `solim-shared-modifiers`: Rename all `pad*` methods to `padding*`, and add `paddingX`, `paddingY`, `marginX`, and `marginY` to `ElementModifiers` and core UI components.
- `modifier-clarity`: Enforce canonical `padding` terminology across all Solim components and add `marginX` and `marginY` horizontal/vertical outer cell spacing modifiers to `LayoutModifiers`.

## Impact

- **Public APIs**:
  - `solim.modifier.ElementModifiers`: removed `pad*` static methods; added `paddingX`, `paddingY`, `marginX`, `marginY`.
  - `solim.layout.LayoutModifiers`: added `marginX`, `marginY` (float and `Readable<Float>` overloads).
  - `solim.layout.Column`, `solim.layout.Row`: removed `pad*` methods; added `paddingX`, `paddingY`, `marginX`, `marginY`.
  - `solim.display.Text`, `solim.display.SolimImage`, `solim.display.NetworkImage`: added `paddingX`, `paddingY`, `marginX`, `marginY`.
  - `solim.input.Button`: added `marginX`, `marginY` (float and `Readable<Float>`).
  - `solim.layout.Container`: added `paddingX`, `paddingY`.
- **Mod Code**: Replaced any `.pad(...)` calls in active mod code (e.g. `AuthOverlay.java`) with `.padding(...)`.
- **Backwards Compatibility**: The user explicitly requested to migrate all without backwards compatibility; abbreviated `pad` methods are removed.
