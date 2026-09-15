## ADDED Requirements

### Requirement: Self-contained models under mindustrytool.models

All DTOs used by `MindustryTool` and `Github` SHALL be copied from `src/old` into `src/mindustrytool/models` with package `mindustrytool.models` and MUST NOT import `old.*`.

#### Scenario: Models exist in new package
- **WHEN** `src/mindustrytool/models` is listed
- **THEN** it contains `MapData`, `MapDetailData`, `SchematicData`, `SchematicDetailData`, `TagCategory`, `TagData`, `UserData`, `ModData`, `ServerData`, `PlayerConnectRoom`, `PlayerConnectProvider`, `ChannelDto`, `ChatMessage`, `ChatUser`, `UserSession`, `Sort`, and any other DTOs required by retained endpoints, each with `package mindustrytool.models`

#### Scenario: No old imports in new code
- **WHEN** `grep -R "old\\.mindustrytool" src/mindustrytool` is run
- **THEN** it returns no results

### Requirement: DTO fidelity and Jackson compatibility

Copied DTOs SHALL preserve field names and Jackson annotations to parse actual API JSON (`FAIL_ON_UNKNOWN_PROPERTIES=false`).

#### Scenario: DTO parses sample API JSON
- **WHEN** sample JSON for `MapData` from `GET /maps/{id}` is deserialized via `JsonUtils.fromJson(MapData.class, json)`
- **THEN** all required fields populate and unknown fields are ignored without exception

#### Scenario: Inner ServerDto extracted
- **WHEN** `ServerService` inner `ServerDto` is inspected
- **THEN** a top-level `mindustrytool.models.ServerData` exists with `id`, `name`, `address`, `port`, `status` and Lombok `@Data` or equivalent

### Requirement: Copied JSON utility in new codebase

A new `mindustrytool.utils.JsonUtils` SHALL be copied from `old.mindustrytool.Utils` JSON methods (Jackson `ObjectMapper` with `JavaTimeModule`, `toJson`, `fromJson`, `fromJsonArray`) and expose static helpers for the new services.

#### Scenario: JsonUtils handles DTO parsing
- **WHEN** `JsonUtils.fromJson(MapDetailData.class, json)` is called
- **THEN** it returns a deserialized instance using a shared `ObjectMapper` configured with `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES=false` and `JavaTimeModule`

#### Scenario: Json array parsing for lists
- **WHEN** `JsonUtils.fromJsonArray(TagData.class, jsonArray)` is called
- **THEN** it returns `List<TagData>` via `readerForListOf`

#### Scenario: No old.Utils import in services
- **WHEN** `MindustryTool.java`, `Github.java`, and `Request.java` are inspected
- **THEN** they import `mindustrytool.utils.JsonUtils` and not `old.mindustrytool.Utils`
