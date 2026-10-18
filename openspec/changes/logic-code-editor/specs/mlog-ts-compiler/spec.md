## ADDED Requirements

### Requirement: Forward Compilation from Modern JS/TS Syntax to MLOG
The compiler SHALL compile `.mts` source code (variables, constants, arithmetic/logical expressions, `if/else`, `while`, `for`, `break`/`continue`, inline `function` declarations, template strings, memory array indexing `cell[i]`, building/unit property & method sugar, tuple destructuring for multi-output instructions, and `asm(...)` escape hatches) into valid Mindustry Logic (`MLOG`) instructions.

#### Scenario: Compiling control flow and expressions
- **WHEN** compiling `.mts` code containing `let active = switch1.enabled; if (active) { cell1[0] = (a + b) * 2; }`
- **THEN** the compiler emits valid `sensor`, conditional `jump`, `op add`, `op mul`, and `write` MLOG instructions with correct jump target indices

#### Scenario: Compiling inline functions and template strings
- **WHEN** compiling `.mts` code with a user function `function doubleVal(v) { return v * 2 }` and `` print(`Value: ${doubleVal(x)}`) `` followed by `printflush(message1)`
- **THEN** the compiler inlines `doubleVal(x)` and emits sequential `print` instructions followed by `printflush message1`

#### Scenario: Compiling unit control and multi-return locate destructuring
- **WHEN** compiling `ubind(@flare); let [found, cx, cy, core] = ulocate.building("core", false); ucontrol.move(cx, cy);`
- **THEN** the compiler emits `ubind @flare`, `ulocate building core false @copper cx cy found core`, and `ucontrol move cx cy 0 0 0`

### Requirement: Hybrid Bidirectional MLOG Decompilation
The decompiler SHALL convert MLOG code back into readable `.mts` source code by restoring lossless `# @mts:` metadata comments when valid and otherwise reconstructing structured `.mts` code (`if/else`, `while`, inline expressions, and building/unit sugar) via Control-Flow Graph (CFG) analysis.

#### Scenario: Lossless round-trip via metadata comments
- **WHEN** `.mts` source code is compiled with `# @mts:` metadata comments and subsequently decompiled without external modification to the MLOG instructions
- **THEN** the decompiler restores the exact original `.mts` source code including comments and function definitions

#### Scenario: Heuristic CFG decompilation of arbitrary processor MLOG
- **WHEN** arbitrary raw MLOG without metadata (containing `sensor`, `op`, forward conditional `jump` branches, and backward `jump` loops) is decompiled
- **THEN** the decompiler folds single-use temporary variables into inline expressions and reconstructs `if/else` and `while` blocks in valid `.mts` syntax

### Requirement: Deterministic AST Code Formatter
The formatter SHALL parse `.mts` source code into an AST and format it with consistent indentation, operator spacing, and brace placement while preserving comments.

#### Scenario: Formatting unindented source code
- **WHEN** poorly indented `.mts` code `if(a>0){let x=1+2;print(x)}` is passed to the formatter
- **THEN** the formatter outputs multi-line indented code with normalized spacing around operators and braces

### Requirement: Static Bug Reporter and Linter
The linter SHALL analyze `.mts` AST and control flow to report syntax errors, reads of uninitialized variables, instruction count exceeding `1000`, unreachable dead code, and `print`/`draw`/`ucontrol` usage without corresponding `printflush`/`drawflush`/`ubind`.

#### Scenario: Detecting uninitialized variable typo
- **WHEN** `.mts` code assigns `let copper = 10` and later reads `coppper` (which is neither declared, a `@built-in`, nor a linked building name)
- **THEN** the linter produces a warning diagnostic on the exact line and column of `coppper`

#### Scenario: Detecting missing printflush or ubind
- **WHEN** `.mts` code invokes `print("hello")` without any reachable `printflush(...)` in the script
- **THEN** the linter reports a warning indicating that printed text is never flushed to a message block

### Requirement: Bidirectional SourceMap Generation
The compiler SHALL produce a `SourceMap` mapping each generated MLOG instruction index (`0..N-1`) to its originating 1-based `.mts` source line and mapping each `.mts` source line to its generated MLOG instruction indices.

#### Scenario: Resolving MLOG counter to high-level source line
- **WHEN** a `.mts` statement on line 7 compiles to MLOG instruction indices 14, 15, and 16
- **THEN** querying `sourceMap.getSourceLine(15)` returns `7` and `sourceMap.getMlogIndices(7)` contains `[14, 15, 16]`
