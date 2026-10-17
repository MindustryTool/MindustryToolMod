## REMOVED Requirements

### Requirement: Chained Reactive Property Bindings on Card

**Reason**: A card's configuration (background, border, rounding, spacing, sizes) is configuration, not content or behaviour, so it is declared through `.props(...)` like every other host. Retaining chained reactive bindings on `Card` would be the one host with a second configuration path, which is exactly the inconsistency this change removes.

**Migration**: Replace a chained card binding such as `card().background(bg).border(1f, color)` with `.props(background(bg), border(1f, color))`; reactive values use the same property factories with a `Readable` argument and remain owned by and disposed with the card.
