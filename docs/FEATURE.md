# MindustryToolMod Feature Inventory

> Total: ~150 Java files across 23 feature groups + core infrastructure

## Core Infrastructure

| Group | Files | Description |
|-------|-------|-------------|
| **Config** | `src/mindustrytool/Config.java` | Environment URLs, API endpoints, GitHub/repo config |
| **Main** | `src/mindustrytool/Main.java` | Mod entry point, feature registration, packet replacement, dir setup |
| **Utils** | `src/mindustrytool/Utils.java` | JSON parsing, markdown rendering, I/O helpers |
| **IconUtils** | `src/mindustrytool/IconUtils.java` | Icon mapping utilities |
| **MdtInitEvent** | `src/mindustrytool/MdtInitEvent.java` | Init event fired after feature setup |
| **MdtKeybinds** | `src/mindustrytool/MdtKeybinds.java` | Key binding registration |
| **Feature** | `features/Feature.java` | Feature lifecycle interface (init, onEnable, onDisable) |
| **FeatureManager** | `features/FeatureManager.java` | Singleton registry: init/enable/disable/persist feature states |
| **FeatureMetadata** | `features/FeatureMetadata.java` | Builder for feature metadata (name, icon, order) |
| **WebFeature** | `features/WebFeature.java` | Static list of web tool links |

---

## Auth

| File | Description |
|------|-------------|
| `features/auth/AuthHttp.java` | HTTP client for auth API calls (login, refresh, logout) |
| `features/auth/AuthLoginDialog.java` | Login dialog UI |
| `features/auth/AuthService.java` | Singleton: session management, token storage, auth state |
| `features/auth/dto/LoginEvent.java` | Event fired on login |
| `features/auth/dto/LogoutEvent.java` | Event fired on logout |
| `features/auth/dto/SessionLoadEvent.java` | Event fired when session loaded from disk |
| `features/auth/dto/UserSession.java` | Session data model (tokens, user info) |

---

## Autoplay

| File | Description |
|------|-------------|
| `features/autoplay/AutoplayFeature.java` | Feature entry: registers all AI tasks |
| `features/autoplay/AutoplaySettingDialog.java` | Settings UI for autoplay config |
| `features/autoplay/tasks/AutoplayTask.java` | Abstract base for all autoplay tasks |
| `features/autoplay/tasks/BaseAutoplayAI.java` | Shared AI logic (pathfinding, targeting) |
| `features/autoplay/tasks/AttackTask.java` | Auto-attack enemy units/blocks |
| `features/autoplay/tasks/MiningTask.java` | Auto-mining of ores |
| `features/autoplay/tasks/RepairTask.java` | Auto-repair damaged buildings |
| `features/autoplay/tasks/SelfBuildTask.java` | Auto-build structures from schematics |
| `features/autoplay/tasks/FleeTask.java` | Auto-flee from danger |
| `features/autoplay/tasks/RebuildTask.java` | Auto-rebuild destroyed structures |
| `features/autoplay/tasks/SelfHealTask.java` | Auto-heal when damaged |
| `features/autoplay/tasks/FollowAssistTask.java` | Follow & assist teammate units |

---

## Background

| File | Description |
|------|-------------|
| `features/background/BackgroundFeature.java` | Custom background image display |
| `features/background/BackgroundSettingsDialog.java` | Background settings UI |

---

## Browser: Map

| File | Description |
|------|-------------|
| `features/browser/FilterDialog.java` | Shared filter/search dialog |
| `features/browser/SearchConfig.java` | Shared search configuration |
| `features/browser/map/MapBrowserFeature.java` | Map browser feature entry |
| `features/browser/map/MapDialog.java` | Map list dialog |
| `features/browser/map/MapImage.java` | Network-loaded map preview image |
| `features/browser/map/MapInfoDialog.java` | Map detail/info dialog |

---

## Browser: Schematic

| File | Description |
|------|-------------|
| `features/browser/schematic/SchematicBrowserFeature.java` | Schematic browser feature entry |
| `features/browser/schematic/SchematicDialog.java` | Schematic list dialog |
| `features/browser/schematic/SchematicImage.java` | Network-loaded schematic preview image |
| `features/browser/schematic/SchematicInfoDialog.java` | Schematic detail/info dialog |

---

## Chat: Global

| File | Description |
|------|-------------|
| `features/chat/global/ChatFeature.java` | Global chat feature entry |
| `features/chat/global/ChatService.java` | Core chat service (send, receive, connect) |
| `features/chat/global/ChatApiClient.java` | REST API client for chat |
| `features/chat/global/ChatStreamClient.java` | SSE stream client for real-time messages |
| `features/chat/global/ChatStateManager.java` | Chat connection lifecycle state |
| `features/chat/global/ChatConfig.java` | Chat configuration |
| `features/chat/global/ChatStore.java` | In-memory message/channel store |
| `features/chat/global/ContentType.java` | Enum for chat content types |
| `features/chat/global/AttachContentDialog.java` | Attachment/dialog content picker |
| `features/chat/global/ChatSettingsDialog.java` | Chat settings UI |
| `features/chat/global/dto/ChannelDto.java` | Channel DTO |
| `features/chat/global/dto/ChatMessage.java` | Message DTO |
| `features/chat/global/dto/ChatUser.java` | User DTO |
| `features/chat/global/dto/LastReadMessageStore.java` | Last-read message persistence |
| `features/chat/global/events/ChatMessageReceive.java` | Event: message received |
| `features/chat/global/events/ChatStateChange.java` | Event: connection state changed |
| `features/chat/global/events/MessagesUpdateEvent.java` | Event: messages list updated |
| `features/chat/global/events/UsersUpdateEvent.java` | Event: users list updated |
| `features/chat/global/ui/ChannelList.java` | Channel list UI component |
| `features/chat/global/ui/ChatInput.java` | Chat input UI component |
| `features/chat/global/ui/ChatOverlay.java` | Chat overlay HUD component |
| `features/chat/global/ui/MessageList.java` | Message list UI component |
| `features/chat/global/ui/UserList.java` | User list UI component |

---

## Chat: Pretty

| File | Description |
|------|-------------|
| `features/chat/pretty/PrettyChatFeature.java` | Pretty chat feature entry (currently commented out) |
| `features/chat/pretty/PrettyChatConfig.java` | Pretty chat configuration |
| `features/chat/pretty/PrettyChatSettingsDialog.java` | Pretty chat settings UI |

---

## Chat: Translation

| File | Description |
|------|-------------|
| `features/chat/translation/ChatTranslationFeature.java` | Chat translation feature entry |
| `features/chat/translation/ChatTranslationConfig.java` | Translation configuration |
| `features/chat/translation/ChatTranslationSettingsDialog.java` | Translation settings UI |
| `features/chat/translation/TranslationProvider.java` | Interface for translation providers |
| `features/chat/translation/DeepLTranslationProvider.java` | DeepL translation provider |
| `features/chat/translation/GeminiTranslationProvider.java` | Google Gemini translation provider |
| `features/chat/translation/MindustryToolTranslationProvider.java` | Built-in translation provider |

---

## Display: Health Bar

| File | Description |
|------|-------------|
| `features/display/healthbar/HealthBarVisualizer.java` | Health bar rendering over units/blocks |
| `features/display/healthbar/HealthBarConfig.java` | Health bar configuration |
| `features/display/healthbar/HealthBarSettingsDialog.java` | Health bar settings UI |

---

## Display: Item Visualizer

| File | Description |
|------|-------------|
| `features/display/itemvisualizer/ItemVisualizerFeature.java` | Item visualization feature (currently commented out) |
| `features/display/itemvisualizer/ItemVisualizerSettings.java` | Item visualizer configuration |
| `features/display/itemvisualizer/ItemVisualizerSettingsDialog.java` | Item visualizer settings UI |

---

## Display: Pathfinding

| File | Description |
|------|-------------|
| `features/display/pathfinding/PathfindingDisplay.java` | Pathfinding overlay rendering |
| `features/display/pathfinding/PathfindingConfig.java` | Pathfinding configuration |
| `features/display/pathfinding/PathfindingSettingsDialog.java` | Pathfinding settings UI |
| `features/display/pathfinding/PathfindingCache.java` | Path cache data model |
| `features/display/pathfinding/PathfindingCacheManager.java` | Manager for path cache |

---

## Display: Progress

| File | Description |
|------|-------------|
| `features/display/progress/ProgressDisplay.java` | Building progress overlay |
| `features/display/progress/ProgressConfig.java` | Progress display configuration |
| `features/display/progress/ProgressDisplaySettingsDialog.java` | Progress display settings UI |

---

## Display: Quick Access

| File | Description |
|------|-------------|
| `features/display/quickaccess/QuickAccessFeature.java` | HUD toolbar for quick feature access |
| `features/display/quickaccess/QuickAccessConfig.java` | Quick access configuration |
| `features/display/quickaccess/QuickAccessSettingsDialog.java` | Quick access settings UI |

---

## Display: Range

| File | Description |
|------|-------------|
| `features/display/range/RangeDisplay.java` | Tower/unit range visualization |
| `features/display/range/RangeDisplayConfig.java` | Range display configuration |
| `features/display/range/RangeDisplaySettingsDialog.java` | Range display settings UI |

---

## Display: Team Resource

| File | Description |
|------|-------------|
| `features/display/teamresource/TeamResourceFeature.java` | Team resource overview with flow rates |
| `features/display/teamresource/TeamResourceConfig.java` | Team resource configuration |
| `features/display/teamresource/TeamResourceAllTeamsDialog.java` | All-teams resource dialog |
| `features/display/teamresource/SplitBar.java` | Split bar rendering component |

---

## Display: Toggle Rendering

| File | Description |
|------|-------------|
| `features/display/togglerendering/ToggleRenderingFeature.java` | Toggle rendering of game elements |
| `features/display/togglerendering/ToggleRenderingConfig.java` | Toggle rendering configuration |
| `features/display/togglerendering/ToggleRenderingSettingsDialog.java` | Toggle rendering settings UI |

---

## Display: Wave Preview

| File | Description |
|------|-------------|
| `features/display/wavepreview/WavePreviewFeature.java` | Incoming wave composition preview |
| `features/display/wavepreview/WavePreviewConfig.java` | Wave preview configuration |

---

## God Mode

| File | Description |
|------|-------------|
| `features/godmode/GodModeFeature.java` | Debug/cheat tools feature entry |
| `features/godmode/GodModeProvider.java` | Interface for god mode providers |
| `features/godmode/InternalGodModeProvider.java` | Singleplayer/host provider (direct API) |
| `features/godmode/JSGodModeProvider.java` | Remote server provider (via /js commands) |
| `features/godmode/GodModeDialogs.java` | God mode main dialog container |
| `features/godmode/GodModeSettingsDialog.java` | God mode settings |
| `features/godmode/GodModeUnitSelectionDialog.java` | Unit selection dialog |
| `features/godmode/GodModeUnitConfigDialog.java` | Unit configuration dialog |
| `features/godmode/GodModeItemConfigDialog.java` | Item configuration dialog |
| `features/godmode/GodModeItemSelectionDialog.java` | Item selection dialog |
| `features/godmode/GodModeEffectDialog.java` | Effect selection/play dialog |
| `features/godmode/GodModeTeamSelectionDialog.java` | Team selection dialog |
| `features/godmode/GodModePlayerSelectionDialog.java` | Player selection dialog |
| `features/godmode/GodModeCoreConfigDialog.java` | Core configuration dialog |
| `features/godmode/GodModeCoreSelectionDialog.java` | Core selection dialog |
| `features/godmode/PositionSelector.java` | Position selection map overlay |
| `features/godmode/PositionSelectionDialog.java` | Position selection dialog |

---

## Music

| File | Description |
|------|-------------|
| `features/music/MusicFeature.java` | Custom music import/management |
| `features/music/MusicConfig.java` | Music configuration |
| `features/music/MusicSettingsDialog.java` | Music settings UI |
| `features/music/MusicType.java` | Music type enum |
| `features/music/dto/MusicRegisterEvent.java` | Event: music registered |

---

## Player Connect

| File | Description |
|------|-------------|
| `features/playerconnect/PlayerConnectFeature.java` | Network proxy/hosting feature entry |
| `features/playerconnect/PlayerConnect.java` | Core player connect logic |
| `features/playerconnect/NetworkProxy.java` | Network proxy server/client |
| `features/playerconnect/PlayerConnectConfig.java` | Player connect configuration |
| `features/playerconnect/PlayerConnectProvider.java` | Provider data model |
| `features/playerconnect/PlayerConnectProviders.java` | Provider listing |
| `features/playerconnect/PlayerConnectRoom.java` | Room data model |
| `features/playerconnect/PlayerConnectRoomConnected.java` | Connected room model |
| `features/playerconnect/PlayerConnectLink.java` | Link/URL generation |
| `features/playerconnect/PlayerConnectRenderer.java` | Room list rendering |
| `features/playerconnect/PlayerConnectJoinInjector.java` | Join dialog injection |
| `features/playerconnect/PlayerConnectJoinWarningDialog.java` | Join warning dialog |
| `features/playerconnect/PlayerConnectPasswordDialog.java` | Password entry dialog |
| `features/playerconnect/PlayerConnectSettingDialog.java` | Settings UI |
| `features/playerconnect/CreateRoomDialog.java` | Create room dialog |
| `features/playerconnect/JoinRoomDialog.java` | Join room dialog |
| `features/playerconnect/Packets.java` | Packet definitions (Ping, Handshake, etc.) |
| `features/playerconnect/NoopRatekeeper.java` | No-op rate limiter |
| `features/playerconnect/RoomCreatedEvent.java` | Event: room created |

---

## Save Sync

| File | Description |
|------|-------------|
| `features/savesync/SaveSyncFeature.java` | Save sync feature entry |
| `features/savesync/SyncService.java` | Core sync logic |
| `features/savesync/FileService.java` | File I/O for save data |
| `features/savesync/StorageService.java` | Cloud storage operations |
| `features/savesync/SaveSyncFileChanges.java` | File change detection |
| `features/savesync/SaveSyncCreateSlotDialog.java` | Create storage slot dialog |
| `features/savesync/SaveSyncSlotSelectionDialog.java` | Slot selection dialog |
| `features/savesync/SaveSyncSettingsDialog.java` | Settings UI |
| `features/savesync/SaveSyncProgressDialog.java` | Progress dialog |
| `features/savesync/dto/CheckHashesDto.java` | DTO: check hashes request |
| `features/savesync/dto/CheckHashesResponseDto.java` | DTO: check hashes response |
| `features/savesync/dto/ClientFileDto.java` | DTO: client file metadata |
| `features/savesync/dto/CreateStorageSlotDto.java` | DTO: create slot request |
| `features/savesync/dto/DownloadDto.java` | DTO: download request |
| `features/savesync/dto/StorageSlotDto.java` | DTO: storage slot |
| `features/savesync/dto/SyncSlotDto.java` | DTO: sync slot |
| `features/savesync/dto/SyncSlotResponseDto.java` | DTO: sync slot response |

---

## Settings

| File | Description |
|------|-------------|
| `features/settings/FeatureSettingDialog.java` | Central settings dialog with feature cards |
| `features/settings/FeatureCard.java` | Individual feature card UI |
| `features/settings/IconBrowserDialog.java` | Icon browser for feature customization |

---

## Smart Drill

| File | Description |
|------|-------------|
| `features/smartdrill/SmartDrillFeature.java` | Auto-place drills with power/ducts |
| `features/smartdrill/SmartDrillSettingDialog.java` | Smart drill settings |

---

## Smart Upgrade

| File | Description |
|------|-------------|
| `features/smartupgrade/SmartUpgradeFeature.java` | Smart upgrade chains (conveyors, walls, drills) |

---

## Time Control

| File | Description |
|------|-------------|
| `features/time/TimeControlFeature.java` | Game speed control (forward/rewind) |
| `features/time/TimeControlConfig.java` | Time control configuration |

---

## Services

| File | Description |
|------|-------------|
| `services/ServerService.java` | Server list fetching & caching |
| `services/AuthService.java` | Auth token management (duplicated in features/auth) |
| `services/UpdateService.java` | Version check, release notes fetch |
| `services/PlayerConnectService.java` | Room listing & caching |
| `services/UserService.java` | Batched user data fetching |
| `services/MapService.java` | Map download & detail fetch |
| `services/SchematicService.java` | Schematic download & detail fetch |
| `services/TagService.java` | Tag/category data |
| `services/ModService.java` | Planet/mod list data |
| `services/CrashReportService.java` | Crash detection & report submission |
| `services/CrashReportDialog.java` | Crash report dialog UI |
| `services/TapListener.java` | Hold-to-interact tile listener |
| `services/UpdateAvailableDialog.java` | Update notification dialog |

---

## DTO

| File | Description |
|------|-------------|
| `dto/MapData.java` | Map list item DTO |
| `dto/MapDetailData.java` | Map detail DTO |
| `dto/SchematicData.java` | Schematic list item DTO |
| `dto/SchematicDetailData.java` | Schematic detail DTO |
| `dto/ModData.java` | Planet/mod list DTO |
| `dto/UserData.java` | User data DTO |
| `dto/PagingRequest.java` | Pagination request DTO |
| `dto/Sort.java` | Sort config DTO |
| `dto/TagCategory.java` | Tag category DTO |
| `dto/TagData.java` | Tag data DTO |
| `dto/TaskData.java` | Generic task DTO |
| `dto/TaskResponse.java` | Task response DTO |

---

## UI

| File | Description |
|------|-------------|
| `ui/ChangelogDialog.java` | Release changelog browser with pagination |
| `ui/Debouncer.java` | Input debouncing utility |
| `ui/DetailStats.java` | Detail stats display component |
| `ui/NetworkImage.java` | Async network-loaded image with disk cache |
| `ui/TagBar.java` | Tag selection bar component |
| `ui/TagContainer.java` | Tag container component |
| `ui/UserCard.java` | User profile card component |

---

## Utils

| File | Description |
|------|-------------|
| `utils/State.java` | Generic state machine |
