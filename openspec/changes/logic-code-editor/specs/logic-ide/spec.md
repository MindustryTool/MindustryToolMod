## ADDED Requirements

### Requirement: Native LogicDialog Integration and Standalone IDE Access
`LogicEditorFeature` SHALL provide a standalone entry point in the mod toolbar/QuickAccess HUD and SHALL inject an "Open in Logic IDE" action button into Mindustry's native `LogicDialog` when enabled.

#### Scenario: Opening Logic IDE from a processor's native LogicDialog
- **WHEN** the player opens a `LogicBuild` processor in Mindustry's native `LogicDialog` and clicks "Open in Logic IDE"
- **THEN** `LogicEditorDialog` opens bound to that `LogicBuild`, automatically decompiles (or restores via `# @mts:` metadata) the processor's current MLOG code into the `.mts` editor, and loads its linked building names (`LogicBuild.links`)

#### Scenario: Applying compiled code back to the attached processor
- **WHEN** the player clicks "Apply to Processor" inside `LogicEditorDialog` while bound to a `LogicBuild`
- **THEN** the current `.mts` script is compiled to MLOG and written to the `LogicBuild` configuration

### Requirement: Split-Pane Workspace Layout with Collapsible Tool Tabs
`LogicEditorDialog` SHALL render a split-pane Solim layout with the `.mts` `SolimCodeEditor` and action toolbar on the left, and a collapsible right dock containing tabs for `[Compiled MLOG]`, `[Debugger & Watch]`, `[Problems / Linter]`, and `[AI Copilot]`.

#### Scenario: Live synchronization with Compiled MLOG tab
- **WHEN** the user edits `.mts` source code while the `[Compiled MLOG]` tab is active
- **THEN** the compiled MLOG preview and instruction counter (`N / 1000`) update reactively, and selecting a line in the `.mts` editor highlights the corresponding instructions in the MLOG tab

#### Scenario: Navigating from Problems tab to source line
- **WHEN** the user clicks a diagnostic entry in the `[Problems / Linter]` tab
- **THEN** `SolimCodeEditor` scrolls to and focuses the offending source line

### Requirement: Local Script Library, Built-in Starter Examples, and Reusable Mod Snippets
The IDE SHALL provide a Script Library drawer/modal allowing players to save and load named `.mts` scripts on disk, load built-in starter examples (covering Unit Carrier, Smart Turret Radar, Display Dashboard, and Memory Logger), and insert or import reusable standard library helper functions provided by the mod.

#### Scenario: Saving and reloading a user script
- **WHEN** the player saves the current script as `"flare-carrier"` in the Script Library and later selects `"flare-carrier"` from the library
- **THEN** the script is persisted to local storage and restored into the editor

#### Scenario: Inserting a reusable mod snippet or starter example
- **WHEN** the player selects a built-in example or reusable standard library snippet (e.g., `drawProgressBar`, `clampRatio`, or `unitMoveAndTake`)
- **THEN** the selected template or reusable function code is inserted into the editor (or resolved during compilation when imported from `"std"`)

### Requirement: Direct In-World LExecutor Debugger and Variable Watch
When attached to an in-world `LogicBuild` in singleplayer/local games, the `[Debugger & Watch]` tab SHALL directly hook the processor's `LExecutor` to support Pause, Resume, Step Instruction, Step Source Line, line Breakpoints, and live inspection of `executor.vars` and `executor.textBuffer`.

#### Scenario: Pausing on a line breakpoint and inspecting variables
- **WHEN** the player sets a breakpoint on line 10 in `SolimCodeEditor` while attached to a running singleplayer `LogicBuild` and `executor.counter` reaches an instruction mapped to line 10
- **THEN** the debugger pauses the processor's execution, highlights line 10 as the active execution line, and displays the current values of all `executor.vars` and the `print` buffer in the `[Debugger & Watch]` tab

#### Scenario: Stepping one high-level source line
- **WHEN** the debugger is paused on line 10 and the player clicks "Step Line"
- **THEN** the debugger advances `executor.runOnce()` until `@counter` reaches an instruction mapped to a different `.mts` source line (or hits another breakpoint) and updates the variable watch table

### Requirement: Bring-Your-Own-Key (BYOK) AI Copilot Integration
The `[AI Copilot]` tab and `LogicEditorSettingsDialog` SHALL allow players to configure their own AI API base URL, API key, and model identifier, and SHALL execute all HTTP requests through `mindustrytool.services.Request` with automatic context injection of the processor's linked blocks, active `.mts` code, and linter diagnostics.

#### Scenario: Generating code with linked building context
- **WHEN** the player configures a valid BYOK API key, attaches the IDE to a processor linked to `switch1`, `vault1`, and `cyclone1`, and prompts the AI Copilot to generate logic
- **THEN** `LogicAiService` sends an HTTP request via `mindustrytool.services.Request` containing the linked block metadata (`switch1`, `vault1`, `cyclone1`) and `.mts` grammar context, and offers to insert or apply the returned `.mts` code

#### Scenario: One-click AI fix for a linter or compiler error
- **WHEN** a compiler or linter error is present and the player clicks "Fix with AI"
- **THEN** the AI Copilot sends the source code and diagnostic details via `mindustrytool.services.Request` and presents the corrected `.mts` code

### Requirement: Complete Internationalization Coverage
All user-visible labels, buttons, tabs, tooltips, diagnostic messages, example titles, and settings in `LogicEditorFeature` SHALL use localization keys defined in `assets/bundles/bundle.properties` with descriptive translator comments.

#### Scenario: Bundle verification passes
- **WHEN** `./gradlew checkBundles` is executed
- **THEN** all translation keys used by the Logic IDE feature exist in `assets/bundles/bundle.properties` with descriptive comments above each key
