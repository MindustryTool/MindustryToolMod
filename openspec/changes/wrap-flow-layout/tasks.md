## 1. Core Wrap Component Rework

- [ ] 1.1 Update Wrap to attach to parent layouts with default growX() so WrapTable receives the container's full available width
- [ ] 1.2 Implement 2D gap spacing in Wrap.respace() to apply horizontal gap and vertical line gap cleanly
- [ ] 1.3 Add alignment methods (left(), center(), ight(), 	op(), ottom(), justify(Justify)) to Wrap that configure WrapTable.align
- [ ] 1.4 Add unit tests in solim-core verifying multi-row wrapping, gap spacing, and alignment on Wrap

## 2. Integration & Verification

- [ ] 2.1 Verify BrowserFilterDialog chips and categories wrap cleanly onto multiple rows
- [ ] 2.2 Verify project builds and all tests pass with gradlew :solim-core:test :mod:test
