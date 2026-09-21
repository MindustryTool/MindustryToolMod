# Mindustry Tool — Changelog

Covers **v4.59.1-v8 → v5.1.0-v8** (September 2026). This release is a ground-up rewrite of the entire mod with dozens of new features.

---

## ✨ Features

### In-game Global Chat
- Full chat system with channels, message history, unread badges and instant scroll.
- Send and receive **image attachments**, with fullscreen image previews.
- Message grouping for consecutive messages, replies, and message action popups.
- **Room invite cards** showing the room's player list, and game presence sharing.
- Background sync with an SSE watchdog for reliable live updates.

### Chat Translation & Pretty Chat
- Translate chat messages with multiple translation providers.
- **Pretty Chat**: built-in text prettifiers that clean up and format chat messages.

### Autoplay AI
- Complete autonomous gameplay AI: mining, building, rebuilding, repairing, self-healing, attacking, fleeing, and follow-assist tasks.

### New Features
- **Smart Upgrade** — upgrade blocks in bulk, with a configurable double-tap activation gesture.
- **Free Camera** — move the camera freely, with a centered indicator.
- **Virtual Joystick** — on-screen joystick, great for mobile.
- **Custom Music** — rewritten with menu and editor slots.
- **Quick Schematic Grid** — multi-page grid management, slot tracking, and much higher page/grid limits.
- **Feature Favorites & Keybinds** — favorite your features and use a unified keybind system with sensible defaults.
- **Screenshot** — take in-game screenshots with a keybind.
- **Emoji Browser** — searchable emoji/glyph browser.
- **Camera Zoom** — custom zoom levels.
- **Pathfinding Display** — visualize pathfinding.
- **Bridge Visualizer** — visualize bridge/duct connections with optimized rendering.

### Display & HUD Rewrites
- **Range Display** — granular per-block range toggles (high-performance), plus a zoom threshold setting.
- **Team Resource HUD** — draggable resource overlay, with an all-teams dialog.
- **God Mode** — fully rewritten UI with popup display mode.
- **Time Control** — fully rewritten, with popup display mode.
- **Wave Preview**, **Progress Display**, **Health Bar** — all rewritten with new settings UIs.
- **Quick Access HUD** — reorder items, hide the drag handle, and open God Mode / Time Control as popups.

---

## 🎨 Improved

- **Complete UI overhaul** — every dialog, HUD and browser was rebuilt on a new reactive UI framework: smoother, faster, and far less lag.
- **Map & Schematic Browser** — redesigned with filter chips, animated loading indicators, caching, and fixed pagination.
- **Player Connect** — new room card UI, provider room grouping, and relay support.
- **Login** — new in-game login flow with a browser link and retry support, session-based and reactive.
- **General Settings** — new settings dialog with quick access to logs and crash reporting.
- **Language** — language dropdown to switch languages in-game.
- **Custom Background** — full settings UI with opacity and reset options.
- **Update System** — cleaner update dialog with formatted release notes, plus an optional **beta channel** so you can test pre-releases.
- **Crash Reporting** — automatic crash reports with recovery ("Re-enable Features") and bug reporting built in.

---

## 🐛 Fixed

- HUD overlays no longer reset position when minimizing the desktop window.
- Fixed mobile tap handling and mining crashes.
- Fixed chat initial load and stale channel issues.
- Fixed scrollbar overlap in the map/schematic browsers.
- Fixed team resource crash.
- Removed autoplay cooldown, improved mining behavior and follow-assist.
- Fixed layout, spacing and scaling issues across the whole mod.
- Timezone-safe date handling for release notes and chat timestamps.

---

## ⚙️ Under the Hood

- Entire codebase rewritten and reorganized; legacy code removed.
- New reactive data layer (queries, caching, image prefetching) for snappier browsers.
- Modernized HTTP client with structured error handling and request/query-builder support.
- Updated to Mindustry **v160**.
