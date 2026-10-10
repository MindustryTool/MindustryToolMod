## Why

Writing and maintaining Mindustry Logic (`mlog`) inside the game is painful: the vanilla visual block canvas (`LCanvas`) becomes unwieldy past a few dozen instructions, while raw MLOG assembly lacks structured control flow (`if/else`, `while`, `for`, functions), inline mathematical expressions, and static validation—causing simple variable typos (`coppper` instead of `copper`) to silently evaluate to `null`/`0` at runtime. External web compilers (like Mindcode or MlogJS) require constant alt-tabbing and copy-pasting, cannot inspect a processor's actual in-world linked buildings (`switch1`, `vault1`, `cyclone1`), and cannot debug a live `LExecutor` in-game.

Building a complete in-game Logic IDE directly into MindustryTool bridges high-level programming ergonomics with live in-world processor context: players can write Modern JS/TS-style scripts that compile to MLOG, decompile any existing processor's MLOG back into readable structured code, catch bugs via static analysis, step through live `LExecutor` instructions with variable watches, reuse built-in examples and standard library snippets, and use a Bring-Your-Own-Key (BYOK) AI Copilot that automatically understands the processor's linked blocks.

## What Changes

- **Reusable `SolimCodeEditor` Widget (`:solim-core` & `:solim`)**:
  - A high-performance, multi-line code editor component (`SolimCodeEditor` exposed via `UI.codeEditor(...)`) built for Solim.
  - Separates the plain-text line buffer from token rendering (avoiding Arc `[#rrggbb]` markup corruption in cursor/selection indices).
  - Virtualized line viewport rendering (60 FPS even on 1,000+ line scripts), line number gutter, interactive breakpoint toggle gutter, diagnostic severity indicators, active execution line highlight, auto-indentation, and undo/redo history.
- **Bidirectional Modern JS/TS ⇄ MLOG Language Engine (`mlog-ts-compiler`)**:
  - **Forward Compiler (`.mts` → `MLOG`)**: Pure Java 8-compatible recursive-descent lexer, parser, AST, and IR generator supporting `let`/`const`, inline `function` declarations, `if/else`, `while`, `for`, `break`/`continue`, template strings (`` `Copper: ${stored}` ``), memory cell array indexing (`cell1[i]`), building/unit property & method sugar (`switch1.enabled`, `vault1[@copper]`, `cyclone1.shoot(...)`, `ucontrol.move(...)`), tuple destructuring for multi-output instructions (`let [found, x, y, core] = ulocate.building(...)`), and raw MLOG escape hatches (`asm("...")`).
  - **Hybrid Reverse Decompiler (`MLOG` → `.mts`)**: Restores 100% lossless high-level source when compact `# @src` metadata comments are present, and falls back to a Control-Flow Graph (CFG) decompiler that recovers `if/else` branches, `while` loops, inline expressions, and method sugar from any arbitrary processor in schematics or maps.
  - **AST Code Formatter**: One-click deterministic pretty-printer normalizing indentation, spacing, and brace layout.
  - **Static Bug Reporter / Linter**: Real-time diagnostics for syntax errors, uninitialized/typo variable reads, instruction limit overflow (`> 1000` instructions), unreachable dead code, and missing `ubind`/`printflush`/`drawflush` calls.
  - **Bidirectional `SourceMap`**: Maps high-level source lines to generated MLOG instruction indices (`@counter`) and vice versa.
- **In-Game Logic IDE Workspace & Library (`LogicEditorFeature`)**:
  - Accessible both via an **"Open in Logic IDE"** button injected into Mindustry's native `LogicDialog` (bound to the active `LogicBuild`) and via a standalone feature button in the mod toolbar/QuickAccess HUD.
  - **Split-Pane Layout**: Code editor and script toolbar on the left; collapsible right dock with tabs for **`[Compiled MLOG]`**, **`[Debugger & Watch]`**, **`[Problems / Linter]`**, and **`[AI Copilot]`**.
  - **Script Library, Starter Examples & Reusable Mod Snippets**: Save/load named user scripts on disk (`Vars.dataDirectory`), browse built-in starter examples (Unit Carrier, Turret Radar, Display Dashboard, Memory Logger), and insert/import reusable standard library helper functions provided by the mod (math utilities, display progress bars, unit state helpers).
- **Direct In-World `LExecutor` Debugger**:
  - Hooks directly into the attached in-world `LogicBuild.executor` (`LExecutor`) in singleplayer/local games.
  - Supports Pause, Resume, Step Instruction (1 MLOG op), Step Line (advance until `@counter` maps to a new high-level source line), gutter Breakpoints, live reactive Variable Watch table (`executor.vars`), and `textBuffer` print preview.
- **BYOK AI Copilot Integration**:
  - Configured via Feature Settings (user-provided API base URL, API key, and model identifier) and executed strictly through `mindustrytool.services.Request`.
  - Automatically injects the attached processor's linked building names/types (`links`), processor tier (`@ipt`), active source code, and current linter diagnostics to support Generate, Explain Processor, and One-Click Fix Error workflows.

## Capabilities

### New Capabilities
- `solim-code-editor`: Multi-line syntax-highlighted code editor component in `:solim-core` / `:solim` with virtualized line rendering, line numbers, breakpoint/diagnostic gutter, token coloring, and undo/redo.
- `mlog-ts-compiler`: Bidirectional Modern JS/TS-style high-level language compiler, hybrid CFG decompiler, AST formatter, static bug linter, and `SourceMap` generator for Mindustry Logic.
- `logic-ide`: Split-pane in-game Logic IDE feature, native `LogicDialog` integration, user script library with built-in examples and reusable mod code snippets, direct `LExecutor` debugger, and BYOK AI Copilot.

### Modified Capabilities
<!-- None -->

## Impact

- **`:solim-core` & `:solim`**:
  - `solim.input.SolimCodeEditor` (and supporting token/gutter/line model classes)
  - `solim.UI.codeEditor(...)` facade methods
  - Headless Solim unit tests in `solim-core/src/test/java/solim/input/SolimCodeEditorTest.java`
- **`:mod`**:
  - `mindustrytool.features.logiceditor.*` (Feature, IDE Dialog, Settings Dialog, Split-Pane Views, Native `LogicDialog` injector)
  - `mindustrytool.features.logiceditor.lang.*` (Lexer, Parser, AST, Formatter, Linter, Compiler, CFG Decompiler, SourceMap, Mod Standard Library Snippets)
  - `mindustrytool.features.logiceditor.debug.*` (`LExecutor` debugger controller & watch state)
  - `mindustrytool.features.logiceditor.ai.*` (BYOK AI service using `mindustrytool.services.Request`)
  - Registration in `mindustrytool.Main`
  - Localization keys with translator comments in `assets/bundles/bundle.properties`
