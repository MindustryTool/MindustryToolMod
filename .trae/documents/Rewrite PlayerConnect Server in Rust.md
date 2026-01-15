I have analyzed the `structure.md` and the existing Java codebase (`NetworkRelay.java`, `NetworkProxy.java`). Here is the plan to rewrite the PlayerConnect Server in Rust.

# Plan: Rewrite PlayerConnect Server in Rust

## 1. Project Setup
- [ ] Initialize `Cargo.toml` with dependencies:
  - `tokio` (Async runtime)
  - `axum` (HTTP server)
  - `serde`, `serde_json` (Serialization)
  - `bytes` (Buffer handling)
  - `dashmap` (Concurrent maps for state)
  - `tracing` (Logging)
  - `uuid` (Unique IDs)

## 2. Core Modules Implementation
### 2.1 Configuration (`config.rs`)
- [ ] Define `Config` struct.
- [ ] Load `PLAYER_CONNECT_PORT` and `PLAYER_CONNECT_HTTP_PORT` from environment variables.

### 2.2 Data Models (`models.rs`)
- [ ] Define structs matching `structure.md`:
  - `Room`, `Stats`, `Player`.
  - Implement `serde::Serialize`/`Deserialize`.

### 2.3 Packets & Serialization (`packets.rs`)
- [ ] Define `Packet` enum with variants for all packet types (e.g., `RoomJoinPacket`, `MessagePacket`).
- [ ] Define `FrameworkMessage` enum (Ping, DiscoverHost, etc.).
- [ ] Implement custom serialization/deserialization to match `NetworkRelay.java`'s `Serializer`:
  - Handle `ByteBuffer` logic.
  - Handle `id` prefixing (Framework vs. Packet vs. Raw).
  - **Note**: `Packets.id` value needs to be verified (placeholder will be used).

### 2.4 Application State (`state.rs`)
- [ ] Define `AppState` struct:
  - `rooms`: `DashMap<String, Room>`
  - `connections`: `DashMap<i32, Connection>`
  - `packet_queue`: Cache for unconnected packets.
- [ ] Implement helpers for finding/removing rooms and connections.

## 3. Server Logic
### 3.1 Proxy Server (`proxy_server.rs`)
- [ ] Implement TCP Listener on `PLAYER_CONNECT_PORT`.
- [ ] Implement Connection handling loop:
  - Read bytes -> Deserialize -> Process -> Serialize -> Send.
- [ ] Implement `NetworkRelay` logic:
  - Handle `RoomCreationRequest`, `RoomJoin`, `RoomClosure`.
  - Forward packets between Host and Clients (`ConnectionPacketWrapPacket`).
  - Handle `FrameworkMessage` (Ping/Pong).
  - Manage connection idling and spam protection.

### 3.2 HTTP Server (`http_server.rs`)
- [ ] Implement routes:
  - `GET /api/v1/ping`: Health check.
  - `GET /api/v1/rooms`: SSE (Server-Sent Events) for room updates.
  - `GET /{roomId}`: HTML page for room metadata.
  - `POST /{roomId}`: Return server port.
- [ ] Integrate with `AppState` to broadcast updates.

### 3.3 Main Entry (`main.rs`)
- [ ] Initialize Config and Logger.
- [ ] Create shared `AppState`.
- [ ] Spawn `Proxy Server` and `Http Server` tasks.
- [ ] Handle graceful shutdown.

## 4. Verification
- [ ] Verify packet serialization logic against Java implementation.
- [ ] Ensure `DashMap` usage prevents race conditions.
