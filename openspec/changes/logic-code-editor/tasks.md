## 1. Phase 1: Reusable `SolimCodeEditor` in `:solim-core` & `:solim`

- [ ] 1.1 Create token, diagnostic, and document line models (`CodeToken`, `CodeDiagnostic`, `CodeDocument`) in `solim-core/src/solim/input/` for plain-text multi-line editing and undo/redo history
- [ ] 1.2 Implement `SolimCodeEditor` in `solim-core/src/solim/input/SolimCodeEditor.java` with two-way `Signal<String>` binding, virtualized line viewport rendering, pluggable token highlighter, line number/breakpoint/diagnostic gutter, active execution line highlight, and keyboard/clipboard ergonomics
- [ ] 1.3 Expose `codeEditor(...)` factory methods in `solim/src/solim/UI.java`
- [ ] 1.4 Write headless behavioral unit tests in `solim-core/src/test/java/solim/input/SolimCodeEditorTest.java` verifying two-way signal binding without color markup corruption, undo/redo transitions, breakpoint toggling, virtualized visible line calculation, and disposal cleanup

## 2. Phase 2: Forward `.mts` Compiler, Formatter, Linter & Mod Standard Library

- [ ] 2.1 Implement `.mts` Lexer and Recursive-Descent Parser (`MtsLexer`, `MtsParser`, and `AstNode` hierarchy) in `mod/src/mindustrytool/features/logiceditor/lang/` supporting `let`/`const`, `function`, `if/else`, `while`, `for`, `break`/`continue`, template strings, array indexing `cell[i]`, building/unit method sugar, tuple destructuring, and `asm(...)`
- [ ] 2.2 Implement `MtsTokenHighlighter` in `mod/src/mindustrytool/features/logiceditor/lang/` to map `.mts` and `MLOG` lines to `SolimCodeEditor` color spans
- [ ] 2.3 Implement `MtsFormatter` (`AstNode` pretty-printer) normalizing indentation, operator spacing, and braces while preserving comments
- [ ] 2.4 Implement `MtsLinter` to detect syntax errors, uninitialized/typo variable reads, instruction count overflow (`> 1000`), unreachable code, and missing `ubind`/`printflush`/`drawflush`
- [ ] 2.5 Implement `MlogCompiler` and bidirectional `SourceMap` (`sourceLine ⇄ mlogIndex`) with inline function expansion, `# @mts:` lossless source metadata comment encoding, and built-in `"std"` reusable snippet resolution
- [ ] 2.6 Create built-in starter examples and reusable mod standard library snippets (`MtsStandardLibrary` & `MtsExamples`: Unit Carrier, Smart Turret Radar, Display Dashboard, Memory Logger, `clampRatio`, `drawProgressBar`, unit helpers)
- [ ] 2.7 Write unit tests in `mod/src/test/java/mindustrytool/features/logiceditor/lang/` covering forward compilation, `SourceMap` accuracy, AST formatting, and static linter bug detection

## 3. Phase 3: Hybrid MLOG ⇄ `.mts` Decompiler

- [ ] 3.1 Implement `MlogParser` and lossless `# @mts:` metadata comment decoder with instruction hash verification
- [ ] 3.2 Implement `MlogCfgDecompiler` to partition raw MLOG instructions into basic blocks, inline single-use temporary variables into expressions, and reconstruct `if/else` branches, `while` loops, and building/unit method sugar
- [ ] 3.3 Write unit tests in `mod/src/test/java/mindustrytool/features/logiceditor/lang/MlogDecompilerTest.java` verifying both lossless metadata round-tripping and heuristic CFG decompilation of raw MLOG scripts

## 4. Phase 4: Direct In-World `LExecutor` Debugger & Variable Watch

- [ ] 4.1 Implement `LogicDebuggerController` in `mod/src/mindustrytool/features/logiceditor/debug/` hooking into an attached singleplayer `LogicBuild.executor` (`LExecutor`) for Pause, Resume, Step Instruction, Step Source Line, and breakpoint interception
- [ ] 4.2 Expose reactive signals on `LogicDebuggerController` for active `@counter`, active `.mts` line (via `SourceMap`), live `executor.vars` snapshots, and `executor.textBuffer` output
- [ ] 4.3 Write behavioral unit tests for `LogicDebuggerController` stepping, breakpoint triggering, and source-line mapping against a local `LExecutor` instance

## 5. Phase 5: BYOK AI Copilot Service

- [ ] 5.1 Implement `LogicAiService` in `mod/src/mindustrytool/features/logiceditor/ai/` using `mindustrytool.services.Request` with configurable BYOK base URL, API key, and model settings
- [ ] 5.2 Build prompt context assembly in `LogicAiService` that injects `.mts` grammar rules, the attached processor's linked building names/types (`LogicBuild.links`), current source code, and active linter diagnostics for Generate, Explain, and Fix Error actions

## 6. Phase 6: Logic IDE Split-Pane UI, Script Library & Native `LogicDialog` Integration

- [ ] 6.1 Add all required localization keys with descriptive translator comments to `assets/bundles/bundle.properties`
- [ ] 6.2 Implement `LogicScriptLibrary` for saving/loading user scripts on disk (`Vars.dataDirectory`) alongside built-in examples and reusable mod snippets
- [ ] 6.3 Implement `LogicEditorSettingsDialog` and `LogicEditorSettingsView` (BYOK AI endpoint/key/model configuration, editor font scale, metadata comment toggle)
- [ ] 6.4 Implement `LogicEditorDialog` and split-pane views (`LogicEditorView`, `CompiledMlogTabView`, `DebuggerWatchTabView`, `ProblemsTabView`, `AiCopilotTabView`, and `ScriptLibraryModal`) using declarative Solim UI
- [ ] 6.5 Implement `LogicEditorFeature` with standalone QuickAccess/toolbar entry and native `Vars.ui.logic` (`LogicDialog`) button injection to open the currently edited `LogicBuild` directly in the Logic IDE, and register `LogicEditorFeature` in `mindustrytool.Main`
- [ ] 6.6 Run `./gradlew checkBundles test` to verify all localization keys and unit tests pass
