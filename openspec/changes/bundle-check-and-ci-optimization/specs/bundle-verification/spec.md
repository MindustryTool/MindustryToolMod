## ADDED Requirements

### Requirement: Automated Bundle Key Completeness
The verification task SHALL ensure that every locale bundle (`assets/bundles/bundle_<locale>.properties`) contains all keys present in the base bundle (`assets/bundles/bundle.properties`). Any missing key in any locale bundle SHALL cause the verification task to fail.

#### Scenario: All keys present in locale bundle
- **WHEN** `bundle_vi.properties` contains all keys defined in `bundle.properties`
- **THEN** key completeness check passes without error.

#### Scenario: Key missing in locale bundle
- **WHEN** a key exists in `bundle.properties` but is absent from `bundle_ru.properties`
- **THEN** the verification task logs the missing key name and fails the build.

### Requirement: Obsolete Key Rejection
The verification task SHALL detect and reject any keys present in a locale bundle that do not exist in the base bundle (`bundle.properties`).

#### Scenario: Obsolete key detected
- **WHEN** a locale bundle contains a key (e.g. `feature.bridge-visualizer.name`) not found in `bundle.properties`
- **THEN** the verification task logs the obsolete key and fails the build.

### Requirement: Duplicate Key Detection
The verification task SHALL detect any duplicate keys within the same bundle file.

#### Scenario: Duplicate key found in a bundle file
- **WHEN** a bundle file contains two or more definitions of the same key
- **THEN** the verification task logs the duplicate key and line numbers and fails the build.

### Requirement: Placeholder Syntax Consistency
The verification task SHALL verify that placeholders (`{0}`, `{1}`, etc.) in translated values match the count and indices of placeholders in the base bundle string.

#### Scenario: Matching placeholders
- **WHEN** base string has `{0}` and `{1}` and the translated string also has `{0}` and `{1}`
- **THEN** placeholder check passes.

#### Scenario: Placeholder index mismatch
- **WHEN** base string has `{0}` and `{1}` but the translated string only has `{0}`
- **THEN** the verification task reports the placeholder mismatch and fails the build.

### Requirement: Untranslated Key Monitoring
The verification task SHALL monitor and report keys whose value in a locale bundle is identical to the base English bundle value, ignoring format specifiers, numbers, and allowed brand names.

#### Scenario: Untranslated string flagged as warning
- **WHEN** a locale string has an identical non-empty value to English that is not in the allowlist
- **THEN** the verification task logs a warning with the untranslated key count and details without failing the build.
