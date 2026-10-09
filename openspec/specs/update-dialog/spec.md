# update-dialog Specification

## Purpose
Specifies update changelog presentation, target release isolation, Solim card layout, and separator partitioning for `UpdateDialog`.
## Requirements
### Requirement: Target Release Isolation
`ChangelogFormatter` SHALL parse GitHub releases and isolate the release entry matching the target update version from the remaining releases.

#### Scenario: Target release appears below recent beta release
- **WHEN** GitHub releases contain a newer beta release at index 0 and the target stable release at index 1
- **THEN** `ChangelogFormatter` SHALL identify index 1 as the target release and partition the beta and older releases into other releases

#### Scenario: Target release matches exact tag
- **WHEN** the target version matches a release `tag_name` exactly
- **THEN** `ChangelogFormatter` SHALL select that release as the target release

#### Scenario: Target release matches parsed version
- **WHEN** the target version is formatted without a `v` prefix (e.g. `5.2.9`) and a release tag is `v5.2.9-v8`
- **THEN** `ChangelogFormatter` SHALL match the release based on parsed version components

#### Scenario: Target release not present in GitHub releases
- **WHEN** none of the GitHub releases match the target version
- **THEN** `ChangelogFormatter` SHALL set `targetRelease` to null and retain all releases in `otherReleases`

### Requirement: Solim Target Card and Separator Presentation
`UpdateDialog` SHALL render the target release in a dedicated Solim card container with a green dot indicator, followed by a native Solim divider and other releases when present.

#### Scenario: Dialog displays target release card at top of scroll
- **WHEN** an update dialog is shown with a matched target release
- **THEN** the target release notes SHALL be rendered inside a dedicated Solim card container at the top of the changelog scroll view
- **AND** the target release header SHALL include a subtle green indicator `[green]● [accent]{tagName}[white]`

#### Scenario: Divider and clean header between target and other releases
- **WHEN** both a target release and one or more other releases exist
- **THEN** a native Solim `divider()` SHALL be displayed below the target card
- **AND** a clean localized header `update.changelog.other-releases` SHALL precede the remaining releases

#### Scenario: Single release does not show redundant divider
- **WHEN** the target release is the only release available in the payload
- **THEN** the target card SHALL be displayed without an "Other Releases" divider or header

#### Scenario: Fallback display when target is unmatched
- **WHEN** no target release was matched from GitHub releases
- **THEN** all available releases SHALL be displayed in standard changelog format without an empty target card or separator header

