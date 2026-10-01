# bundle-verification Specification

## Purpose
Automated verification task for localization bundles comparing locale bundles against base English bundle, enforcing key completeness, obsolete key rejection, duplicate key prevention, placeholder syntax integrity, and untranslated string monitoring.

## Requirements

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
The verification task SHALL reject and fail the build on any keys whose value in any locale bundle is identical to the base English bundle value, unless the key is explicitly permitted by `assets/bundles/untranslated-allowlist.txt` or is a pure format token. Furthermore, the verification task SHALL enforce an anti-laziness guard that rejects any entry in `untranslated-allowlist.txt` whose English value contains natural language English grammatical words (such as `the`, `is`, `are`, `you`, `your`, `to`, `for`, `with`, `that`, `this`, `when`, `from`, `not`, `will`, `can`, `please`, `failed`, `success`, `error`, etc.).

#### Scenario: Untranslated string fails build
- **WHEN** a locale string has an identical non-empty value to English and is not in `untranslated-allowlist.txt`
- **THEN** the verification task logs an error with the untranslated key name and fails the build.

#### Scenario: Legitimate special term passes
- **WHEN** a key is present in `untranslated-allowlist.txt` (such as `web-feature.wiki.name`) and does not contain English grammatical stop-words
- **THEN** the verification task allows the identical value and verification passes.

#### Scenario: Anti-laziness guard rejects invalid allowlist entry
- **WHEN** an entry in `untranslated-allowlist.txt` corresponds to a base English text containing common English stop-words (e.g. "Select the preferred mode")
- **THEN** the verification task rejects the allowlist entry and fails the build with an anti-laziness error.

