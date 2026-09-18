## ADDED Requirements

### Requirement: NetworkImage Layout Methods and Parent Cell Constraints
NetworkImage SHALL implement CellConfig<NetworkImage> and ElementConfig<NetworkImage> for declarative layout geometry and parent cell constraints. Sizing methods (size(float, float), size(float), width(float), height(float), and reactive variants) SHALL set both element dimensions and preferred cell dimensions. Parent cell constraint modifiers (minWidth, maxWidth, minHeight, maxHeight, growX, growY, grow) SHALL be buffered in PendingCellConfig and automatically bound to the parent Cell when attached via ParentStack or applied immediately if already attached. NetworkImage SHALL NOT provide inner padding() or gap() methods and SHALL NOT implement SpacingAware.

#### Scenario: Element dimension configuration
- **WHEN** networkImage(url).size(64f, 48f) is configured
- **THEN** the underlying Arc Image width is 64f and height is 48f, and the preferred width and height in cellConfig() are 64f and 48f.

#### Scenario: Reactive width and height
- **WHEN** networkImage(url).width(widthSignal).height(heightSignal) is declared and the signals emit new values
- **THEN** the underlying Arc Image width and height update to match the emitted values, and updates cease after dispose().

#### Scenario: Parent cell grow and bounds constraints
- **WHEN** networkImage(url).minWidth(20f).maxWidth(100f).growX() is added inside a parent Table
- **THEN** the parent cell's minWidth is 20f, maxWidth is 100f, and expandX and fillX are enabled.

### Requirement: NetworkImage Declarative Alignment
NetworkImage SHALL support declarative alignment via .top(), .left(), and .center(). Calling these methods before or after attachment to a parent Table SHALL configure the parent cell's alignment to the respective direction.

#### Scenario: Alignment configured before parent attachment
- **WHEN** networkImage(url).top().left() is declared before the image is added to a parent Table
- **THEN** upon attachment to the Table, the resulting Cell has top-left alignment configured.

#### Scenario: Alignment configured after parent attachment
- **WHEN** top() or center() is called on a NetworkImage already attached to a Table
- **THEN** the parent cell's alignment is updated immediately.

### Requirement: NetworkImage Outer Margin via CellConfig
NetworkImage SHALL delegate all outer spacing to CellConfig margins. Calling margin(float), margin(float, float, float, float), marginTop, marginBottom, marginLeft, marginRight, marginX, marginY, or their reactive overloads SHALL configure the parent cell's padding through PendingCellConfig.

#### Scenario: Static margin configuration in Table
- **WHEN** networkImage(url).margin(10f) is attached inside a parent Table
- **THEN** the parent cell's padTop, padLeft, padBottom, and padRight are each equal to 10f.

#### Scenario: Directional margin configuration in Table
- **WHEN** networkImage(url).marginTop(4f).marginLeft(8f).marginBottom(12f).marginRight(16f) is attached inside a parent Table
- **THEN** the parent cell's padding values reflect 4f top, 8f left, 12f bottom, and 16f right.