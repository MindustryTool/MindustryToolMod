# solim-mcp-debug-server Specification

## Purpose
TBD - created by archiving change mcp-server-solim-ui-debugging. Update Purpose after archive.

## Requirements

### Requirement: Reflection-based introspection without source changes
The MCP debug server SHALL inspect live Solim UI state purely through Java reflection over existing Solim classes. It SHALL NOT require any modifications to Solim core source code (`solim-reactivity`, `solim-component`, or any other existing Solim capability).

#### Scenario: Inspect without modifying Solim source
- **WHEN** the MCP server starts and inspects a running Solim UI
- **THEN** all `solim.*` classes remain byte-for-byte identical to their shipped form and the server reads state (component tree, signal values, bindings) via reflection

#### Scenario: Reflection access via cached handles
- **WHEN** the server introspects a Solim class for the first time
- **THEN** it caches resolved reflective handles (fields/methods) for subsequent queries and does not re-resolve on every request

#### Scenario: Graceful degradation on schema drift
- **WHEN** a reflective access fails because a Solim field/method no longer exists or has an incompatible type
- **THEN** the server logs a warning, marks that introspection capability as unavailable, and continues serving the remaining capabilities without crashing

### Requirement: MCP server module
The MCP server SHALL live in a separate module `solim-mcp`, depending on `solim` and an MCP SDK, and providing read-only UI debugging tools.

#### Scenario: Server lives in dedicated module
- **WHEN** the project is built
- **THEN** MCP server classes are compiled into the `solim-mcp` module and do not alter the `solim` module artifacts

#### Scenario: Read-only inspection
- **WHEN** any MCP tool is invoked
- **THEN** it returns debug data and never mutates component state, signal values, bindings, or layout

### Requirement: Component tree introspection tool
The server SHALL provide a tool `get_component_tree` that returns the live visible UI hierarchy rooted at the specified `Element` (default current scene root). Each node SHALL include the element name, class name, and its children.

#### Scenario: Query component tree
- **WHEN** `get_component_tree` is called with no arguments
- **THEN** it returns the current Scene2D `Element` tree with node names, class names, and nested children

#### Scenario: Query subtree from named element
- **WHEN** `get_component_tree` is called with a target element name or path
- **THEN** it returns only the subtree of the matching element

### Requirement: Signal and computed value introspection tool
The server SHALL provide a tool `get_signal_values` that returns the current values of `Signal` and `Computed` instances reachable from the inspected component tree and reactive context, using reflection to read internal state (value fields, dependency/observer counts) where public accessors are insufficient.

#### Scenario: Read signal values
- **WHEN** `get_signal_values` is called
- **THEN** it returns each discovered `Signal`/`Computed` with its current value and, when accessible, its listener/observer/dependency counts

#### Scenario: Nearest matching signals filter
- **WHEN** `get_signal_values` is called with a query/filter
- **THEN** it returns only signals whose name or derived label matches the query

### Requirement: Binding status introspection tool
The server SHALL provide a tool `get_bindings` that reports reactive bindings and effects installed on inspected components, including their target element, bound source type, and active/subscribed state as visible via reflection.

#### Scenario: Query bindings
- **WHEN** `get_bindings` is called for a component or element
- **THEN** it returns the list of reactive bindings/effects owned or attached to it with their status

### Requirement: Layout metrics introspection tool
The server SHALL provide a tool `get_layout` that returns layout metrics (position, size, alignment, expansion flags) for queried elements.

#### Scenario: Query element metrics
- **WHEN** `get_layout` is called with an element reference
- **THEN** it returns `x`, `y`, `width`, `height`, and alignment/expansion info for that element

### Requirement: WebSocket transport with real-time subscriptions
The server SHALL support WebSocket transport for real-time updates. The server SHALL emit change notifications when inspected UI state changes (signal updates, component mount/unmount).

#### Scenario: Subscribe to UI changes
- **WHEN** a client opens a WebSocket and subscribes to updates
- **THEN** the server streams notification events when relevant signal values change or components mount/unmount

#### Scenario: Polling fallback for one-off queries
- **WHEN** a client performs a one-off query over HTTP
- **THEN** the server responds synchronously with a snapshot without requiring an open connection

### Requirement: Opt-in security with token auth
The server SHALL be disabled by default. It SHALL be enabled only via configuration, and connections SHALL require a random token verified during the WebSocket/HTTP handshake.

#### Scenario: Disabled by default
- **WHEN** the mod starts without MCP debug configuration
- **THEN** no MCP server socket is opened and no debugging endpoints are exposed

#### Scenario: Token required for connection
- **WHEN** a client connects without a valid token
- **THEN** the connection is rejected and no UI state is exposed

### Requirement: No performance overhead when disabled
The server SHALL add negligible overhead to Solim when not in use: no hooks, no observers, and no background threads when disabled.

#### Scenario: Zero cost when disabled
- **WHEN** the MCP server is not enabled
- **THEN** no reflective caches are initialized, no polling timers run, and Solim behavior is unaffected

### Requirement: Screenshot tool registered in the debug server
The MCP debug server SHALL register the `take_screenshot` tool in `ToolRegistry` so it is discoverable and callable like all existing tools.

#### Scenario: tools/list advertises take_screenshot
- **WHEN** a client calls `tools/list`
- **THEN** the returned tool list contains an entry named `take_screenshot` with a description and an object input schema

#### Scenario: Existing tools unaffected
- **WHEN** a client calls `tools/list` or invokes any pre-existing tool
- **THEN** all previously available tools remain present with unchanged names and behavior