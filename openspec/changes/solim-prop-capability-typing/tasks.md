## 1. Marker types

- [ ] 1.1 Add `HostCapability`, `ElementCapability extends HostCapability`, `TableCapability extends ElementCapability`, `CellCapability extends HostCapability`, `ContainerCapability extends HostCapability`, `AlignmentCapability` in `solim.modifier`
- [ ] 1.2 Implement `HostCapability` on the `Component` base (`solim-api`) so every host inherits the root marker
- [ ] 1.3 Declare the capability set on every host's `implements` clause; table-backed containers get `TableCapability` + `CellCapability` + `ContainerCapability`, `SolimStack` gets `ElementCapability` + `ContainerCapability` only, leaves get `ElementCapability` + `CellCapability` as applicable

## 2. Factories declare their real requirement

- [ ] 2.1 Layout/table factories (`gap`, `padding*`, `rounded`, `border`, `background*`, `top`/`bottom`/`left`/`right`/`center`/`align`) require `TableCapability` (alignment requires the alignment capability, see 4)
- [ ] 2.2 Element factories (`color`, `width`/`height`/`size`, `opacity`, `alpha`, `visible`, `tooltip`, `name`) require `ElementCapability`
- [ ] 2.3 Cell factories (`prefWidth`/`prefHeight`/`min*`/`max*`, `grow*`, `margin*`) require `CellCapability`
- [ ] 2.4 Child/structural factories (`children`) require `ContainerCapability`
- [ ] 2.5 `raw(...)` takes an explicit capability so an escape hatch states the host it targets

## 3. children() relocation

- [ ] 3.1 Declare `SELF children(@Nullable Runnable block)` on `ContainerCapability<SELF>`
- [ ] 3.2 Delete `children` from `Configurable`
- [ ] 3.3 Delete `children` from the leaves (`Text`, `SolimImage`, `Badge`, `Divider`, `Spacer`, `SolimTextField`, `SolimSlider`, `Switch`, `Checkbox`, `NetworkImage`, `SolimVideo`)
- [ ] 3.4 Delete `Props.children`
- [ ] 3.5 Confirm every container keeps its current `children` body and fluent return

## 4. Polymorphic alignment

- [ ] 4.1 Add `AlignmentCapability` with a method that applies a composed alignment mask to the host's parent cell and, for containers, its inner table defaults
- [ ] 4.2 Implement it on `Column`, `Row`, `Card`, `Grid`, `Wrap`, `Tabs`, `Scroll`, `SolimCollapser`, `Popup`, and the reactive containers
- [ ] 4.3 Delete the `instanceof` ladder in `Values.alignCoordinated` and dispatch through the capability

## 5. Call-site fallout

- [ ] 5.1 Build `solim-core`; remove every leaf `children(...)` call the compiler rejects
- [ ] 5.2 Build `mod`; fix every element-only-table-prop call site the compiler rejects (`gap`/`padding`/`rounded`/`border`/`background` on a non-table host), using the equivalent element prop (`margin`, `color`) where one exists
- [ ] 5.3 Confirm no call site regresses to `table() != null` as a capability test

## 6. Tests

- [ ] 6.1 Add a test asserting each host declares the expected capability set (the `SolimStack`, `SolimImage`, and `Text` distinctions)
- [ ] 6.2 Add a test proving a table-kind property on a `TableCapability` host still applies, and that `table()` being null no longer implies "no table capability" (`SolimStack` hosts children)
- [ ] 6.3 Add a test proving `children(...)` on a container still attaches, and that a leaf no longer exposes it (compile-time, via a source check or by construction)
- [ ] 6.4 Add a test proving alignment dispatch through the capability produces the same composed mask as the removed ladder
- [ ] 6.5 Run the full `solim-core` suite
