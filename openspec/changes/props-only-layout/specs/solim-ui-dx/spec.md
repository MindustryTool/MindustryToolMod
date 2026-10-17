## ADDED Requirements

### Requirement: Layout and alignment declared exclusively via props
Containers (`Column`, `Row`, `Card`, `Wrap`, `Grid`) SHALL declare gap, alignment, and spacing exclusively through `.props(...)`. Chained layout methods on container classes (`.gap(...)`, `.top()`, `.bottom()`, `.left()`, `.right()`, `.center()`, `.align(...)`) SHALL be removed.

#### Scenario: Gap declared in props
- **WHEN** configuring a container with item spacing
- **THEN** the spacing is declared as a prop argument `.props(gap(unit(2)))` instead of a chained `.gap(...)` method

#### Scenario: Alignment declared in props
- **WHEN** configuring a container or element with alignment
- **THEN** alignment is declared as a prop argument `.props(center(), top())` or `.props(align(...))` instead of chained alignment methods

### Requirement: Layout props re-exported via UI facade
The public facade `solim.UI` SHALL re-export standard layout and alignment property factories (`gap`, `center`, `top`, `bottom`, `left`, `right`, `align`, `grow`, `growX`, `growY`, `padding`, `paddingX`, `paddingY`) so that client code can declare declarative props without separate static imports from `solim.modifier.Values`.

#### Scenario: Facade static import
- **WHEN** client code imports `static solim.UI.*`
- **THEN** layout property factories such as `gap(...)`, `center()`, `top()`, `left()` are directly in scope for use inside `.props(...)`
