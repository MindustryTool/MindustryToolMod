# donation Specification

## Purpose

Provides a polite donation reminder dialog after the user finishes interacting with feature settings, governed by a 3-day cooldown interval and opt-out preference, as well as a permanent donation shortcut in the feature settings toolbar.

## Requirements

### Requirement: Donation dialog prompt on settings exit
The system SHALL evaluate whether to show the donation reminder dialog when the user closes `FeatureSettingDialog`.

#### Scenario: Eligible prompt on settings close
- **WHEN** user closes `FeatureSettingDialog`, "Don't show this again" is false, and at least 3 days (72 hours) have elapsed since the last prompt (or it has never been prompted)
- **THEN** the system displays the `DonationDialog` and records the current timestamp as the last prompted time

#### Scenario: Cooldown active
- **WHEN** user closes `FeatureSettingDialog` and less than 3 days have elapsed since the last prompt
- **THEN** the system does not show the `DonationDialog`

#### Scenario: User opted out
- **WHEN** user closes `FeatureSettingDialog` and "Don't show this again" is set to true
- **THEN** the system does not show the `DonationDialog`

### Requirement: Donation dialog layout and controls
The `DonationDialog` SHALL display a heart icon, informative description, an unchecked "Don't show this again" checkbox, a "Donate" action button, and a dismiss button.

#### Scenario: User clicks Donate button
- **WHEN** user clicks the "Donate" button in `DonationDialog`
- **THEN** the system opens `https://mindustry-tool.com/en/donate` in the external browser and closes the dialog

#### Scenario: User checks Don't show this again
- **WHEN** user toggles the "Don't show this again" checkbox and dismisses or interacts with the dialog
- **THEN** the system persists `mindustrytool.donation.dont-show-again` as true

#### Scenario: User dismisses dialog
- **WHEN** user clicks the close or "Maybe Later" button
- **THEN** the dialog closes and the state is preserved

### Requirement: Permanent donation button in settings view
The system SHALL provide a permanent "Donate" button in the `FeatureSettingsView` toolbar beside the "Create mod suggestion" button.

#### Scenario: User clicks permanent donate button
- **WHEN** user clicks the "Donate" button in `FeatureSettingsView` toolbar
- **THEN** the system directly opens `https://mindustry-tool.com/en/donate` in the external browser
