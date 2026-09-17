## ADDED Requirements

### Requirement: Emoji feature icon asset

The mod SHALL provide a Lucide `smile.png` icon asset in `assets/icons/` for the Emoji feature, and the Emoji feature metadata SHALL reference it via `FileIcon.of("smile.png")`.

#### Scenario: Emoji icon asset availability

- **WHEN** the mod is initialized or the Emoji feature queries its icon asset
- **THEN** `assets/icons/smile.png` exists as a 24x24 white RGBA PNG.

#### Scenario: Emoji metadata loads file icon

- **WHEN** `Feature.getMetadata().getIcon()` is called on the Emoji feature
- **THEN** it returns the `smile.png` drawable, falling back gracefully without throwing when the asset cannot load.
