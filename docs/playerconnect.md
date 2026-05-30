# Player Connect Audit

## Findings

### Security

- **CRITICAL** `NetworkProxy.java:186-187` — `VirtualConnection.sendTCP/UDP` wraps objects in `ConnectionPacketWrapPacket` and sends over network proxy; NO validation of objects received from connected clients — any connected client can send arbitrary packets
- **CRITICAL** `NetworkProxy.java:131-135` — `received()` handles all packet types; `ConnectionPacketWrapPacket` objects are unwrapped and dispatched to server `dispatchListener` without validation — potential arbitrary packet injection
- **HIGH** `NetworkProxy.java:80-83` — `PacketSerializer.read()` reads packet ID from buffer; unknown packet IDs could trigger unexpected behavior in `Packets.newPacket()`
- **HIGH** `PlayerConnectConfig.java:18-19` — Password stored in `Core.settings` as plain string
- **MEDIUM** `NetworkProxy.java:159-163` — Room ID generated via `UUID.randomUUID()` and used for room identification; but timeout/long-running connections could leak room ID

### Concurrency

- **HIGH** `NetworkProxy.java:93-108` — Custom `run()` loop calls `update(250)` and iterates connections; `VirtualConnection.sendTCP/UDP` may be called from other threads (game logic), creating concurrent access to `proxy.sendTCP` — no synchronization
- **HIGH** `NetworkProxy.java:113-116` — `Selector` accessed via reflection; thread safety depends on arc library internals
- **MEDIUM** `NetworkProxy.java:56` — `connections` IntMap and `orderedConnections` Seq are modified from network thread and possibly game thread; no synchronization
- **MEDIUM** `NetworkProxy.java:269-270` — `VirtualConnection.sendTCP` called from game thread but proxy's sendTCP called on network thread; cross-thread data sharing without locks

### Anti-Patterns

- **HIGH** `NetworkProxy.java` — Massive class (~400 lines) combining connection management, serialization, packet handling, and room management — violates Single Responsibility
- **HIGH** `NetworkProxy.java:59-63` — Reflection to get `Server.dispatchListener` — fragile coupling to arc.net internals
- **MEDIUM** `NetworkProxy.java:135-155` — `received()` has a long if-else chain of `instanceof` checks for 10+ packet types
- **MEDIUM** `Packets.java` — Many inner packet classes with manual serialization; no schema/versioning for packet evolution
- **MEDIUM** `NoopRatekeeper.java` — No-op rate limiter; disables all rate limiting for connected clients through proxy — potential for spam/DoS through proxy
- **MEDIUM** `PlayerConnectFeature.java:37-42` — `Vars.ui.join.shown()` event adds the injector every time join dialog is shown; `inject()` may add duplicate UI elements

### Dead Code / Tech Debt

- **MEDIUM** `NetworkProxy.java:162` — `RoomLinkPacket` handler says "This is not used anymore" — dead code branch in `received()`
- **MEDIUM** `PlayerConnectProviders.java` / `PlayerConnectRoom.java` / `PlayerConnectRenderer.java` — Some classes may be partially used or duplicated logic
- **LOW** `PlayerConnectFeature.java:55-60` — Ping display uses orange/yellow/red coloring; hardcoded thresholds

### Performance

- **LOW** `NetworkProxy.java:93-108` — `update(250)` blocks for up to 250ms per iteration; during this time, incoming packets are not processed
- **LOW** `NetworkProxy.java:288-290` — `VirtualConnection.sendTCP/sendUDP` creates new packet wrapper object on every call; could generate many short-lived allocations
