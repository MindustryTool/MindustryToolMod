## ADDED Requirements

### Requirement: Composer send gating on validity
The chat composer send button SHALL be enabled only while no send is in flight and the message input is valid per its configured validator. The imperative validity guard in the send handler SHALL be retained as defense in depth.

#### Scenario: Send disabled on invalid input
- **WHEN** the composer input is empty or fails validation while no send is in flight
- **THEN** the send button is disabled

#### Scenario: Send disabled while sending
- **WHEN** a send request is in flight
- **THEN** the send button is disabled regardless of input validity

#### Scenario: Send enabled on valid idle input
- **WHEN** no send is in flight and the input passes validation
- **THEN** the send button is enabled
