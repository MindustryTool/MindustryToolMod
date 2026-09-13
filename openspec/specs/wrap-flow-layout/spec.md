# wrap-flow-layout Specification

## Purpose
Auto-wrapping flow container wrapping Arc.scene.ui.layout.WrapTable with 2D gap spacing, default container expansion, and Solim alignment support.

## Requirements
### Requirement: Automatic Line Wrapping
The system SHALL wrap child elements in a Wrap container onto new rows whenever the accumulated cell widths on the current row exceed the container's available width.

#### Scenario: Children wrap when container width exceeded
- **WHEN** multiple buttons are added inside a wrap() container whose combined width exceeds the container width
- **THEN** children that do not fit on the current line are laid out on subsequent rows below

### Requirement: 2D Gap Spacing
The system SHALL support uniform horizontal and vertical spacing between wrapped elements without leaving leading indentations on wrapped rows.

#### Scenario: Uniform gap on wrapped items
- **WHEN** developer calls .gap(unit(1)) on a wrap() container
- **THEN** adjacent items on the same row have horizontal gap and rows have vertical gap spacing

### Requirement: Container Alignment Support
The system SHALL support configuring child flow alignment via .left(), .center(), and .right() methods on Wrap.

#### Scenario: Align wrap flow to left
- **WHEN** developer calls wrap().left()
- **THEN** the child items align to the left side of the container
