## ADDED Requirements

### Requirement: Bundle Key Character Set
The verification task SHALL require every key defined in the base bundle (`assets/bundles/bundle.properties`) to match the pattern `^[a-z0-9]+([.-][a-z0-9]+)*$`, where `.` and `-` separate and join lowercase alphanumeric segments. Matching SHALL be case-sensitive: a key containing any uppercase letter SHALL be a violation.

#### Scenario: Lowercase dot-separated key accepted
- **WHEN** a base bundle key consists of lowercase alphanumeric segments separated by dots, such as `feature.chat.settings.enable`
- **THEN** the verification task reports no key format error for that key

#### Scenario: Hyphenated segment accepted
- **WHEN** a base bundle key contains a hyphen inside a segment, such as `feature.translation.gemini.api-key`
- **THEN** the verification task reports no key format error for that key

#### Scenario: Digit inside segment accepted
- **WHEN** a base bundle key contains a digit inside a segment, such as `feature.player-connect.step1-title`
- **THEN** the verification task reports no key format error for that key

#### Scenario: Uppercase letter rejected
- **WHEN** a base bundle key contains an uppercase letter, such as `keybind.freeCameraToggle.name`
- **THEN** the verification task logs the offending key and fails the build

#### Scenario: Underscore rejected
- **WHEN** a base bundle key contains an underscore, such as `feature.map-rules.save_error`
- **THEN** the verification task logs the offending key and fails the build

#### Scenario: Case sensitivity is not relaxed
- **WHEN** the key format match is evaluated
- **THEN** it SHALL be evaluated case-sensitively, so that a key differing only by letter case is rejected rather than accepted

### Requirement: Unconditional Key Format Enforcement
The key format rule SHALL apply to every key in the base bundle and SHALL admit no exemption. No allowlist, ignore file, or inline marker SHALL exempt a key from the character set requirement. Because the existing locale completeness and obsolete key rules guarantee every locale bundle carries exactly the base key set, satisfying the base bundle SHALL be sufficient for the format rule across all bundles.

#### Scenario: No exemption list consulted
- **WHEN** a key violates the character set and is listed in `untranslated-allowlist.txt`
- **THEN** the verification task still fails the build with a key format error

#### Scenario: Locale bundles inherit the constraint
- **WHEN** every key in the base bundle conforms to the character set
- **THEN** all locale bundles conform as well, without a separate format check per locale file