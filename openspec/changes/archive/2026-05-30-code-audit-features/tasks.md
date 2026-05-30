## 1. Feature Inventory

- [x] 1.1 Create `docs/FEATURE.md` with all feature groups, descriptions, and file mappings
- [x] 1.2 Verify coverage: every `.java` file in `src/mindustrytool/` is referenced

## 2. Core Infrastructure Audit

- [x] 2.1 Audit `Config.java`, `Main.java`, `Utils.java`, `IconUtils.java`, `MdtInitEvent.java`, `MdtKeybinds.java`
- [x] 2.2 Audit `services/` package (CrashReportService, UpdateService, MapService, ModService, PlayerConnectService, SchematicService, ServerService, TagService, TapListener, UserService)
- [x] 2.3 Audit `dto/` package
- [x] 2.4 Audit `ui/` package
- [x] 2.5 Audit `utils/` package

## 3. Auth Feature Audit

- [x] 3.1 Audit `features/auth/` (AuthService, AuthLoginDialog, AuthHttp, DTOs)

## 4. Autoplay Feature Audit

- [x] 4.1 Audit `features/autoplay/` (AutoplayFeature, SettingDialog, all task types)

## 5. Browser Features Audit

- [x] 5.1 Audit `features/browser/map/` (MapBrowserFeature, MapDialog, MapImage, MapInfoDialog)
- [x] 5.2 Audit `features/browser/schematic/` (SchematicBrowserFeature, SchematicDialog, SchematicImage, SchematicInfoDialog)
- [x] 5.3 Audit `features/browser/` shared (FilterDialog, SearchConfig)

## 6. Chat Features Audit

- [x] 6.1 Audit `features/chat/global/` (ChatFeature, ChatApiClient, ChatService, ChatStreamClient, ChatStateManager, ChatConfig, ChatStore, DTOs, events, UI components)
- [x] 6.2 Audit `features/chat/pretty/` (PrettyChatFeature, PrettyChatConfig, PrettyChatSettingsDialog)
- [x] 6.3 Audit `features/chat/translation/` (ChatTranslationFeature, all translation providers, config, settings dialog)

## 7. Display Features Audit

- [x] 7.1 Audit `features/display/healthbar/`
- [x] 7.2 Audit `features/display/itemvisualizer/`
- [x] 7.3 Audit `features/display/pathfinding/`
- [x] 7.4 Audit `features/display/progress/`
- [x] 7.5 Audit `features/display/quickaccess/`
- [x] 7.6 Audit `features/display/range/`
- [x] 7.7 Audit `features/display/teamresource/`
- [x] 7.8 Audit `features/display/togglerendering/`
- [x] 7.9 Audit `features/display/wavepreview/`

## 8. Gameplay Features Audit

- [x] 8.1 Audit `features/godmode/`
- [x] 8.2 Audit `features/time/`
- [x] 8.3 Audit `features/smartdrill/`
- [x] 8.4 Audit `features/smartupgrade/`

## 9. Network & Sync Features Audit

- [x] 9.1 Audit `features/playerconnect/`
- [x] 9.2 Audit `features/savesync/`

## 10. Content & Settings Features Audit

- [x] 10.1 Audit `features/background/`
- [x] 10.2 Audit `features/music/`
- [x] 10.3 Audit `features/settings/`
