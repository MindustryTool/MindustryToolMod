## Why

The MindustryToolMod codebase has grown significantly (100+ Java files) without systematic code review. Features were added incrementally, creating risk of bugs, security issues, performance bottlenecks, dead code, and architectural drift. A comprehensive audit is needed to catalog all features, document the codebase structure, and identify quality issues before they cause problems.

## What Changes

- Create `docs/FEATURE.md` listing all features with descriptions and file mappings
- For each feature group, create `docs/<feature>.md` with:
  - Full source code audit findings
  - Security issues, data loss risks, concurrency bugs
  - Anti-patterns, dead code, tech debt
  - Performance bottlenecks, architectural violations
  - Specific file paths + line references for each finding
- Cover all feature groups: auth, autoplay, background, browser (map/schematic), chat (global/pretty/translation), display (healthbar/item-visualizer/pathfinding/progress/quickaccess/range/teamresource/togglerendering/wavepreview), godmode, music, playerconnect, savesync, settings, smartdrill, smartupgrade, time, plus core infrastructure (services, dto, ui, utils, config)
- Prioritize: security > data loss > concurrency > anti-patterns > performance > architecture

## Capabilities

### New Capabilities
- `code-audit`: Comprehensive codebase audit with per-feature findings documents

### Modified Capabilities
<!-- No existing specs to modify -->

## Impact

- New `docs/` directory with audit documentation
- No code changes (audit-only)
- Findings may drive future refactoring changes
