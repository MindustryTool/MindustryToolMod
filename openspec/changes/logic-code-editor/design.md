## Context

Mindustry's Logic system (`mindustry.logic.*`) executes flat assembly-like instructions (`MLOG`) inside each processor's `LExecutor` instance (up to `LogicBlock.maxInstructions = 1000` instructions). While MLOG is compact and deterministic, authoring complex logic in Mindustry's native block-based `LCanvas` or raw MLOG text suffers from major limitations:
1. **No structured control flow or expressions**: Every `if/else`, `while`, or `for` loop must be hand-lowered to `jump <line> <cond>` instructions, and compound math expressions must be split into temporary variables (`op add __t0 a b`).
2. **Silent runtime failures**: MLOG has no static variable checking; reading a misspelled variable (`coppper`) silently yields `0`/`null`, and forgetting `printflush` or `ubind` causes subtle runtime misbehavior.
3. **Lack of in-game code editing primitives**: Solim currently provides `SolimTextField` (wrapping single-line `arc.scene.ui.TextField`), with no multi-line code editor supporting line numbers, syntax coloring, diagnostics, or breakpoints.
4. **Disconnected external tooling**: Web-based MLOG compilers cannot inspect the in-world processor's linked buildings (`LogicBuild.links`), cannot step through a live `LExecutor`, and cannot offer context-aware AI assistance inside the game.

All code must adhere to MindustryToolMod's architecture constraints:
- **Java 8 Runtime & Android D8 Compatibility**: Use `arc.func.*` (`Func`, `Cons`, `Prov`, `Boolf`) instead of `java.util.function.*`, and avoid post-Java-8 stdlib APIs.
- **Module Isolation**: Reusable UI primitives belong in `:solim-core` (exposed via `:solim`'s `UI`), while `:mod` must never reference `:solim-runtime`.
- **HTTP & i18n**: All AI network calls must use `mindustrytool.services.Request`, and all user-facing strings must be localized in `assets/bundles/bundle.properties`.

## Goals / Non-Goals

**Goals:**
- Build a reusable, virtualized multi-line code editor component (`SolimCodeEditor` / `UI.codeEditor`) in `:solim-core` and `:solim` with token highlighting, line numbers, breakpoint/diagnostic gutter, selection, clipboard, auto-indent, and undo/redo.
- Implement a pure-Java bidirectional compiler between a Modern JS/TS-style language (`.mts`) and `MLOG`, including an AST code formatter and a static bug linter.
- Support both 100% lossless round-tripping (via compact `# @src` metadata comments) and heuristic Control-Flow Graph (CFG) decompilation for arbitrary existing MLOG processors.
- Provide a split-pane in-game Logic IDE (`LogicEditorFeature`) accessible from both Mindustry's native `LogicDialog` and the mod toolbar.
- Ship a local Script Library alongside built-in starter examples and reusable mod standard library code snippets.
- Hook directly into the attached in-world `LogicBuild.executor` (`LExecutor`) in singleplayer to pause, step by source line or MLOG instruction, trigger breakpoints, and watch live variables.
- Integrate a Bring-Your-Own-Key (BYOK) AI Copilot via `mindustrytool.services.Request` that automatically includes the processor's linked blocks, code, and diagnostics.

**Non-Goals:**
- Supporting full ECMAScript runtime features that have no MLOG equivalent (e.g., dynamic heap closures, prototypes, `async/await`, or garbage-collected objects).
- Mutating or pausing remote server `LExecutor` instances in multiplayer (stepping and breakpoints operate on local/singleplayer `LExecutor` instances; in multiplayer, applying compiled MLOG updates the processor configuration via standard `LogicBuild` config packets).
- Replacing Mindustry's vanilla `LogicDialog` completely—players can still use vanilla `LCanvas` and click "Open in Logic IDE" whenever desired.

## Decisions

### 1. Dedicated Virtualized `SolimCodeEditor` in `:solim-core` Instead of `TextArea` Color Markup
- **Decision**: Implement `SolimCodeEditor` in `solim.input` as a custom interactive `Element` (backed by a plain-text line model `List<String>`, cursor/selection state, undo/redo ring buffer, and pluggable token/gutter providers) rather than injecting `[#rrggbb]` color tags into `arc.scene.ui.TextArea`.
- **Rationale**: Injecting Arc color markup tags into `TextArea`'s text buffer corrupts character index math for cursor movement, mouse hit-testing, and clipboard copy/paste. By keeping the document model strictly plain text and applying token colors only during `draw()` for the visible line range (`firstVisibleLine..lastVisibleLine`), we achieve:
  - Constant 60 FPS rendering even on 1,000+ line files.
  - Native rendering of a left gutter (line numbers, red/amber diagnostic markers, clickable breakpoint dots) and active debugger line highlight.
  - Full headless testability in `:solim-core` without requiring Mindustry runtime initialization.
- **Alternatives Considered**:
  - *Subclassing `arc.scene.ui.TextArea`*: Rejected because `TextArea` couples line wrapping, cursor offsets, and `GlyphLayout` directly to the raw text string and offers no gutter or per-token color hook.

### 2. Pure-Java Recursive-Descent `.mts` Compiler & AST Pipeline
- **Decision**: Implement a self-contained language pipeline in `mindustrytool.features.logiceditor.lang`:
  ```
  Source (.mts) ──▶ Lexer ──▶ Parser ──▶ AstNode ──┬──▶ AstFormatter (Source)
                                                   ├──▶ StaticLinter (Diagnostics)
                                                   └──▶ MlogCompiler (MLOG + SourceMap)
  ```
- **Language Grammar Highlights (Modern JS/TS Style)**:
  - **Variables & Constants**: `let x = 10`, `const MAX = 100` (constants fold at compile time).
  - **Control Flow**: `if (cond) { ... } else { ... }`, `while (cond) { ... }`, `for (let i = 0; i < n; i++) { ... }`, `break`, `continue`.
  - **Functions**: `function name(a, b) { return a + b }` — inlined at call sites by default (since MLOG has no hardware call stack and inlining avoids `@counter` return-address overhead), with cycle detection to prevent recursive expansion loops.
  - **Property & Indexing Sugar**:
    - `building.prop` or `building[@item]` → `sensor __tmp building @prop` (on RHS) or `control prop building val` (on LHS for controllable properties like `enabled`).
    - `cell1[idx]` → `read __tmp cell1 idx` (RHS) / `write val cell1 idx` (LHS).
  - **Method & Built-in Sugar**:
    - `turret.shoot(x, y, enabled)` → `control shoot turret x y enabled 0`
    - `turret.radar(f1, f2, f3, sort, order)` → `radar f1 f2 f3 sort turret order __tmp`
    - `ubind(@flare)`, `ucontrol.move(x, y)`, `ucontrol.itemTake(core, @copper, 20)`, `ucontrol.itemDrop(vault1, 20)`
    - Multi-return tuple destructuring: `let [found, x, y, building] = ulocate.building("core", false)`
  - **Template Strings & Print**: `` print(`Status: ${active}\nCopper: ${stored}`) `` lowers into sequential `print` instructions, and `printflush(message1)` flushes.
  - **Display Drawing**: `draw.clear(r, g, b)`, `draw.color(r, g, b, a)`, `draw.rect(x, y, w, h)`, `draw.lineRect(...)`, `drawflush(display1)`.
  - **Escape Hatch & Mod Snippets**: `asm("setrate 100")` emits raw MLOG verbatim; `import { clampRatio, drawProgressBar } from "std"` (or snippet insertion) brings in reusable mod standard library functions.
- **Alternatives Considered**:
  - *Embedding Rhino / Nashorn / ANTLR*: Rejected due to APK/DEX size bloat, reflection/classloader issues on Android under Mindustry's `ModClassLoader`, and mismatch with MLOG's static register model.

### 3. Hybrid MLOG ⇄ `.mts` Round-Tripping (Metadata Comments + CFG Structurer)
- **Decision**: Combine two complementary mechanisms in `MlogDecompiler`:
  1. **Lossless Comment Metadata (`# @mts:<base64-chunk>`)**: When compiling `.mts` → `MLOG` in the IDE, append compact `# @mts:...` comment lines at the end of the MLOG output (Mindustry's `LAssembler` ignores lines starting with `#`, while `LogicBuild.code` preserves them inside the compressed processor configuration as long as total text stays within `maxByteLen`). When reopening a processor containing valid `# @mts:` comments whose instruction hash matches the active MLOG instructions, the exact original `.mts` source (including comments and function definitions) is restored.
  2. **Heuristic Control-Flow Graph (CFG) Decompiler**: When opening any processor without `# @mts:` metadata (or if the MLOG was modified externally), parse the raw MLOG instructions into basic blocks:
     - Reconstruct expression trees by inlining single-use temporary assignments (`op` / `sensor` / `read` whose destination variable is only read once before being overwritten).
     - Recognize backward `jump` edges as `while (cond) { ... }` loops and forward conditional `jump` diamonds as `if (cond) { ... } else { ... }` blocks.
     - Fallback to structured `goto` / labeled jumps or `asm(...)` for irreducible spaghetti jumps so 100% of valid MLOG processors decompile into valid, re-compilable `.mts` code.

### 4. Bidirectional `SourceMap` (`sourceLine ⇄ mlogIndex`)
- **Decision**: `MlogCompiler` produces an immutable `SourceMap` recording:
  - `int[] mlogToSourceLine`: for each MLOG instruction index `0..N-1`, the 1-based `.mts` line number that generated it.
  - `IntSeq[] sourceLineToMlogIndices`: for each `.mts` line, the generated MLOG instruction indices.
- **Rationale**: Powers three features simultaneously:
  1. Clicking a line in `.mts` highlights the corresponding instructions in the `[Compiled MLOG]` tab (and vice versa).
  2. The debugger maps `executor.counter.numval` directly to the active `.mts` line in real time.
  3. Line breakpoints placed in the `SolimCodeEditor` gutter translate directly to target `@counter` instruction indices.

### 5. Direct In-World `LExecutor` Debugger Hook
- **Decision**: Create `LogicDebuggerController` in `mindustrytool.features.logiceditor.debug` that attaches to a target `LogicBuild` in singleplayer/local games:
  - **Pause / Step Execution**: When paused in the debugger, intercept or hold the processor's per-tick execution (by overriding/controlling `LogicBuild` update ticking or restoring `executor.counter` while paused and explicitly invoking `executor.runOnce()` only on manual step commands).
  - **Step Instruction**: Calls `executor.runOnce()` once and updates reactive signals for `@counter`, active `.mts` line, `vars`, and `textBuffer`.
  - **Step Source Line**: Repeatedly calls `executor.runOnce()` (with a safety cap of `1000` ops) until `sourceMap.getSourceLine((int) executor.counter.numval)` changes or hits a breakpoint.
  - **Breakpoints**: Checked on every `Events.run(Trigger.update, ...)` tick when attached; if `executor.counter.numval` hits a registered breakpoint instruction index, the debugger automatically transitions to `PAUSED`.

### 6. BYOK AI Copilot via `mindustrytool.services.Request`
- **Decision**: Implement `LogicAiService` using `mindustrytool.services.Request` configured from user settings (`logic-editor.ai.baseUrl`, `logic-editor.ai.apiKey`, `logic-editor.ai.model`, defaulting to an OpenAI-compatible `/v1/chat/completions` schema and supporting Gemini/OpenRouter endpoints).
- **Context Injection**: Every AI prompt automatically includes a compact system context describing:
  - The `.mts` language syntax rules and built-in functions.
  - The attached processor's linked blocks (`LogicBuild.links`: e.g., `cyclone1 (Cyclone)`, `vault1 (Vault)`, `switch1 (Switch)`, `message1 (Message)`).
  - Current `.mts` code and any active `StaticLinter` diagnostics.

## Risks / Trade-offs

- **[Risk] Mindustry `LogicBlock` compressed config size limit (`maxByteLen = 1024 * 100`)** → **Mitigation**: Only attach `# @mts:` lossless source comments if the combined compressed size stays well below `LogicBlock`'s limit; otherwise strip metadata comments and rely on the local Script Library cache + CFG Decompiler.
- **[Risk] Infinite loops during compile-time function inlining or Step Line debugging** → **Mitigation**: Enforce a maximum function inline call depth (e.g., 8 levels, flagging recursion in the linter) and cap "Step Source Line" at 1,000 `runOnce()` steps per click.
- **[Risk] Mobile keyboard input and small screens in split-pane view** → **Mitigation**: Allow collapsing the right tool dock into a full-width editor view, and provide quick-insert toolbar buttons (`Format`, `Snippets`, `Compile`) for touch ergonomics.
